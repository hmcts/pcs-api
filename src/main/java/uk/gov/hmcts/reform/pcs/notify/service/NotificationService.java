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
    private final int maxStatusCheckRetries;

    public NotificationService(
        NotificationClient notificationClient,
        NotificationRepository notificationRepository,
        @Value("${notify.status-check-delay-millis}") long statusCheckDelay,
        @Value("${notify.max-status-check-retries:5}") int maxStatusCheckRetries) {
        this.notificationClient = notificationClient;
        this.notificationRepository = notificationRepository;
        this.statusCheckDelay = statusCheckDelay;
        this.maxStatusCheckRetries = maxStatusCheckRetries;
    }

    public SendEmailResponse sendEmail(EmailNotificationRequest emailRequest) {
        SendEmailResponse sendEmailResponse;
        final String destinationAddress = emailRequest.getEmailAddress();
        final String templateId = emailRequest.getTemplateId();
        final Map<String, Object> personalisation = emailRequest.getPersonalisation();
        final String referenceId = UUID.randomUUID().toString();

        // Create notification in database
        CaseNotification caseNotification = createCaseNotification(
            emailRequest.getEmailAddress(), "Email", UUID.randomUUID());

        try {
            sendEmailResponse = notificationClient.sendEmail(
                templateId,
                destinationAddress,
                personalisation,
                referenceId
            );

            log.debug("Email sent successfully. Reference ID: {}", referenceId);
            
            // Update the notification with provider ID
            updateProviderNotificationId(caseNotification.getNotificationId(), sendEmailResponse.getNotificationId());
            
            // Schedule status check
            scheduleStatusCheck(sendEmailResponse.getNotificationId().toString(), 0);

            return sendEmailResponse;
        } catch (NotificationClientException notificationClientException) {
            log.error("Failed to send email. Reference ID: {}. Reason: {}",
                      referenceId,
                      notificationClientException.getMessage(),
                      notificationClientException
            );

            throw new NotificationException("Email failed to send, please try again.", notificationClientException);
        }
    }

    @Async("notificationExecutor")
    public CompletableFuture<Notification> checkNotificationStatus(String notificationId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Wait for configured delay
                TimeUnit.MILLISECONDS.sleep(statusCheckDelay);
                
                Notification notification = notificationClient.getNotificationById(notificationId);
                log.info("Notification status check - ID: {}, Status: {}", 
                    notificationId,
                    notification.getStatus()
                );
                return notification;
            } catch (NotificationClientException | InterruptedException e) {
                log.error("Error checking notification status for ID: {}. Error: {}", 
                    notificationId, 
                    e.getMessage(),
                    e
                );
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                throw new CompletionException(e);
            }
        });
    }

    @Async("notificationExecutor")
    public void scheduleStatusCheck(String notificationId, int retryCount) {
        try {
            // Wait for configured delay before checking
            TimeUnit.MILLISECONDS.sleep(statusCheckDelay);
            
            Notification notification = notificationClient.getNotificationById(notificationId);
            String status = notification.getStatus();
            
            log.info("Notification status check - ID: {}, Status: {}, Retry: {}", 
                notificationId, status, retryCount);
            
            // Update the status in database
            updateNotificationStatus(UUID.fromString(notificationId), status);
            
            // If the status is terminal, we're done
            if (isTerminalStatus(status)) {
                log.info("Notification ID: {} reached terminal status: {}", notificationId, status);
                return;
            }
            
            // If we haven't reached max retries, schedule another check
            if (retryCount < maxStatusCheckRetries) {
                scheduleStatusCheck(notificationId, retryCount + 1);
            } else {
                log.info("Reached maximum retry attempts for notification ID: {}", notificationId);
            }
        } catch (NotificationClientException | InterruptedException e) {
            log.error("Error in scheduled status check for ID: {}. Error: {}", 
                notificationId, 
                e.getMessage(),
                e
            );
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            
            // If we haven't reached max retries, try again despite the error
            if (retryCount < maxStatusCheckRetries) {
                scheduleStatusCheck(notificationId, retryCount + 1);
            } else {
                log.error("Failed to check status after maximum retries for notification ID: {}", notificationId);
            }
        }
    }
    
    private boolean isTerminalStatus(String status) {
        return status.equals(NotificationStatus.DELIVERED.toString()) 
            || status.equals(NotificationStatus.PERMANENT_FAILURE.toString()) 
            || status.equals(NotificationStatus.TECHNICAL_FAILURE.toString());
    }
    
    private void updateNotificationStatus(UUID providerNotificationId, String status) {
        try {
            Optional<CaseNotification> notificationOpt = 
                notificationRepository.findByProviderNotificationId(providerNotificationId);
            
            if (notificationOpt.isPresent()) {
                CaseNotification notification = notificationOpt.get();
                notification.setStatus(status);
                notification.setLastUpdatedAt(LocalDateTime.now());
                
                notificationRepository.save(notification);
                log.info("Updated notification status to {} for ID: {}", 
                    status, notification.getNotificationId());
            } else {
                log.warn("No notification found with provider ID: {}", providerNotificationId);
            }
        } catch (DataAccessException e) {
            log.error("Failed to update notification status for provider ID: {}. Error: {}", 
                providerNotificationId, e.getMessage(), e);
        }
    }
    
    private void updateProviderNotificationId(UUID notificationId, UUID providerNotificationId) {
        try {
            Optional<CaseNotification> notificationOpt = notificationRepository.findById(notificationId);
            
            if (notificationOpt.isPresent()) {
                CaseNotification notification = notificationOpt.get();
                notification.setProviderNotificationId(providerNotificationId);
                notification.setLastUpdatedAt(LocalDateTime.now());
                
                notificationRepository.save(notification);
                log.info("Updated provider notification ID to {} for notification ID: {}", 
                    providerNotificationId, notificationId);
            } else {
                log.warn("No notification found with ID: {}", notificationId);
            }
        } catch (DataAccessException e) {
            log.error("Failed to update provider notification ID for notification ID: {}. Error: {}", 
                notificationId, e.getMessage(), e);
        }
    }

    CaseNotification createCaseNotification(String recipient, String type, UUID caseId) {
        CaseNotification toSaveNotification = new CaseNotification();
        toSaveNotification.setCaseId(caseId);
        // Use the toString() method of the enum to get the string value
        toSaveNotification.setStatus(NotificationStatus.PENDING_SCHEDULE.toString());
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
}
