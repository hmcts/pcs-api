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
     * Handles exceptions that occur while attempting to send an email notification.
     * Depending on the HTTP status code within the exception, this method categorizes
     * the error as a permanent failure, temporary failure, or technical failure,
     * updates the notification status accordingly, and rethrows a respective exception type.
     *
     * @param exception the {@link NotificationClientException} that occurred during the email send attempt
     * @param caseNotification the {@link CaseNotification} object associated with the email notification
     * @param referenceId a unique reference identifier for the email notification
     * @param statusUpdater a {@link Consumer} function to update the status of the notification
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
