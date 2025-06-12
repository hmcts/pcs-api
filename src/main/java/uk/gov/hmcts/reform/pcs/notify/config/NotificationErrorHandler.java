package uk.gov.hmcts.reform.pcs.notify.config;

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
     * Handles exceptions occurring during the sending of emails and logs the error details.
     * Updates the notification status accordingly based on the HTTP status code of the exception.
     *
     * @param exception The exception thrown during the email sending process.
     * @param caseNotification The case notification associated with the email being sent.
     * @param referenceId A unique ID reference used for tracking the email transaction.
     * @param statusUpdater A consumer that updates the notification status based on the outcome.
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
     * Handles the exception encountered while attempting to fetch a notification by logging details
     * about the failure and rethrowing it as a {@code NotificationException}.
     *
     * @param exception the {@code NotificationClientException} thrown during the fetch operation,
     *                  containing details such as the HTTP status code and error message.
     * @param notificationId the unique identifier of the notification that failed to be fetched.
     */
    public void handleFetchException(NotificationClientException exception, String notificationId) {
        int httpStatusCode = exception.getHttpResult();

        log.error("Failed to fetch notification. ID: {}. Status Code: {}. Reason: {}",
                    notificationId,
                    httpStatusCode,
                    exception.getMessage(),
                    exception
        );

        throw new NotificationException("Failed to fetch notification, please try again.", exception);
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
