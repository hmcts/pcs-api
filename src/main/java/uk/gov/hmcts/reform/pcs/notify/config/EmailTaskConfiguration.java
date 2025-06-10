package uk.gov.hmcts.reform.pcs.notify.config;

import com.github.kagkarlsson.scheduler.task.CompletionHandler;
import com.github.kagkarlsson.scheduler.task.SchedulableInstance;
import com.github.kagkarlsson.scheduler.task.TaskDescriptor;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.CustomTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.Schedules;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.pcs.notify.exception.NotificationException;
import uk.gov.hmcts.reform.pcs.notify.exception.PermanentNotificationException;
import uk.gov.hmcts.reform.pcs.notify.exception.TemporaryNotificationException;
import uk.gov.hmcts.reform.pcs.notify.model.EmailNotificationRequest;
import uk.gov.hmcts.reform.pcs.notify.model.NotificationStatus;
import uk.gov.hmcts.reform.pcs.notify.service.NotificationService;
import uk.gov.service.notify.Notification;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;

import java.time.Duration;
import java.time.Instant;


@Configuration
@Slf4j
public class EmailTaskConfiguration {

    public static final TaskDescriptor<EmailState> sendEmailTask =
        TaskDescriptor.of("send-email-task", EmailState.class);

    public static final TaskDescriptor<EmailState> verifyEmailTask =
        TaskDescriptor.of("verify-email-task", EmailState.class);

    private final NotificationService notificationService;

    @Autowired
    public EmailTaskConfiguration(NotificationService notificationService,
                                    NotificationClient notificationClient) {
        this.notificationService = notificationService;
    }

    /**
     * Creates a custom task that handles the process of sending an email using the provided {@link EmailState}.
     * This task communicates with an external email notification service.
     * The task performs the following operations:
     * - Attempts to send an email based on the email state provided.
     * - Extracts the notification ID from the response upon successful email submission and updates the email state.
     * - Chains the successful task to a verification task with a delay to allow processing.
     * - Handles failures:
     *   - For permanent failures, the task is removed without retries.
     *   - For temporary failures, retries the task with an exponential backoff based on error type.
     *   - Retries up to a defined retry limit (currently 3 retries) for other exceptions,
     *     after which the task is removed.
     *
     * @return A {@link CustomTask} object that represents the send email task with the configured behavior.
     */
    @Bean
    public CustomTask<EmailState> sendEmailTask() {
        return Tasks.custom(sendEmailTask).execute((
            (taskInstance, executionContext) -> {
                EmailState emailState = taskInstance.getData();
                log.info("Processing send email task: {}", emailState.id);

                try {
                    EmailNotificationRequest emailRequest = EmailNotificationRequest.builder()
                        .emailAddress(emailState.emailAddress)
                        .templateId(emailState.templateId)
                        .personalisation(emailState.personalisation)
                        .reference(emailState.reference)
                        .emailReplyToId(emailState.emailReplyToId)
                        .build();

                    SendEmailResponse response = notificationService.sendEmail(emailRequest);

                    // Extract notification ID from the response
                    String notificationId = response.getNotificationId().toString();
                    log.info("Email sent successfully. Notification ID: {}", notificationId);

                    // Update the email state with the notification ID
                    EmailState nextState = new EmailState(
                        emailState.id,
                        emailState.emailAddress,
                        emailState.templateId,
                        emailState.personalisation,
                        emailState.reference,
                        emailState.emailReplyToId,
                        notificationId,
                        emailState.retryCount
                    );

                    // Chain to verification task with a delay to allow GOV.UK Notify to process
                    log.info("The time is {}", Instant.now());
                    return new CompletionHandler.OnCompleteReplace<>(
                        currentInstance -> SchedulableInstance.of(
                            new TaskInstance<>(verifyEmailTask.getTaskName(), currentInstance.getId(), nextState),
                            Instant.now().plusSeconds(60)
                        )
                    );
                } catch (PermanentNotificationException e) {
                    log.error("Permanent failure sending email. Reference ID: {}. Reason: {}",
                        emailState.id,
                        e.getMessage(),
                        e
                    );
                    // Don't retry permanent failures
                    return new CompletionHandler.OnCompleteRemove<>();

                } catch (TemporaryNotificationException e) {
                    log.warn("Temporary failure sending email. Reference ID: {}. Reason: {}",
                        emailState.id,
                        e.getMessage(),
                        e
                    );

                    // Always retry temporary failures (429, 500) regardless of retry count
                    EmailState retryState = new EmailState(
                        emailState.id,
                        emailState.emailAddress,
                        emailState.templateId,
                        emailState.personalisation,
                        emailState.reference,
                        emailState.emailReplyToId,
                        null, // No notification ID yet
                        emailState.retryCount + 1
                    );

                    // Use exponential backoff for different error types
                    Duration retryDelay = (e.getCause() instanceof NotificationClientException
                                            notificationClientException)
                        ? getRetryDelayForStatusCode(notificationClientException.getHttpResult())
                        : Duration.ofMinutes(20);

                    log.info("Scheduling retry #{} for temporary failure task: {} in {}",
                        retryState.retryCount, retryState.id, retryDelay);
                    return new CompletionHandler.OnCompleteReschedule<>(
                        Schedules.fixedDelay(retryDelay),
                        retryState
                    );

                } catch (NotificationException e) {
                    log.error("Failed to send email. Reference ID: {}. Reason: {}",
                        emailState.id,
                        e.getMessage(),
                        e
                    );

                    if (emailState.retryCount < 5) {
                        EmailState retryState = new EmailState(
                            emailState.id,
                            emailState.emailAddress,
                            emailState.templateId,
                            emailState.personalisation,
                            emailState.reference,
                            emailState.emailReplyToId,
                            null, // No notification ID yet
                            emailState.retryCount + 1
                        );

                        log.info("Scheduling retry #{} for task: {}", retryState.retryCount, retryState.id);
                        return new CompletionHandler.OnCompleteReschedule<>(
                            Schedules.fixedDelay(Duration.ofMinutes(20)),
                            retryState
                        );
                    } else {
                        log.error("Max retries reached for email task: {}", emailState.id);
                        return new CompletionHandler.OnCompleteRemove<>();
                    }
                }
            })
        );
    }

    /**
     * Creates and returns a custom task for verifying the delivery status of an email notification.
     * The task fetches the email delivery status using the notification service and handles the
     * status accordingly. If the email is delivered successfully, the task completes. In case of
     * failures, it logs the errors and completes. If the email is still pending, the task is
     * rescheduled to check the status later.
     *
     * @return a custom task that verifies email delivery status, reschedules if still pending,
     *         or completes upon successful delivery or failure.
     */
    @Bean
    public CustomTask<EmailState> verifyEmailTask() {
        return Tasks.custom(verifyEmailTask)
            .execute((taskInstance, executionContext) -> {
                EmailState emailState = taskInstance.getData();
                log.info("The time is {}, Verifying email delivery for notification ID: {}",
                            Instant.now(), emailState.notificationId);

                try {
                    // Use notification client to check the email status
                    Notification notification = notificationService.fetchNotificationStatus(emailState.notificationId);
                    String status = notification.getStatus();

                    log.info("Email status for notification ID {}: {}", emailState.notificationId, status);

                    // Check if email is delivered or still in progress
                    if (NotificationStatus.DELIVERED.toString().equalsIgnoreCase(status)) {
                        // Email successfully delivered
                        log.info("Email successfully delivered. Task complete: {}", emailState.id);
                        return new CompletionHandler.OnCompleteRemove<>();
                    } else if ("permanent-failure".equalsIgnoreCase(status)
                        || "temporary-failure".equalsIgnoreCase(status)
                        || "technical-failure".equalsIgnoreCase(status)) {
                        // Handle failures
                        log.error("Email delivery failed with status: {} for task: {}", status, emailState.id);
                        return new CompletionHandler.OnCompleteRemove<>();
                    } else {
                        // Still pending, check again later
                        log.info("Email still processing (status: {}). Will check again for task: {}",
                                status, emailState.id);
                        return new CompletionHandler.OnCompleteReschedule<>(
                            Schedules.fixedDelay(Duration.ofMinutes(1)),
                            emailState
                        );
                    }
                } catch (NotificationClientException e) {
                    log.error("Error verifying email status: {}", e.getMessage());
                    return new CompletionHandler.OnCompleteReschedule<>(
                        Schedules.fixedDelay(Duration.ofMinutes(2)),
                        emailState
                    );
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            });
    }

    /**
     * Determines the retry delay duration based on the provided HTTP status code.
     *
     * @param statusCode the HTTP status code that influences the retry delay calculation.
     * @return the duration to wait before retrying the operation. Specific delays are:
     *         429 (Rate limit) - 5 minutes,
     *         500 (Server error) - 10 minutes,
     *         Other status codes - 20 minutes.
     */
    private Duration getRetryDelayForStatusCode(int statusCode) {
        return switch (statusCode) {
            case 429 -> Duration.ofMinutes(5);  // Rate limit - retry sooner
            case 500 -> Duration.ofMinutes(10); // Server error - retry with moderate delay
            default -> Duration.ofMinutes(20);  // Default delay
        };
    }
}

