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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.pcs.notify.model.EmailState;
import uk.gov.hmcts.reform.pcs.notify.exception.PermanentNotificationException;
import uk.gov.hmcts.reform.pcs.notify.exception.TaskInterruptedException;
import uk.gov.hmcts.reform.pcs.notify.exception.TemporaryNotificationException;
import uk.gov.hmcts.reform.pcs.notify.model.EmailNotificationRequest;
import uk.gov.hmcts.reform.pcs.notify.model.NotificationStatus;
import uk.gov.hmcts.reform.pcs.notify.service.NotificationService;
import uk.gov.service.notify.Notification;
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
    private final int maxRetriesSendEmail;
    private final int maxRetriesCheckEmail;
    private final long sendingBackoffDelay;
    private final long statusCheckDelay;
    private final long verifyingBackoffDelay;

    @Autowired
    public EmailTaskConfiguration(
        NotificationService notificationService,
        @Value("${notify.send-email.max-retries}") int maxRetriesSendEmail,
        @Value("${notify.check-status.max-retries}") int maxRetriesCheckEmail,
        @Value("${notify.send-email.backoff-delay-seconds}") long sendingBackoffDelay,
        @Value("${notify.check-status.delay-seconds}") long verifyingBackoffDelay,
        @Value("${notify.check-status.backoff-delay-seconds}") long statusCheckDelay
    ) {
        this.notificationService = notificationService;
        this.maxRetriesSendEmail = maxRetriesSendEmail;
        this.maxRetriesCheckEmail = maxRetriesCheckEmail;
        this.sendingBackoffDelay = sendingBackoffDelay;
        this.statusCheckDelay = statusCheckDelay;
        this.verifyingBackoffDelay = verifyingBackoffDelay;
    }

    /**
     * Creates and configures a custom task for sending emails. The task processes the send email request
     * using the specified email state and handles success along with both permanent and temporary failures.
     * The task retries on temporary failures using exponential backoff delay up to the maximum retry count
     * and removes the task in case of permanent failures.
     *
     * @return a configured CustomTask instance for sending emails, with proper retry and failure handling.
     */
    @Bean
    public CustomTask<EmailState> sendEmailTask() {
        return Tasks.custom(sendEmailTask)
            .onFailure(new FailureHandler.MaxRetriesFailureHandler<>(
                maxRetriesSendEmail,
                new FailureHandler.ExponentialBackoffFailureHandler<>(Duration.ofSeconds(sendingBackoffDelay))
            ))
            .execute((taskInstance, executionContext) -> {
                EmailState emailState = taskInstance.getData();
                log.info("Processing send email task: {}", emailState.getId());

                try {
                    EmailNotificationRequest emailRequest = EmailNotificationRequest.builder()
                        .emailAddress(emailState.getEmailAddress())
                        .templateId(emailState.getTemplateId())
                        .personalisation(emailState.getPersonalisation())
                        .reference(emailState.getReference())
                        .emailReplyToId(emailState.getEmailReplyToId())
                        .build();

                    SendEmailResponse response = notificationService.sendEmail(emailRequest);
                    String notificationId = response.getNotificationId().toString();
                    log.info("Email sent successfully. Notification ID: {}", notificationId);

                    EmailState nextState = emailState.toBuilder()
                        .notificationId(notificationId)
                        .build();

                    return new CompletionHandler.OnCompleteReplace<>(
                        currentInstance -> SchedulableInstance.of(
                            new TaskInstance<>(verifyEmailTask.getTaskName(), currentInstance.getId(), nextState),
                            Instant.now().plusSeconds(statusCheckDelay)
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
     * Creates and configures a custom task to verify email delivery status.
     * The task attempts to verify the delivery status of a notification email
     * identified by its notification ID present in the task's data. The task
     * includes a failure handling mechanism with a configurable maximum retries
     * and an exponential backoff delay for retry attempts.
     * Logging is performed to provide insights into the success or failure of
     * the email delivery verification process.
     *
     * @return a configured {@link CustomTask} instance for verifying email delivery status.
     */
    @Bean
    public CustomTask<EmailState> verifyEmailTask() {
        return Tasks.custom(verifyEmailTask)
            .onFailure(new FailureHandler.MaxRetriesFailureHandler<>(
                maxRetriesCheckEmail,
                new FailureHandler.ExponentialBackoffFailureHandler<>(Duration.ofSeconds(verifyingBackoffDelay))
            ))
            .execute((taskInstance, executionContext) -> {
                EmailState emailState = taskInstance.getData();
                log.info("Verifying email delivery for ID: {}", emailState.getNotificationId());

                try {
                    Notification notification = notificationService
                        .fetchNotificationStatus(emailState.getNotificationId());
                    String status = notification.getStatus();
                    if (NotificationStatus.DELIVERED.toString().equalsIgnoreCase(status)) {
                        log.info("Email successfully delivered: {}", emailState.getId());
                    } else {
                        log.error("Failure with status: {} for task: {}", status, emailState.getId());
                    }
                    return new CompletionHandler.OnCompleteRemove<>();
                } catch (NotificationClientException e) {
                    log.error("Failed to verify status due to API error", e);
                    return new CompletionHandler.OnCompleteRemove<>();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new TaskInterruptedException("Task interrupted", e);
                }
            });
    }
}

