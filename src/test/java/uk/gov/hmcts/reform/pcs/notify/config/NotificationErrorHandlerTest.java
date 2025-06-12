package uk.gov.hmcts.reform.pcs.notify.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.notify.domain.CaseNotification;
import uk.gov.hmcts.reform.pcs.notify.exception.NotificationException;
import uk.gov.hmcts.reform.pcs.notify.exception.PermanentNotificationException;
import uk.gov.hmcts.reform.pcs.notify.exception.TemporaryNotificationException;
import uk.gov.hmcts.reform.pcs.notify.model.NotificationStatus;
import uk.gov.service.notify.NotificationClientException;

import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationErrorHandler Tests")
class NotificationErrorHandlerTest {

    private NotificationErrorHandler errorHandler;

    @Mock
    private Consumer<NotificationErrorHandler.NotificationStatusUpdate> statusUpdater;

    private CaseNotification mockCaseNotification;
    private static final String REFERENCE_ID = "test-reference-123";
    private static final String NOTIFICATION_ID = "notification-456";

    @BeforeEach
    void setUp() {
        errorHandler = new NotificationErrorHandler();

        mockCaseNotification = new CaseNotification();
        mockCaseNotification.setNotificationId(UUID.randomUUID());
        mockCaseNotification.setRecipient("test@example.com");
        mockCaseNotification.setType("Email");
    }

    @Nested
    @DisplayName("Send Email Exception Handling Tests")
    class SendEmailExceptionHandlingTests {

        @Test
        @DisplayName("Should throw PermanentNotificationException for 400 status code")
        void shouldThrowPermanentNotificationExceptionFor400StatusCode() {
            NotificationClientException clientException = mock(NotificationClientException.class);
            when(clientException.getHttpResult()).thenReturn(400);
            when(clientException.getMessage()).thenReturn("Bad Request");

            assertThatThrownBy(() ->
                errorHandler.handleSendEmailException(clientException,
                                                        mockCaseNotification, REFERENCE_ID, statusUpdater))
                .isInstanceOf(PermanentNotificationException.class)
                .hasMessage("Email failed to send.")
                .hasCause(clientException);

            ArgumentCaptor<NotificationErrorHandler.NotificationStatusUpdate> captor =
                forClass(NotificationErrorHandler.NotificationStatusUpdate.class);
            verify(statusUpdater).accept(captor.capture());

            NotificationErrorHandler.NotificationStatusUpdate statusUpdate = captor.getValue();
            assertThat(statusUpdate.notification()).isEqualTo(mockCaseNotification);
            assertThat(statusUpdate.status()).isEqualTo(NotificationStatus.PERMANENT_FAILURE);
            assertThat(statusUpdate.providerNotificationId()).isNull();
        }

        @Test
        @DisplayName("Should throw PermanentNotificationException for 403 status code")
        void shouldThrowPermanentNotificationExceptionFor403StatusCode() {
            NotificationClientException clientException = mock(NotificationClientException.class);
            when(clientException.getHttpResult()).thenReturn(403);
            when(clientException.getMessage()).thenReturn("Forbidden");

            assertThatThrownBy(() ->
                errorHandler.handleSendEmailException(clientException,
                                                        mockCaseNotification, REFERENCE_ID, statusUpdater))
                .isInstanceOf(PermanentNotificationException.class)
                .hasMessage("Email failed to send.")
                .hasCause(clientException);

            ArgumentCaptor<NotificationErrorHandler.NotificationStatusUpdate> captor =
                forClass(NotificationErrorHandler.NotificationStatusUpdate.class);
            verify(statusUpdater).accept(captor.capture());

            assertThat(captor.getValue().status()).isEqualTo(NotificationStatus.PERMANENT_FAILURE);
        }

        @Test
        @DisplayName("Should throw TemporaryNotificationException for 429 status code")
        void shouldThrowTemporaryNotificationExceptionFor429StatusCode() {
            NotificationClientException clientException = mock(NotificationClientException.class);
            when(clientException.getHttpResult()).thenReturn(429);
            when(clientException.getMessage()).thenReturn("Too Many Requests");

            assertThatThrownBy(() ->
                errorHandler.handleSendEmailException(clientException,
                                                        mockCaseNotification, REFERENCE_ID, statusUpdater))
                .isInstanceOf(TemporaryNotificationException.class)
                .hasMessage("Email temporarily failed to send.")
                .hasCause(clientException);

            ArgumentCaptor<NotificationErrorHandler.NotificationStatusUpdate> captor =
                forClass(NotificationErrorHandler.NotificationStatusUpdate.class);
            verify(statusUpdater).accept(captor.capture());

            assertThat(captor.getValue().status()).isEqualTo(NotificationStatus.TEMPORARY_FAILURE);
        }

        @Test
        @DisplayName("Should throw TemporaryNotificationException for 500 status code")
        void shouldThrowTemporaryNotificationExceptionFor500StatusCode() {
            NotificationClientException clientException = mock(NotificationClientException.class);
            when(clientException.getHttpResult()).thenReturn(500);
            when(clientException.getMessage()).thenReturn("Internal Server Error");

            assertThatThrownBy(() ->
                                    errorHandler.handleSendEmailException(clientException,
                                                                            mockCaseNotification,
                                                                            REFERENCE_ID,
                                                                            statusUpdater))
                .isInstanceOf(TemporaryNotificationException.class)
                .hasMessage("Email temporarily failed to send.")
                .hasCause(clientException);

            ArgumentCaptor<NotificationErrorHandler.NotificationStatusUpdate> captor =
                forClass(NotificationErrorHandler.NotificationStatusUpdate.class);
            verify(statusUpdater).accept(captor.capture());

            assertThat(captor.getValue().status()).isEqualTo(NotificationStatus.TEMPORARY_FAILURE);
        }

        @Test
        @DisplayName("Should throw NotificationException for unknown status codes")
        void shouldThrowNotificationExceptionForUnknownStatusCodes() {
            NotificationClientException clientException = mock(NotificationClientException.class);
            when(clientException.getHttpResult()).thenReturn(502);
            when(clientException.getMessage()).thenReturn("Bad Gateway");

            assertThatThrownBy(() ->
                                    errorHandler.handleSendEmailException(clientException,
                                                                            mockCaseNotification,
                                                                            REFERENCE_ID, statusUpdater))
                .isInstanceOf(NotificationException.class)
                .hasMessage("Email failed to send, please try again.")
                .hasCause(clientException);

            ArgumentCaptor<NotificationErrorHandler.NotificationStatusUpdate> captor =
                forClass(NotificationErrorHandler.NotificationStatusUpdate.class);
            verify(statusUpdater).accept(captor.capture());

            assertThat(captor.getValue().status()).isEqualTo(NotificationStatus.TECHNICAL_FAILURE);
        }

        @Test
        @DisplayName("Should handle multiple different status codes correctly")
        void shouldHandleMultipleDifferentStatusCodesCorrectly() {
            int[] statusCodes = {401, 404, 503, 999};

            for (int statusCode : statusCodes) {
                NotificationClientException clientException = mock(NotificationClientException.class);
                when(clientException.getHttpResult()).thenReturn(statusCode);
                when(clientException.getMessage()).thenReturn("Status " + statusCode);

                assertThatThrownBy(() ->
                                        errorHandler.handleSendEmailException(clientException,
                                                                                mockCaseNotification,
                                                                                REFERENCE_ID, statusUpdater))
                    .isInstanceOf(NotificationException.class)
                    .hasMessage("Email failed to send, please try again.")
                    .hasCause(clientException);
            }
        }
    }

    @Nested
    @DisplayName("Fetch Exception Handling Tests")
    class FetchExceptionHandlingTests {

        @Test
        @DisplayName("Should throw NotificationException for fetch failures")
        void shouldThrowNotificationExceptionForFetchFailures() {
            NotificationClientException clientException = mock(NotificationClientException.class);
            when(clientException.getHttpResult()).thenReturn(404);
            when(clientException.getMessage()).thenReturn("Not Found");

            assertThatThrownBy(() -> errorHandler.handleFetchException(clientException, NOTIFICATION_ID)
                )
                .isInstanceOf(NotificationException.class)
                .hasMessage("Failed to fetch notification, please try again.")
                .hasCause(clientException);
        }

        @Test
        @DisplayName("Should handle different fetch error status codes")
        void shouldHandleDifferentFetchErrorStatusCodes() {
            int[] statusCodes = {400, 401, 403, 500, 502, 503};

            for (int statusCode : statusCodes) {
                NotificationClientException clientException = mock(NotificationClientException.class);
                when(clientException.getHttpResult()).thenReturn(statusCode);
                when(clientException.getMessage()).thenReturn("Status " + statusCode);

                assertThatThrownBy(() ->
                                                  errorHandler.handleFetchException(clientException, NOTIFICATION_ID)
                    )
                    .isInstanceOf(NotificationException.class)
                    .hasMessage("Failed to fetch notification, please try again.")
                    .hasCause(clientException);
            }
        }
    }

    @Nested
    @DisplayName("NotificationStatusUpdate Tests")
    class NotificationStatusUpdateTests {

        @Test
        @DisplayName("Should create NotificationStatusUpdate with all parameters")
        void shouldCreateNotificationStatusUpdateWithAllParameters() {
            UUID providerNotificationId = UUID.randomUUID();
            NotificationStatus status = NotificationStatus.SUBMITTED;

            NotificationErrorHandler.NotificationStatusUpdate statusUpdate =
                new NotificationErrorHandler.NotificationStatusUpdate(
                    mockCaseNotification,
                    status,
                    providerNotificationId
                );

            assertThat(statusUpdate.notification()).isEqualTo(mockCaseNotification);
            assertThat(statusUpdate.status()).isEqualTo(status);
            assertThat(statusUpdate.providerNotificationId()).isEqualTo(providerNotificationId);
        }

        @Test
        @DisplayName("Should create NotificationStatusUpdate with null provider ID")
        void shouldCreateNotificationStatusUpdateWithNullProviderID() {
            NotificationStatus status = NotificationStatus.PERMANENT_FAILURE;

            NotificationErrorHandler.NotificationStatusUpdate statusUpdate =
                new NotificationErrorHandler.NotificationStatusUpdate(
                    mockCaseNotification,
                    status,
                    null
                );

            assertThat(statusUpdate.notification()).isEqualTo(mockCaseNotification);
            assertThat(statusUpdate.status()).isEqualTo(status);
            assertThat(statusUpdate.providerNotificationId()).isNull();
        }
    }
}
