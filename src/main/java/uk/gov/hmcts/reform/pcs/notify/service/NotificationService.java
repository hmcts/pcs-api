package uk.gov.hmcts.reform.pcs.notify.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class NotificationService {
    private final NotificationClient notificationClient;
    private final NotificationRepository notificationRepository;
    private final long statusCheckDelay;

    /**
     * Constructor for the NotificationService.
     *
     * @param notificationClient The notification client for GOV.UK Notify
     * @param notificationRepository Repository for saving notification data
     * @param statusCheckDelay Delay in milliseconds before checking notification status
     */
    public NotificationService(
        NotificationClient notificationClient,
        NotificationRepository notificationRepository,
        @Value("${notify.status-check-delay-millis}") long statusCheckDelay) {
        this.notificationClient = notificationClient;
        this.notificationRepository = notificationRepository;
        this.statusCheckDelay = statusCheckDelay;
    }

    /**
     * Sends an email using GOV.UK Notify service.
     *
     * @param emailRequest The email request containing necessary information
     * @return SendEmailResponse from GOV.UK Notify
     * @throws NotificationException If the email fails to send
     */
    public SendEmailResponse sendEmail(EmailNotificationRequest emailRequest) {
        SendEmailResponse sendEmailResponse;
        final String destinationAddress = emailRequest.getEmailAddress();
        final String templateId = emailRequest.getTemplateId();
        final Map<String, Object> personalisation = emailRequest.getPersonalisation();
        final String referenceId = UUID.randomUUID().toString();

        // Create initial notification in database
        CaseNotification caseNotification = createCaseNotification(
            emailRequest.getEmailAddress(), 
            "Email", 
            UUID.randomUUID()
        );

        try {
            sendEmailResponse = notificationClient.sendEmail(
                templateId,
                destinationAddress,
                personalisation,
                referenceId
            );

            log.debug("Email sent successfully. Reference ID: {}", referenceId);

            // Update notification with provider ID received from GOV.UK Notify
            UUID providerNotificationId = sendEmailResponse.getNotificationId();
            updateNotificationStatus(caseNotification, NotificationStatus.SUBMITTED, providerNotificationId);
            
            // Trigger async status check using CompletableFuture
            checkNotificationStatus(providerNotificationId.toString())
                .exceptionally(throwable -> {
                    log.error("Error checking notification status: {}", throwable.getMessage(), throwable);
                    return null;
                });

            return sendEmailResponse;
        } catch (NotificationClientException notificationClientException) {
            // Update notification status to failure
            updateNotificationStatus(caseNotification, NotificationStatus.TECHNICAL_FAILURE, null);
            
            log.error("Failed to send email. Reference ID: {}. Reason: {}",
                      referenceId,
                      notificationClientException.getMessage(),
                      notificationClientException
            );

            throw new NotificationException("Email failed to send, please try again.", notificationClientException);
        }
    }

    /**
     * Asynchronously checks the status of a notification with GOV.UK Notify.
     * 
     * @param notificationId The ID of the notification to check
     * @return CompletableFuture containing the notification details
     */
    @Async("notificationExecutor")
    public CompletableFuture<Notification> checkNotificationStatus(String notificationId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getAndProcessNotificationStatus(notificationId);
            } catch (NotificationClientException | InterruptedException e) {
                handleStatusCheckException(notificationId, e);
                throw new CompletionException(e);
            }
        });
    }

    /**
     * Creates a case notification in the database.
     * 
     * @param recipient The recipient of the notification
     * @param type The type of notification
     * @param caseId The associated case ID
     * @return The created CaseNotification
     * @throws NotificationException If saving the notification fails
     */
    CaseNotification createCaseNotification(String recipient, String type, UUID caseId) {
        CaseNotification toSaveNotification = new CaseNotification();
        toSaveNotification.setCaseId(caseId);
        // Use the toString() method of the enum to get the string value
        toSaveNotification.setStatus(NotificationStatus.PENDING_SCHEDULE);
        toSaveNotification.setType(type);
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
     * Retrieves notification status from GOV.UK Notify and processes it.
     * 
     * @param notificationId The notification ID to check
     * @return The notification object from GOV.UK Notify
     * @throws NotificationClientException If there's an error communicating with GOV.UK Notify
     * @throws InterruptedException If the thread is interrupted while waiting
     */
    private Notification getAndProcessNotificationStatus(String notificationId) 
            throws NotificationClientException, InterruptedException {
        // Wait for configured delay
        TimeUnit.MILLISECONDS.sleep(statusCheckDelay);
        
        Notification notification = notificationClient.getNotificationById(notificationId);
        
        updateNotificationStatusInDatabase(notification, notificationId);
        
        return notification;
    }
    
    /**
     * Handles exceptions that occur when checking notification status.
     * 
     * @param notificationId The notification ID being checked
     * @param e The exception that was thrown
     */
    private void handleStatusCheckException(String notificationId, Exception e) {
        log.error("Error checking notification status for ID: {}. Error: {}", 
            notificationId, 
            e.getMessage(),
            e
        );
        if (e instanceof InterruptedException) {
            Thread.currentThread().interrupt();
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
