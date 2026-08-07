package uk.gov.hmcts.reform.pcs.notify.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.notify.entities.CaseNotification;
import uk.gov.hmcts.reform.pcs.notify.exception.NotificationException;
import uk.gov.hmcts.reform.pcs.notify.exception.TemporaryNotificationException;
import uk.gov.hmcts.reform.pcs.notify.model.NotificationStatus;
import uk.gov.service.notify.NotificationClientException;

import java.util.UUID;
import java.util.function.Consumer;

import static uk.gov.hmcts.reform.pcs.exception.ErrorCode.EMAIL_FAILED_TO_SEND;
import static uk.gov.hmcts.reform.pcs.exception.ErrorCode.FETCH_NOTIFICATION_FAIL;
import static uk.gov.hmcts.reform.pcs.exception.ErrorCode.TEMP_EMAIL_SEND;

@Component
@Slf4j
public class NotificationErrorHandler {

    /**
     * Handles exceptions occurring during the process of sending an email notification.
     * Based on the HTTP status code of the exception, this method updates the notification's
     * status and may throw further exceptions if necessary.
     *
     * @param exception the {@code NotificationClientException} encountered while sending the email
     * @param caseNotification the {@code CaseNotification} object containing details about the notification
     * @param referenceId a unique reference ID corresponding to the notification attempt
     * @param statusUpdater a {@code Consumer} function to update the status of the notification
     */
    public void handleSendEmailException(NotificationClientException exception,
                                            CaseNotification caseNotification,
                                            String referenceId,
                                            Consumer<NotificationStatusUpdate> statusUpdater) {
        int httpStatusCode = exception.getHttpResult();

        log.error("Failed to send email. Reference ID: {}", referenceId, exception);

        switch (httpStatusCode) {
            case 400, 403 -> {
                statusUpdater.accept(new NotificationStatusUpdate(
                    caseNotification,
                    NotificationStatus.PERMANENT_FAILURE,
                    null
                ));
            }
            case 429, 500 -> {
                statusUpdater.accept(new NotificationStatusUpdate(
                    caseNotification,
                    NotificationStatus.TEMPORARY_FAILURE,
                    null
                ));
                throw new TemporaryNotificationException(TEMP_EMAIL_SEND, exception);
            }
            default -> {
                statusUpdater.accept(new NotificationStatusUpdate(
                    caseNotification,
                    NotificationStatus.TECHNICAL_FAILURE,
                    null
                ));
                throw new NotificationException(EMAIL_FAILED_TO_SEND, exception);
            }
        }
    }

    /**
     * Handles exceptions that occur during the fetch notification process by logging the error details
     * and rethrowing a custom {@code NotificationException}.
     *
     * @param exception       The exception thrown during the fetch operation, containing details such as HTTP status
     *                         code.
     * @param notificationId  The unique identifier of the notification that failed to fetch.
     */
    public void handleFetchException(NotificationClientException exception, String notificationId) {
        int httpStatusCode = exception.getHttpResult();
        log.error("Failed to fetch notification. ID: {}. Status Code: {}", notificationId, httpStatusCode, exception);
        throw new NotificationException(FETCH_NOTIFICATION_FAIL, exception);
    }

    /**
     * Represents an update to the status of a case notification.
     * This class encapsulates information about the notification,
     * the updated status, and an optional provider notification ID.
     */
    public record NotificationStatusUpdate(CaseNotification notification, NotificationStatus status,
                                            UUID providerNotificationId) {
    }
}
