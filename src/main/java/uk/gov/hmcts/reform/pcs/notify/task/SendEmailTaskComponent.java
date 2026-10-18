package uk.gov.hmcts.reform.pcs.notify.task;

import com.github.kagkarlsson.scheduler.task.CompletionHandler;
import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.FailureHandler;
import com.github.kagkarlsson.scheduler.task.SchedulableInstance;
import com.github.kagkarlsson.scheduler.task.TaskDescriptor;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.CustomTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.notify.config.NotificationErrorHandler;
import uk.gov.hmcts.reform.pcs.notify.config.NotificationErrorHandler.NotificationStatusUpdate;
import uk.gov.hmcts.reform.pcs.notify.entities.CaseNotification;
import uk.gov.hmcts.reform.pcs.notify.model.NotificationStatus;
import uk.gov.hmcts.reform.pcs.notify.model.SendEmailTaskData;
import uk.gov.hmcts.reform.pcs.notify.exception.PermanentNotificationException;
import uk.gov.hmcts.reform.pcs.notify.exception.TemporaryNotificationException;
import uk.gov.hmcts.reform.pcs.notify.repository.NotificationRepository;
import uk.gov.hmcts.reform.pcs.notify.service.NotificationService;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;
import uk.gov.service.notify.NotificationClient;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.notify.task.VerifyEmailTaskComponent.verifyEmailTask;

@Component
@Slf4j
public class SendEmailTaskComponent {
    private static final String SEND_EMAIL_TASK_NAME = "send-email-task";

    private static final String MDC_CASE_REFERENCE = "caseReference";
    private static final String MDC_NOTIFICATION_CLAIM_TYPE = "notificationClaimType";
    private static final String MDC_TASK_NAME = "taskName";
    private static final String MDC_TERMINAL_FAILURE = "terminalFailure";
    private static final String MDC_FAILURE_REASON = "failureReason";

    public static final TaskDescriptor<SendEmailTaskData> sendEmailTask =
        TaskDescriptor.of(SEND_EMAIL_TASK_NAME, SendEmailTaskData.class);

    private final NotificationService notificationService;
    private final NotificationClient notificationClient;
    private final NotificationErrorHandler errorHandler;
    private final NotificationRepository notificationRepository;
    private final int maxRetriesSendEmail;
    private final Duration sendingBackoffDelay;
    private final Duration statusCheckTaskDelay;

    @Autowired
    public SendEmailTaskComponent(
        NotificationService notificationService,
        NotificationClient notificationClient,
        NotificationErrorHandler errorHandler,
        NotificationRepository notificationRepository,
        @Value("${notify.send-email.max-retries}") int maxRetriesSendEmail,
        @Value("${notify.send-email.backoff-delay-seconds}") Duration sendingBackoffDelay,
        @Value("${notify.check-status.task-delay-seconds}") Duration statusCheckTaskDelay
    ) {
        this.notificationService = notificationService;
        this.notificationClient = notificationClient;
        this.errorHandler = errorHandler;
        this.notificationRepository = notificationRepository;
        this.maxRetriesSendEmail = maxRetriesSendEmail;
        this.sendingBackoffDelay = sendingBackoffDelay;
        this.statusCheckTaskDelay = statusCheckTaskDelay;
    }

    /**
     * Defines a custom task for sending email notifications by interacting with the
     * notification client and processing the associated state. The task uses a retry
     * mechanism with exponential backoff in case of failures, up to the configured
     * maximum number of retries. On successful execution, the task initiates the
     * verification of the sent email's delivery status.
     * The method includes error handling for various scenarios, such as permanent
     * notification failures, temporary failures, and missing notifications. Updates
     * the notification state in the system following the email sending process.
     *
     * @return the custom task for sending email notifications
     */
    @Bean
    public CustomTask<SendEmailTaskData> sendEmailTask() {
        return Tasks.custom(sendEmailTask)
            .onFailure(new FailureHandler.MaxRetriesFailureHandler<>(
                maxRetriesSendEmail,
                new FailureHandler.ExponentialBackoffFailureHandler<>(sendingBackoffDelay)
            ))
            .execute((taskInstance, executionContext) -> {
                SendEmailTaskData taskData = taskInstance.getData();
                log.info("Processing send email task: {} with DB notification ID: {}",
                         taskData.getId(), taskData.getDbNotificationId());

                Optional<CaseNotification> notificationOpt = notificationRepository.findById(
                    taskData.getDbNotificationId());
                if (notificationOpt.isEmpty()) {
                    log.error("Notification not found with ID: {}", taskData.getDbNotificationId());
                    return new CompletionHandler.OnCompleteRemove<>();
                }

                CaseNotification caseNotification = notificationOpt.get();

                try {
                    return sendEmail(taskData, caseNotification);
                } catch (RuntimeException e) {
                    // db-scheduler deletes the task after this attempt, so record the failure on the notification
                    // and say so once, rather than leaving it scheduled for ever.
                    if (isFinalAttempt(executionContext)) {
                        markAsFailed(taskData);
                        logTerminalFailure(taskData, caseNotification, e);
                    }
                    throw e;
                }
            });
    }

    private CompletionHandler<SendEmailTaskData> sendEmail(SendEmailTaskData taskData,
                                                           CaseNotification caseNotification) {
        try {
            final String templateId = taskData.getTemplateId();
            final String destinationAddress = taskData.getEmailAddress();
            final Map<String, Object> personalisation = taskData.getPersonalisation();
            final String referenceId = UUID.randomUUID().toString();

            SendEmailResponse response = notificationClient.sendEmail(
                templateId,
                destinationAddress,
                personalisation,
                referenceId
            );

            if (response.getNotificationId() == null) {
                log.error("Email service returned null notification ID for task: {}", taskData.getId());
                throw new PermanentNotificationException("Null notification ID from email service",
                                                            new IllegalStateException(
                                                                "Email service returned null notification ID"));
            }

            notificationService.updateNotificationAfterSending(
                taskData.getDbNotificationId(),
                response.getNotificationId()
            );

            String notificationId = response.getNotificationId().toString();
            log.info("Request sent successfully. Notification ID: {}", notificationId);

            SendEmailTaskData nextState = taskData.toBuilder()
                .notificationId(notificationId)
                .build();

            return new CompletionHandler.OnCompleteReplace<>(
                currentInstance -> SchedulableInstance.of(
                    new TaskInstance<>(
                        verifyEmailTask
                            .getTaskName(),
                        currentInstance.getId(), nextState),
                    Instant.now().plus(statusCheckTaskDelay)
                )
            );
        } catch (NotificationClientException e) {
            log.error("NotificationClient error sending email: {}", e.getMessage(), e);

            if (isPermanentFailure(e)) {
                errorHandler.handleSendEmailException(
                    e,
                    caseNotification,
                    UUID.randomUUID().toString(),
                    this::updateNotificationFromStatusUpdate
                );
                logTerminalFailure(taskData, caseNotification, e);
                return new CompletionHandler.OnCompleteRemove<>();
            } else {
                throw new TemporaryNotificationException("Email temporarily failed to send.", e);
            }
        }
    }

    // MaxRetriesFailureHandler(maxRetries) runs the task maxRetries + 1 times in all.
    private boolean isFinalAttempt(ExecutionContext executionContext) {
        return executionContext.getExecution().consecutiveFailures + 1 > maxRetriesSendEmail;
    }

    private void markAsFailed(SendEmailTaskData taskData) {
        try {
            notificationService.updateNotificationStatus(taskData.getDbNotificationId(),
                                                         NotificationStatus.TECHNICAL_FAILURE.toString());
        } catch (RuntimeException e) {
            log.error("Could not mark notification {} as failed", taskData.getDbNotificationId(), e);
        }
    }

    /**
     * Logs, once, that the email will not be sent, with {@code terminalFailure=true}, the task name, case
     * reference and claim type in the MDC, which App Insights copies into customDimensions for alerting.
     */
    private void logTerminalFailure(SendEmailTaskData taskData, CaseNotification caseNotification,
                                    Exception cause) {
        final Long caseReference = caseReference(taskData.getDbNotificationId());
        MDC.put(MDC_TASK_NAME, SEND_EMAIL_TASK_NAME);
        MDC.put(MDC_TERMINAL_FAILURE, "true");
        MDC.put(MDC_FAILURE_REASON, cause.getClass().getSimpleName());
        MDC.put(MDC_NOTIFICATION_CLAIM_TYPE, String.valueOf(caseNotification.getClaimType()));
        if (caseReference != null) {
            MDC.put(MDC_CASE_REFERENCE, String.valueOf(caseReference));
        }
        try {
            log.error("Sending email notification {} permanently failed for case {}", taskData.getDbNotificationId(),
                      caseReference, cause);
        } finally {
            MDC.remove(MDC_TASK_NAME);
            MDC.remove(MDC_TERMINAL_FAILURE);
            MDC.remove(MDC_FAILURE_REASON);
            MDC.remove(MDC_NOTIFICATION_CLAIM_TYPE);
            MDC.remove(MDC_CASE_REFERENCE);
        }
    }

    private Long caseReference(Integer dbNotificationId) {
        try {
            return notificationRepository.findCaseReferenceById(dbNotificationId).orElse(null);
        } catch (RuntimeException e) {
            log.error("Could not find the case of notification {}", dbNotificationId, e);
            return null;
        }
    }

    private void updateNotificationFromStatusUpdate(NotificationStatusUpdate statusUpdate) {
        notificationService.updateNotificationStatus(
            statusUpdate.notification().getId(),
            statusUpdate.status().toString()
        );
    }

    private boolean isPermanentFailure(NotificationClientException e) {
        int httpStatusCode = e.getHttpResult();
        return httpStatusCode == 400 || httpStatusCode == 403;
    }
}
