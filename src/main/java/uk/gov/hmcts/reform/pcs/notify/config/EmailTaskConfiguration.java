package uk.gov.hmcts.reform.pcs.notify.config;

import com.github.kagkarlsson.scheduler.task.CompletionHandler;
import com.github.kagkarlsson.scheduler.task.FailureHandler;
import com.github.kagkarlsson.scheduler.task.SchedulableInstance;
import com.github.kagkarlsson.scheduler.task.TaskDescriptor;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.CustomTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
    public EmailTaskConfiguration(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Creates and returns a task for sending email notifications.
     * The task handles email sending logic, processes responses,
     * and includes retrying mechanisms for temporary failures
     * with a maximum of 5 retries. Permanent failures are logged
     * and the task is removed after failure.
     *
     * @return a configured CustomTask for processing email notifications using the EmailState object.
     */
    @Bean
    public CustomTask<EmailState> sendEmailTask() {
        return Tasks.custom(sendEmailTask)
            .onFailure(new FailureHandler.MaxRetriesFailureHandler<>(
                5, // max retries
                new FailureHandler.ExponentialBackoffFailureHandler<>(Duration.ofSeconds(3)) // base delay
            ))
            .execute((taskInstance, executionContext) -> {
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
                    String notificationId = response.getNotificationId().toString();
                    log.info("Email sent successfully. Notification ID: {}", notificationId);

                    EmailState nextState = emailState.withNotificationId(notificationId);

                    return new CompletionHandler.OnCompleteReplace<>(
                        currentInstance -> SchedulableInstance.of(
                            new TaskInstance<>(verifyEmailTask.getTaskName(), currentInstance.getId(), nextState),
                            Instant.now().plusSeconds(60)
                        )
                    );
                } catch (PermanentNotificationException e) {
                    log.error("Permanent failure sending email: {}", e.getMessage(), e);
                    return new CompletionHandler.OnCompleteRemove<>();
                } catch (TemporaryNotificationException e) {
                    log.warn("Retryable failure: {}", e.getMessage(), e);
                    // Let the scheduler retry using the configured failure handler
                    throw e;
                }
            });
    }


    /**
     * Creates and configures a custom task for verifying email delivery status.
     * The task handles retry logic in case of failures, with exponential backoff for temporary issues.
     * It checks the delivery status of an email using the notification service and processes the status accordingly.
     *
     * @return a configured instance of {@code CustomTask} for managing the email verification process with retry and
     *     failure handling mechanisms.
     */
    @Bean
    public CustomTask<EmailState> verifyEmailTask() {
        return Tasks.custom(verifyEmailTask)
            .onFailure(new FailureHandler.MaxRetriesFailureHandler<>(
                5,
                new FailureHandler.ExponentialBackoffFailureHandler<>(Duration.ofSeconds(5))
            ))
            .execute((taskInstance, executionContext) -> {
                EmailState emailState = taskInstance.getData();
                log.info("Verifying email delivery for ID: {}", emailState.notificationId);

                try {
                    Notification notification = notificationService.fetchNotificationStatus(emailState.notificationId);
                    String status = notification.getStatus();

                    if (NotificationStatus.DELIVERED.toString().equalsIgnoreCase(status)) {
                        log.info("Email successfully delivered: {}", emailState.id);
                        return new CompletionHandler.OnCompleteRemove<>();
                    } else if (NotificationStatus.TEMPORARY_FAILURE.toString().equalsIgnoreCase(status)) {
                        log.warn("Temporary failure status for task: {}", emailState.id);
                        throw new TemporaryNotificationException("Temporary delivery failure", null); // triggers retry
                    } else {
                        log.error("Permanent failure with status: {} for task: {}", status, emailState.id);
                        return new CompletionHandler.OnCompleteRemove<>();
                    }
                } catch (NotificationClientException e) {
                    log.error("Failed to verify status due to API error", e);
                    throw new TemporaryNotificationException("Failed to verify email status", e);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Task interrupted", e);
                }
            });
    }
}

