package uk.gov.hmcts.reform.pcs.notify.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.notify.domain.CaseNotification;
import uk.gov.hmcts.reform.pcs.notify.exception.NotificationException;
import uk.gov.hmcts.reform.pcs.notify.model.EmailNotificationRequest;
import uk.gov.hmcts.reform.pcs.notify.model.NotificationStatus;
import uk.gov.hmcts.reform.pcs.notify.repository.NotificationRepository;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;
import uk.gov.service.notify.Notification;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class NotificationService {
    private final NotificationClient notificationClient;
    private final NotificationRepository notificationRepository;
    private final NotificationErrorHandler errorHandler;

    /**
     * Constructs a new NotificationService with the specified dependencies.
     *
     * @param notificationClient the client used for sending notifications
     * @param notificationRepository the repository responsible for managing notification data
     * @param errorHandler the handler for processing notification-related errors
     */
    public NotificationService(
        NotificationClient notificationClient,
        NotificationRepository notificationRepository,
        NotificationErrorHandler errorHandler) {
        this.notificationClient = notificationClient;
        this.notificationRepository = notificationRepository;
        this.errorHandler = errorHandler;
    }

    /**
     * Sends an email using the provided email notification request details.
     * This method utilizes a notification client to send an email to the specified
     * recipient with a given template ID and personalization data and handles
     * the entire lifecycle of email notification tracking.
     *
     * @param emailRequest the request object containing details for the email to be sent.
     *                     This includes the recipient's email address, the template ID,
     *                     and any personalization data that should be included in the email.
     * @return a {@code SendEmailResponse} object containing the response details from
     *         the notification client if the email is sent successfully.
     *         Note that if an exception occurs during the process, this method will not
     *         return normally and will allow the appropriate error handler to manage the error.
     */
    public SendEmailResponse sendEmail(EmailNotificationRequest emailRequest) {
        final String destinationAddress = emailRequest.getEmailAddress();
        final String templateId = emailRequest.getTemplateId();
        final Map<String, Object> personalisation = emailRequest.getPersonalisation();
        final String referenceId = UUID.randomUUID().toString();

        // Create initial notification in database
        CaseNotification caseNotification = createCaseNotification(
            emailRequest.getEmailAddress(),
            UUID.randomUUID()
        );

        try {
            SendEmailResponse sendEmailResponse = notificationClient.sendEmail(
                templateId,
                destinationAddress,
                personalisation,
                referenceId
            );

            log.debug("Email sent successfully. Reference ID: {}", referenceId);

            // Update notification with provider ID received from GOV.UK Notify
            UUID providerNotificationId = sendEmailResponse.getNotificationId();
            updateNotificationStatus(caseNotification, NotificationStatus.SUBMITTED, providerNotificationId);

            return sendEmailResponse;
        } catch (NotificationClientException notificationClientException) {
            errorHandler.handleSendEmailException(
                notificationClientException,
                caseNotification,
                referenceId,
                this::updateNotificationFromStatusUpdate
            );
            // This line will never be reached due to exceptions thrown in error handler
            return null;
        }
    }

    /**
     * Fetches the notification status for the given notification ID and updates
     * the notification status in the database accordingly.
     *
     * @param notificationId the unique identifier of the notification to be fetched
     * @return the fetched Notification object containing details about the notification
     * @throws NotificationClientException if an error occurs during the notification retrieval
     * @throws InterruptedException if the thread executing the method is interrupted
     */
    public Notification fetchNotificationStatus(String notificationId)
        throws NotificationClientException, InterruptedException {
        try {
            Notification notification = notificationClient.getNotificationById(notificationId);
            updateNotificationStatusInDatabase(notification, notificationId);
            return notification;
        } catch (NotificationClientException notificationClientException) {
            errorHandler.handleFetchException(notificationClientException, notificationId);
            // This line will never be reached due to exception thrown in error handler
            return null;
        }
    }

    /**
     * Helper method to update notification status from NotificationStatusUpdate object.
     *
     * @param statusUpdate The status update containing notification, status, and provider ID
     */
    private void updateNotificationFromStatusUpdate(NotificationErrorHandler.NotificationStatusUpdate statusUpdate) {
        updateNotificationStatus(
            statusUpdate.notification(),
            statusUpdate.status(),
            statusUpdate.providerNotificationId()
        );
    }

    /**
     * Creates a case notification in the database.
     *
     * @param recipient The recipient of the notification
     * @param caseId    The associated case ID
     * @return The created CaseNotification
     * @throws NotificationException If saving the notification fails
     */
    private CaseNotification createCaseNotification(String recipient, UUID caseId) {
        CaseNotification toSaveNotification = new CaseNotification();
        toSaveNotification.setCaseId(caseId);
        // Use the toString() method of the enum to get the string value
        toSaveNotification.setStatus(NotificationStatus.PENDING_SCHEDULE);
        toSaveNotification.setType("Email");
        toSaveNotification.setRecipient(recipient);

        try {
            CaseNotification savedNotification = notificationRepository.save(toSaveNotification);
            log.info(
                "Case Notification with ID {} has been saved to the database", savedNotification.getNotificationId()
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
     * Updates the notification status in the database, based on the status from GOV.UK Notify.
     *
     * @param notification The notification object from GOV.UK Notify
     * @param notificationId The notification ID for database lookup
     */
    private void updateNotificationStatusInDatabase(Notification notification, String notificationId) {
        try {
            CaseNotification caseNotification = notificationRepository
                .findByProviderNotificationId(UUID.fromString(notificationId))
                .orElse(null);

            if (caseNotification != null) {
                updateNotificationWithStatus(caseNotification, notification.getStatus());
            } else {
                log.warn("Could not find case notification with provider ID: {}", notificationId);
            }
        } catch (Exception e) {
            log.error("Error updating notification status in database for ID: {}. Error: {}",
                      notificationId, e.getMessage(), e);
        }
    }

    /**
     * Updates a notification with the status from GOV.UK Notify
     *
     * @param caseNotification The notification entity to update
     * @param status The status string from GOV.UK Notify
     */
    private void updateNotificationWithStatus(CaseNotification caseNotification, String status) {
        try {
            NotificationStatus notificationStatus = NotificationStatus.fromString(status);
            updateNotificationStatus(caseNotification, notificationStatus, null);
        } catch (IllegalArgumentException e) {
            log.warn("Unknown notification status: {}", status);
        }
    }

    /**
     * Updates the status of a notification and saves it to the database.
     *
     * @param notification The notification to update
     * @param status The new status to set
     * @param providerNotificationId Optional provider notification ID to set (can be null)
     * @return An Optional containing the updated notification, or an empty Optional if an error occurred
     */
    private Optional<CaseNotification> updateNotificationStatus(
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

            CaseNotification saved = notificationRepository.save(notification);
            log.info("Updated notification status to {} for notification ID: {}",
                     status, notification.getNotificationId());
            return Optional.of(saved);
        } catch (Exception e) {
            log.error("Error updating notification status to {}: {}",
                      status, e.getMessage(), e);
            return Optional.empty();
        }
    }
}
