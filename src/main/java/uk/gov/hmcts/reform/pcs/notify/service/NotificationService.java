package uk.gov.hmcts.reform.pcs.notify.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.notify.exception.NotificationException;
import uk.gov.hmcts.reform.pcs.notify.model.EmailNotificationRequest;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;
import uk.gov.service.notify.Notification;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

@Service
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private final NotificationClient notificationClient;
    private final long statusCheckDelay;

    public NotificationService(
        NotificationClient notificationClient,
        @Value("${notify.status-check-delay-millis}") long statusCheckDelay) {
        this.notificationClient = notificationClient;
        this.statusCheckDelay = statusCheckDelay;
    }

    public SendEmailResponse sendEmail(EmailNotificationRequest emailRequest) {
        SendEmailResponse sendEmailResponse;
        final String destinationAddress = emailRequest.getEmailAddress();
        final String templateId = emailRequest.getTemplateId();
        final Map<String, Object> personalisation = emailRequest.getPersonalisation();
        final String referenceId = UUID.randomUUID().toString();

        try {
            sendEmailResponse = notificationClient.sendEmail(
                templateId,
                destinationAddress,
                personalisation,
                referenceId
            );

            log.debug("Email sent successfully. Reference ID: {}", referenceId);
            
            // Trigger async status check using CompletableFuture
            checkNotificationStatus(sendEmailResponse.getNotificationId().toString())
                .exceptionally(throwable -> {
                    log.error("Error checking notification status: {}", throwable.getMessage(), throwable);
                    return null;
                });

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
}
