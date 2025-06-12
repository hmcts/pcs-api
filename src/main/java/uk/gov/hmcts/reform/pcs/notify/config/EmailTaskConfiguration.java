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
import uk.gov.hmcts.reform.pcs.notify.domain.CaseNotification;
import uk.gov.hmcts.reform.pcs.notify.model.EmailState;
import uk.gov.hmcts.reform.pcs.notify.exception.PermanentNotificationException;
import uk.gov.hmcts.reform.pcs.notify.exception.TemporaryNotificationException;
import uk.gov.hmcts.reform.pcs.notify.model.NotificationStatus;
import uk.gov.hmcts.reform.pcs.notify.repository.NotificationRepository;
import uk.gov.hmcts.reform.pcs.notify.service.NotificationService;
import uk.gov.service.notify.Notification;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Configuration
@Slf4j
public class EmailTaskConfiguration {
    private static final String SEND_EMAIL_TASK_NAME = "send-email-task";
    private static final String VERIFY_EMAIL_TASK_NAME = "verify-email-task";

    public static final TaskDescriptor<EmailState> sendEmailTask =
        TaskDescriptor.of(SEND_EMAIL_TASK_NAME, EmailState.class);
    public static final TaskDescriptor<EmailState> verifyEmailTask =
        TaskDescriptor.of(VERIFY_EMAIL_TASK_NAME, EmailState.class);

    private final NotificationService notificationService;
    private final NotificationClient notificationClient;
    private final NotificationErrorHandler errorHandler;
    private final NotificationRepository notificationRepository;
    private final int maxRetriesSendEmail;
    private final int maxRetriesCheckEmail;
    private final Duration sendingBackoffDelay;
    private final Duration statusCheckTaskDelay;
    private final Duration statusCheckBackoffDelay;

    @Autowired
    public EmailTaskConfiguration(
        NotificationService notificationService,
        NotificationClient notificationClient,
        NotificationErrorHandler errorHandler,
        NotificationRepository notificationRepository,
        @Value("${notify.send-email.max-retries}") int maxRetriesSendEmail,
        @Value("${notify.check-status.max-retries}") int maxRetriesCheckEmail,
        @Value("${notify.send-email.backoff-delay-seconds}") Duration sendingBackoffDelay,
        @Value("${notify.check-status.task-delay-seconds}") Duration statusCheckTaskDelay,
        @Value("${notify.check-status.backoff-delay-seconds}") Duration statusCheckBackoffDelay
    ) {
        this.notificationService = notificationService;
        this.notificationClient = notificationClient;
        this.errorHandler = errorHandler;
        this.notificationRepository = notificationRepository;
        this.maxRetriesSendEmail = maxRetriesSendEmail;
        this.maxRetriesCheckEmail = maxRetriesCheckEmail;
        this.sendingBackoffDelay = sendingBackoffDelay;
        this.statusCheckTaskDelay = statusCheckTaskDelay;
        this.statusCheckBackoffDelay = statusCheckBackoffDelay;
    }

    @Bean
    public CustomTask<EmailState> sendEmailTask() {
        return Tasks.custom(sendEmailTask)
            .onFailure(new FailureHandler.MaxRetriesFailureHandler<>(
                maxRetriesSendEmail,
                new FailureHandler.ExponentialBackoffFailureHandler<>(sendingBackoffDelay)
            ))
            .execute((taskInstance, executionContext) -> {
                EmailState emailState = taskInstance.getData();
                log.info("Processing send email task: {} with DB notification ID: {}",
                            emailState.getId(), emailState.getDbNotificationId());

                Optional<CaseNotification> notificationOpt = notificationRepository.findById(
                    emailState.getDbNotificationId());
                if (notificationOpt.isEmpty()) {
                    log.error("Notification not found with ID: {}", emailState.getDbNotificationId());
                    return new CompletionHandler.OnCompleteRemove<>();
                }

                CaseNotification caseNotification = notificationOpt.get();

                try {
                    final String templateId = emailState.getTemplateId();
                    final String destinationAddress = emailState.getEmailAddress();
                    final Map<String, Object> personalisation = emailState.getPersonalisation();
                    final String referenceId = UUID.randomUUID().toString();

                    SendEmailResponse response = notificationClient.sendEmail(
                        templateId,
                        destinationAddress,
                        personalisation,
                        referenceId
                    );

                    if (response.getNotificationId() == null) {
                        log.error("Email service returned null notification ID for task: {}", emailState.getId());
                        throw new PermanentNotificationException("Null notification ID from email service",
                                                                    new IllegalStateException(
                                                                        "Email service returned null notification ID"));
                    }

                    notificationService.updateNotificationAfterSending(
                        emailState.getDbNotificationId(),
                        response.getNotificationId()
                    );

                    String notificationId = response.getNotificationId().toString();
                    log.info("Email sent successfully. Notification ID: {}", notificationId);

                    EmailState nextState = emailState.toBuilder()
                        .notificationId(notificationId)
                        .build();

                    return new CompletionHandler.OnCompleteReplace<>(
                        currentInstance -> SchedulableInstance.of(
                            new TaskInstance<>(verifyEmailTask.getTaskName(), currentInstance.getId(), nextState),
                            Instant.now().plus(statusCheckTaskDelay)
                        )
                    );
                } catch (NotificationClientException e) {
                    log.error("NotificationClient error sending email: {}", e.getMessage(), e);

                    errorHandler.handleSendEmailException(
                        e,
                        caseNotification,
                        UUID.randomUUID().toString(),
                        this::updateNotificationFromStatusUpdate
                    );
                    return null;
                } catch (PermanentNotificationException e) {
                    log.error("Permanent failure sending email: {}", e.getMessage(), e);
                    return new CompletionHandler.OnCompleteRemove<>();
                } catch (TemporaryNotificationException e) {
                    log.warn("Retryable failure: {}", e.getMessage(), e);
                    throw e;
                }
            });
    }

    @Bean
    public CustomTask<EmailState> verifyEmailTask() {
        return Tasks.custom(verifyEmailTask)
            .onFailure(new FailureHandler.MaxRetriesFailureHandler<>(
                maxRetriesCheckEmail,
                new FailureHandler.ExponentialBackoffFailureHandler<>(statusCheckBackoffDelay)
            ))
            .execute((taskInstance, executionContext) -> {
                EmailState emailState = taskInstance.getData();
                log.info("Verifying email delivery for ID: {}", emailState.getNotificationId());

                try {
                    Notification notification = notificationClient.getNotificationById(emailState.getNotificationId());

                    notificationService.updateNotificationStatus(
                        emailState.getDbNotificationId(),
                        notification.getStatus()
                    );

                    String status = notification.getStatus();
                    if (NotificationStatus.DELIVERED.toString().equalsIgnoreCase(status)) {
                        log.info("Email successfully delivered: {}", emailState.getId());
                    } else {
                        log.error("Failure with status: {} for task: {}", status, emailState.getId());
                    }
                    return new CompletionHandler.OnCompleteRemove<>();
                } catch (NotificationClientException e) {
                    log.error("Failed to verify status due to API error", e);

                    errorHandler.handleFetchException(e, emailState.getNotificationId());
                    return new CompletionHandler.OnCompleteRemove<>();
                }
            });
    }

    private void updateNotificationFromStatusUpdate(NotificationErrorHandler.NotificationStatusUpdate statusUpdate) {
        notificationService.updateNotificationStatus(
            statusUpdate.notification().getNotificationId(),
            statusUpdate.status().toString()
        );
    }
}
