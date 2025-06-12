package uk.gov.hmcts.reform.pcs.notify.service;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.notify.config.EmailTaskConfiguration;
import uk.gov.hmcts.reform.pcs.notify.domain.CaseNotification;
import uk.gov.hmcts.reform.pcs.notify.exception.NotificationException;
import uk.gov.hmcts.reform.pcs.notify.model.EmailNotificationRequest;
import uk.gov.hmcts.reform.pcs.notify.model.EmailNotificationResponse;
import uk.gov.hmcts.reform.pcs.notify.model.EmailState;
import uk.gov.hmcts.reform.pcs.notify.model.NotificationStatus;
import uk.gov.hmcts.reform.pcs.notify.repository.NotificationRepository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final SchedulerClient schedulerClient;

    /**
     * Constructs a new NotificationService with the specified dependencies.
     *
     * @param notificationRepository the repository responsible for managing notification data
     * @param schedulerClient the scheduler client for task scheduling
     */
    public NotificationService(
        NotificationRepository notificationRepository,
        SchedulerClient schedulerClient) {
        this.notificationRepository = notificationRepository;
        this.schedulerClient = schedulerClient;
    }

    /**
     * Schedules an email notification to be sent. This method creates a database record
     * for the notification and schedules a task to handle the actual email sending.
     *
     * @param emailRequest the request object containing details for the email to be sent
     * @return EmailNotificationResponse containing the task ID and initial status
     * @throws NotificationException if there's an error creating the notification record
     */
    public EmailNotificationResponse scheduleEmailNotification(EmailNotificationRequest emailRequest) {
        String taskId = UUID.randomUUID().toString();

        // Create initial notification in database
        CaseNotification caseNotification = createCaseNotification(
            emailRequest.getEmailAddress(),
            UUID.randomUUID(), // You might want to pass this from the request
            taskId
        );

        // Create the email state object for the task
        EmailState emailState = new EmailState(
            taskId,
            emailRequest.getEmailAddress(),
            emailRequest.getTemplateId(),
            emailRequest.getPersonalisation(),
            emailRequest.getReference(),
            emailRequest.getEmailReplyToId(),
            null, // notification ID will be set after sending
            caseNotification.getNotificationId()
        );

        // Schedule the send email task to run immediately
        boolean scheduled = schedulerClient.scheduleIfNotExists(
            EmailTaskConfiguration.sendEmailTask
                .instance(taskId)
                .data(emailState)
                .scheduledTo(Instant.now())
        );

        if (!scheduled) {
            log.warn("Task with ID {} already exists", taskId);
        }

        // Update notification status to SCHEDULED
        updateNotificationStatus(caseNotification, NotificationStatus.SCHEDULED, null);

        // Create response
        EmailNotificationResponse response = new EmailNotificationResponse();
        response.setTaskId(taskId);
        response.setStatus(NotificationStatus.SCHEDULED.toString());
        response.setNotificationId(caseNotification.getNotificationId());

        log.info("Email notification scheduled with task ID: {} and notification ID: {}",
                 taskId, caseNotification.getNotificationId());

        return response;
    }

    /**
     * Updates notification status after successful email sending.
     * Called by the task after NotificationClient.sendEmail() succeeds.
     *
     * @param dbNotificationId the database notification ID
     * @param providerNotificationId the notification ID from GOV.UK Notify
     */
    public void updateNotificationAfterSending(UUID dbNotificationId, UUID providerNotificationId) {
        Optional<CaseNotification> notificationOpt = notificationRepository.findById(dbNotificationId);
        if (notificationOpt.isEmpty()) {
            log.error("Notification not found with ID: {}", dbNotificationId);
            return;
        }

        CaseNotification notification = notificationOpt.get();
        updateNotificationStatus(notification, NotificationStatus.SUBMITTED, providerNotificationId);
    }

    /**
     * Updates notification status after email sending failure.
     * Called by the task when NotificationClient.sendEmail() fails.
     *
     * @param dbNotificationId the database notification ID
     * @param exception the exception that occurred
     */
    public void updateNotificationAfterFailure(UUID dbNotificationId, Exception exception) {
        Optional<CaseNotification> notificationOpt = notificationRepository.findById(dbNotificationId);
        if (notificationOpt.isEmpty()) {
            log.error("Notification not found with ID on failure: {}", dbNotificationId);
            return;
        }

        CaseNotification notification = notificationOpt.get();
        updateNotificationStatus(notification, NotificationStatus.PERMANENT_FAILURE, null);
        log.error("Email sending failed for notification ID: {}, error: {}",
                  dbNotificationId, exception.getMessage());
    }

    /**
     * Creates a case notification in the database.
     *
     * @param recipient The recipient of the notification
     * @param caseId    The associated case ID
     * @param taskId    The task ID for tracking
     * @return The created CaseNotification
     * @throws NotificationException If saving the notification fails
     */
    private CaseNotification createCaseNotification(String recipient, UUID caseId, String taskId) {
        CaseNotification toSaveNotification = new CaseNotification();
        toSaveNotification.setCaseId(caseId);
        toSaveNotification.setStatus(NotificationStatus.PENDING_SCHEDULE);
        toSaveNotification.setType("Email");
        toSaveNotification.setRecipient(recipient);

        try {
            CaseNotification savedNotification = notificationRepository.save(toSaveNotification);
            log.info(
                "Case Notification with ID {} has been saved to the database with task ID {}",
                savedNotification.getNotificationId(), taskId
            );
            return savedNotification;
        } catch (DataAccessException dataAccessException) {
            log.error(
                "Failed to save Case Notification with Case ID: {}. Reason: {}",
                toSaveNotification.getCaseId(),
                dataAccessException.getMessage(),
                dataAccessException
            );
            throw new NotificationException("Failed to save Case Notification.", dataAccessException);
        }
    }



    /**
     * Updates notification status from verification task.
     * Called by the verification task after checking status with NotificationClient.
     *
     * @param dbNotificationId the database notification ID
     * @param statusString the status string from GOV.UK Notify
     */
    public void updateNotificationStatus(UUID dbNotificationId, String statusString) {
        Optional<CaseNotification> notificationOpt = notificationRepository.findById(dbNotificationId);
        if (notificationOpt.isEmpty()) {
            log.error("Notification not found with ID on status update: {}", dbNotificationId);
            return;
        }

        CaseNotification notification = notificationOpt.get();
        try {
            NotificationStatus status = NotificationStatus.fromString(statusString);
            updateNotificationStatus(notification, status, null);
        } catch (IllegalArgumentException e) {
            log.warn("Unknown notification status: {}", statusString);
        }
    }

    /**
     * Updates the status of a notification and saves it to the database.
     *
     * @param notification The notification to update
     * @param status The new status to set
     * @param providerNotificationId Optional provider notification ID to set (can be null)
     */
    private void updateNotificationStatus(
        CaseNotification notification,
        NotificationStatus status,
        UUID providerNotificationId) {

        try {
            notification.setStatus(status);
            notification.setLastUpdatedAt(LocalDateTime.now());

            if (providerNotificationId != null) {
                notification.setProviderNotificationId(providerNotificationId);
            }

            if (status == NotificationStatus.SENDING) {
                notification.setSubmittedAt(LocalDateTime.now());
            }

            notificationRepository.save(notification);
            log.info("Updated notification status to {} for notification ID: {}",
                     status, notification.getNotificationId());
        } catch (Exception e) {
            log.error("Error updating notification status to {}: {}",
                      status, e.getMessage(), e);
        }
    }
}
