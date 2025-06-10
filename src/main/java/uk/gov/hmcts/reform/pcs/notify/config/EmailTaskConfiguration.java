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
import uk.gov.hmcts.reform.pcs.notify.model.EmailNotificationRequest;
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
                } catch (NotificationException e) {
                    log.error("Failed to send email. Reference ID: {}. Reason: {}",
                        emailState.id,
                        e.getMessage(),
                        e
                    );
                    if (emailState.retryCount < 3) {
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
     * Task for verifying that the email was delivered/processed.
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
                    if ("delivered".equalsIgnoreCase(status)) {
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
                    throw new RuntimeException(e);
                }
            });
    }
}

