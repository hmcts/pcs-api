package uk.gov.hmcts.reform.pcs.notify.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.notify.domain.CaseNotification;
import uk.gov.hmcts.reform.pcs.notify.exception.NotificationException;
import uk.gov.hmcts.reform.pcs.notify.exception.PermanentNotificationException;
import uk.gov.hmcts.reform.pcs.notify.exception.TemporaryNotificationException;
import uk.gov.hmcts.reform.pcs.notify.model.NotificationStatus;
import uk.gov.service.notify.NotificationClientException;

import java.util.UUID;
import java.util.function.Consumer;

@Component
@Slf4j
public class NotificationErrorHandler {

    /**
     * Handles NotificationClientException from GOV.UK Notify and updates notification status accordingly.
     *
     * @param exception The NotificationClientException that occurred
     * @param caseNotification The case notification to update
     * @param referenceId The reference ID for logging
     * @param statusUpdater Function to update notification status
     * @throws PermanentNotificationException for 400, 403 status codes
     * @throws TemporaryNotificationException for 429, 500 status codes
     * @throws NotificationException for all other status codes
     */
    public void handleSendEmailException(NotificationClientException exception,
                                         CaseNotification caseNotification,
                                         String referenceId,
                                         Consumer<NotificationStatusUpdate> statusUpdater) {
        int httpStatusCode = exception.getHttpResult();

        log.error("Failed to send email. Reference ID: {}. Reason: {}",
                  referenceId,
                  exception.getMessage(),
                  exception
        );

        switch (httpStatusCode) {
            case 400, 403 -> {
                statusUpdater.accept(new NotificationStatusUpdate(
                    caseNotification,
                    NotificationStatus.PERMANENT_FAILURE,
                    null
                ));
                throw new PermanentNotificationException("Email failed to send.", exception);
            }
            case 429, 500 -> {
                statusUpdater.accept(new NotificationStatusUpdate(
                    caseNotification,
                    NotificationStatus.TEMPORARY_FAILURE,
                    null
                ));
                throw new TemporaryNotificationException("Email temporarily failed to send.", exception);
            }
            default -> {
                statusUpdater.accept(new NotificationStatusUpdate(
                    caseNotification,
                    NotificationStatus.TECHNICAL_FAILURE,
                    null
                ));
                throw new NotificationException("Email failed to send, please try again.", exception);
            }
        }
    }

    /**
     * Handles NotificationClientException from fetch operations.
     *
     * @param exception The NotificationClientException that occurred
     * @param notificationId The notification ID for logging
     * @throws NotificationException Always throws this exception for fetch failures
     */
    public void handleFetchException(NotificationClientException exception, String notificationId) {
        int httpStatusCode = exception.getHttpResult();

        log.error("Failed to fetch notification. ID: {}. Status Code: {}. Reason: {}",
                  notificationId,
                  httpStatusCode,
                  exception.getMessage()
        );

        throw new NotificationException("Failed to fetch notification, please try again.", exception);
    }

    /**
     * Data class to encapsulate notification status update parameters.
     */
    public static class NotificationStatusUpdate {
        private final CaseNotification notification;
        private final NotificationStatus status;
        private final UUID providerNotificationId;

        public NotificationStatusUpdate(CaseNotification notification,
                                        NotificationStatus status,
                                        UUID providerNotificationId) {
            this.notification = notification;
            this.status = status;
            this.providerNotificationId = providerNotificationId;
        }

        public CaseNotification getNotification() {
            return notification;
        }

        public NotificationStatus getStatus() {
            return status;
        }

        public UUID getProviderNotificationId() {
            return providerNotificationId;
        }
    }
}
