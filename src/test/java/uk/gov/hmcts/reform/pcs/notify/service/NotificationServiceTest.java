package uk.gov.hmcts.reform.pcs.notify.service;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import uk.gov.hmcts.reform.pcs.notify.domain.CaseNotification;
import uk.gov.hmcts.reform.pcs.notify.exception.NotificationException;
import uk.gov.hmcts.reform.pcs.notify.exception.PermanentNotificationException;
import uk.gov.hmcts.reform.pcs.notify.exception.TemporaryNotificationException;
import uk.gov.hmcts.reform.pcs.notify.model.EmailNotificationRequest;
import uk.gov.hmcts.reform.pcs.notify.model.NotificationStatus;
import uk.gov.hmcts.reform.pcs.notify.repository.NotificationRepository;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;
import uk.gov.service.notify.Notification;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService Tests")
class NotificationServiceTest {

    @Mock
    private NotificationClient notificationClient;

    @Mock
    private NotificationRepository notificationRepository;

    private NotificationService notificationService;

    private static final long STATUS_CHECK_DELAY = 1000L;
    private static final String EMAIL_ADDRESS = "test@example.com";
    private static final String TEMPLATE_ID = "template-123";
    private static final UUID CASE_ID = UUID.randomUUID();
    private static final UUID NOTIFICATION_ID = UUID.randomUUID();
    private static final UUID PROVIDER_NOTIFICATION_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
            notificationClient,
            notificationRepository,
            STATUS_CHECK_DELAY
        );
    }

    @Nested
    @DisplayName("Send Email Tests")
    class SendEmailTests {

        private EmailNotificationRequest emailRequest;
        private Map<String, Object> personalisation;
        private CaseNotification mockCaseNotification;
        private SendEmailResponse mockSendEmailResponse;

        @BeforeEach
        void setUp() {
            personalisation = new HashMap<>();
            personalisation.put("name", "John Doe");
            personalisation.put("caseNumber", "12345");

            emailRequest = new EmailNotificationRequest();
            emailRequest.setEmailAddress(EMAIL_ADDRESS);
            emailRequest.setTemplateId(TEMPLATE_ID);
            emailRequest.setPersonalisation(personalisation);

            mockCaseNotification = new CaseNotification();
            mockCaseNotification.setNotificationId(NOTIFICATION_ID);
            mockCaseNotification.setCaseId(CASE_ID);
            mockCaseNotification.setStatus(NotificationStatus.PENDING_SCHEDULE);
            mockCaseNotification.setType("Email");
            mockCaseNotification.setRecipient(EMAIL_ADDRESS);

            mockSendEmailResponse = mock(SendEmailResponse.class);
        }

        @Test
        @DisplayName("Should successfully send email and update notification status")
        void shouldSuccessfullySendEmailAndUpdateNotificationStatus() throws NotificationClientException {
            // Given
            when(mockSendEmailResponse.getNotificationId()).thenReturn(PROVIDER_NOTIFICATION_ID);
            when(notificationRepository.save(ArgumentMatchers.any(CaseNotification.class)))
                .thenReturn(mockCaseNotification);
            when(notificationClient.sendEmail(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                                                ArgumentMatchers.anyMap(), ArgumentMatchers.anyString()))
                .thenReturn(mockSendEmailResponse);

            SendEmailResponse result = notificationService.sendEmail(emailRequest);

            Assertions.assertThat(result).isEqualTo(mockSendEmailResponse);

            verify(notificationClient).sendEmail(
                ArgumentMatchers.eq(TEMPLATE_ID),
                ArgumentMatchers.eq(EMAIL_ADDRESS),
                ArgumentMatchers.eq(personalisation),
                ArgumentMatchers.anyString()
            );

            verify(notificationRepository, times(2)).save(ArgumentMatchers
                                                                .any(CaseNotification.class));

            ArgumentCaptor<CaseNotification> notificationCaptor = ArgumentCaptor.forClass(CaseNotification.class);
            verify(notificationRepository, times(2)).save(notificationCaptor.capture());

            CaseNotification initialNotification = notificationCaptor.getAllValues().get(0);
            Assertions.assertThat(initialNotification.getRecipient()).isEqualTo(EMAIL_ADDRESS);
            Assertions.assertThat(initialNotification.getType()).isEqualTo("Email");
            Assertions.assertThat(initialNotification.getStatus()).isEqualTo(NotificationStatus.PENDING_SCHEDULE);

            CaseNotification updatedNotification = notificationCaptor.getAllValues().get(1);
            Assertions.assertThat(updatedNotification.getStatus()).isEqualTo(NotificationStatus.SUBMITTED);
            Assertions.assertThat(updatedNotification.getProviderNotificationId()).isEqualTo(PROVIDER_NOTIFICATION_ID);
            Assertions.assertThat(updatedNotification.getLastUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should throw PermanentNotificationException for 400 status code")
        void shouldThrowPermanentNotificationExceptionFor400StatusCode() throws NotificationClientException {
            when(notificationRepository.save(ArgumentMatchers.any(CaseNotification.class)))
                .thenReturn(mockCaseNotification);
            NotificationClientException clientException = mock(NotificationClientException.class);
            when(clientException.getHttpResult()).thenReturn(400);
            when(clientException.getMessage()).thenReturn("Bad Request");
            when(notificationClient.sendEmail(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                                                ArgumentMatchers.anyMap(), ArgumentMatchers.anyString()))
                .thenThrow(clientException);

            Assertions.assertThatThrownBy(() -> notificationService.sendEmail(emailRequest))
                .isInstanceOf(PermanentNotificationException.class)
                .hasMessage("Email failed to send.")
                .hasCause(clientException);

            ArgumentCaptor<CaseNotification> notificationCaptor = ArgumentCaptor.forClass(CaseNotification.class);
            verify(notificationRepository, times(2)).save(notificationCaptor.capture());

            CaseNotification updatedNotification = notificationCaptor.getAllValues().get(1);
            Assertions.assertThat(updatedNotification.getStatus()).isEqualTo(NotificationStatus.PERMANENT_FAILURE);
        }

        @Test
        @DisplayName("Should throw PermanentNotificationException for 403 status code")
        void shouldThrowPermanentNotificationExceptionFor403StatusCode() throws NotificationClientException {
            when(notificationRepository.save(ArgumentMatchers.any(CaseNotification.class)))
                .thenReturn(mockCaseNotification);
            NotificationClientException clientException = mock(NotificationClientException.class);
            when(clientException.getHttpResult()).thenReturn(403);
            when(clientException.getMessage()).thenReturn("Forbidden");
            when(notificationClient.sendEmail(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                                                ArgumentMatchers.anyMap(), ArgumentMatchers.anyString()))
                .thenThrow(clientException);

            Assertions.assertThatThrownBy(() -> notificationService.sendEmail(emailRequest))
                .isInstanceOf(PermanentNotificationException.class)
                .hasMessage("Email failed to send.")
                .hasCause(clientException);
        }

        @Test
        @DisplayName("Should throw TemporaryNotificationException for 429 status code")
        void shouldThrowTemporaryNotificationExceptionFor429StatusCode() throws NotificationClientException {
            when(notificationRepository.save(ArgumentMatchers.any(CaseNotification.class)))
                .thenReturn(mockCaseNotification);
            NotificationClientException clientException = mock(NotificationClientException.class);
            when(clientException.getHttpResult()).thenReturn(429);
            when(clientException.getMessage()).thenReturn("Too Many Requests");
            when(notificationClient.sendEmail(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                                                ArgumentMatchers.anyMap(), ArgumentMatchers.anyString()))
                .thenThrow(clientException);

            Assertions.assertThatThrownBy(() -> notificationService.sendEmail(emailRequest))
                .isInstanceOf(TemporaryNotificationException.class)
                .hasMessage("Email temporarily failed to send.")
                .hasCause(clientException);

            ArgumentCaptor<CaseNotification> notificationCaptor = ArgumentCaptor.forClass(CaseNotification.class);
            verify(notificationRepository, times(2)).save(notificationCaptor.capture());

            CaseNotification updatedNotification = notificationCaptor.getAllValues().get(1);
            Assertions.assertThat(updatedNotification.getStatus()).isEqualTo(NotificationStatus.TEMPORARY_FAILURE);
        }

        @Test
        @DisplayName("Should throw TemporaryNotificationException for 500 status code")
        void shouldThrowTemporaryNotificationExceptionFor500StatusCode() throws NotificationClientException {
            when(notificationRepository.save(ArgumentMatchers.any(CaseNotification.class)))
                .thenReturn(mockCaseNotification);
            NotificationClientException clientException = mock(NotificationClientException.class);
            when(clientException.getHttpResult()).thenReturn(500);
            when(clientException.getMessage()).thenReturn("Internal Server Error");
            when(notificationClient.sendEmail(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                                                ArgumentMatchers.anyMap(), ArgumentMatchers.anyString()))
                .thenThrow(clientException);

            Assertions.assertThatThrownBy(() -> notificationService.sendEmail(emailRequest))
                .isInstanceOf(TemporaryNotificationException.class)
                .hasMessage("Email temporarily failed to send.")
                .hasCause(clientException);
        }

        @Test
        @DisplayName("Should throw NotificationException for other status codes")
        void shouldThrowNotificationExceptionForOtherStatusCodes() throws NotificationClientException {
            when(notificationRepository.save(ArgumentMatchers.any(CaseNotification.class)))
                .thenReturn(mockCaseNotification);
            NotificationClientException clientException = mock(NotificationClientException.class);
            when(clientException.getHttpResult()).thenReturn(502);
            when(clientException.getMessage()).thenReturn("Bad Gateway");
            when(notificationClient.sendEmail(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                                                ArgumentMatchers.anyMap(), ArgumentMatchers.anyString()))
                .thenThrow(clientException);

            Assertions.assertThatThrownBy(() -> notificationService.sendEmail(emailRequest))
                .isInstanceOf(NotificationException.class)
                .hasMessage("Email failed to send, please try again.")
                .hasCause(clientException);

            ArgumentCaptor<CaseNotification> notificationCaptor = ArgumentCaptor.forClass(CaseNotification.class);
            verify(notificationRepository, times(2)).save(notificationCaptor.capture());

            CaseNotification updatedNotification = notificationCaptor.getAllValues().get(1);
            Assertions.assertThat(updatedNotification.getStatus()).isEqualTo(NotificationStatus.TECHNICAL_FAILURE);
        }

        @Test
        @DisplayName("Should throw NotificationException when database save fails")
        void shouldThrowNotificationExceptionWhenDatabaseSaveFails() {
            DataAccessException databaseException = mock(DataAccessException.class);
            when(databaseException.getMessage()).thenReturn("Database connection failed");
            when(notificationRepository.save(ArgumentMatchers.any(CaseNotification.class)))
                .thenThrow(databaseException);

            Assertions.assertThatThrownBy(() -> notificationService.sendEmail(emailRequest))
                .isInstanceOf(NotificationException.class)
                .hasMessage("Failed to save Case Notification.")
                .hasCause(databaseException);

            verifyNoInteractions(notificationClient);
        }
    }

    @Nested
    @DisplayName("Fetch Notification Status Tests")
    class FetchNotificationStatusTests {

        private static final String NOTIFICATION_ID_STRING = PROVIDER_NOTIFICATION_ID.toString();
        private Notification mockNotification;
        private CaseNotification mockCaseNotification;

        @BeforeEach
        void setUp() {
            mockNotification = mock(Notification.class);

            mockCaseNotification = new CaseNotification();
            mockCaseNotification.setNotificationId(NOTIFICATION_ID);
            mockCaseNotification.setProviderNotificationId(PROVIDER_NOTIFICATION_ID);
            mockCaseNotification.setStatus(NotificationStatus.SUBMITTED);
        }

        @Test
        @DisplayName("Should successfully fetch notification status and update database")
        void shouldSuccessfullyFetchNotificationStatusAndUpdateDatabase()
            throws NotificationClientException, InterruptedException {
            when(mockNotification.getStatus()).thenReturn("delivered");
            when(notificationClient.getNotificationById(NOTIFICATION_ID_STRING))
                .thenReturn(mockNotification);
            when(notificationRepository.findByProviderNotificationId(PROVIDER_NOTIFICATION_ID))
                .thenReturn(Optional.of(mockCaseNotification));
            when(notificationRepository.save(ArgumentMatchers.any(CaseNotification.class)))
                .thenReturn(mockCaseNotification);

            Notification result = notificationService.fetchNotificationStatus(NOTIFICATION_ID_STRING);

            Assertions.assertThat(result).isEqualTo(mockNotification);

            verify(notificationClient).getNotificationById(NOTIFICATION_ID_STRING);

            verify(notificationRepository).findByProviderNotificationId(PROVIDER_NOTIFICATION_ID);

            verify(notificationRepository).save(ArgumentMatchers.any(CaseNotification.class));
        }

        @Test
        @DisplayName("Should handle case when notification not found in database")
        void shouldHandleCaseWhenNotificationNotFoundInDatabase()
            throws NotificationClientException, InterruptedException {
            when(notificationClient.getNotificationById(NOTIFICATION_ID_STRING))
                .thenReturn(mockNotification);
            when(notificationRepository.findByProviderNotificationId(PROVIDER_NOTIFICATION_ID))
                .thenReturn(Optional.empty());

            Notification result = notificationService.fetchNotificationStatus(NOTIFICATION_ID_STRING);

            Assertions.assertThat(result).isEqualTo(mockNotification);

            verify(notificationClient).getNotificationById(NOTIFICATION_ID_STRING);

            verify(notificationRepository).findByProviderNotificationId(PROVIDER_NOTIFICATION_ID);

            verify(notificationRepository, never()).save(ArgumentMatchers.any(CaseNotification.class));
        }

        @Test
        @DisplayName("Should throw NotificationException when client throws exception")
        void shouldThrowNotificationExceptionWhenClientThrowsException() throws NotificationClientException {
            NotificationClientException clientException = mock(NotificationClientException.class);
            when(clientException.getHttpResult()).thenReturn(404);
            when(clientException.getMessage()).thenReturn("Not Found");
            when(notificationClient.getNotificationById(NOTIFICATION_ID_STRING))
                .thenThrow(clientException);

            Assertions.assertThatThrownBy(() -> notificationService.fetchNotificationStatus(NOTIFICATION_ID_STRING))
                .isInstanceOf(NotificationException.class)
                .hasMessage("Email failed to send, please try again.")
                .hasCause(clientException);

            verifyNoInteractions(notificationRepository);
        }

        @Test
        @DisplayName("Should handle database errors gracefully during status update")
        void shouldHandleDatabaseErrorsGracefullyDuringStatusUpdate()
            throws NotificationClientException, InterruptedException {
            when(notificationClient.getNotificationById(NOTIFICATION_ID_STRING))
                .thenReturn(mockNotification);
            when(notificationRepository.findByProviderNotificationId(PROVIDER_NOTIFICATION_ID))
                .thenThrow(new RuntimeException("Database error"));

            Notification result = notificationService.fetchNotificationStatus(NOTIFICATION_ID_STRING);

            Assertions.assertThat(result).isEqualTo(mockNotification);

            verify(notificationClient).getNotificationById(NOTIFICATION_ID_STRING);
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling Tests")
    class EdgeCasesAndErrorHandlingTests {

        @Test
        @DisplayName("Should handle null personalisation in email request")
        void shouldHandleNullPersonalisationInEmailRequest() throws NotificationClientException {
            EmailNotificationRequest emailRequest = new EmailNotificationRequest();
            emailRequest.setEmailAddress(EMAIL_ADDRESS);
            emailRequest.setTemplateId(TEMPLATE_ID);
            emailRequest.setPersonalisation(null);

            CaseNotification mockCaseNotification = new CaseNotification();
            mockCaseNotification.setNotificationId(NOTIFICATION_ID);
            mockCaseNotification.setStatus(NotificationStatus.PENDING_SCHEDULE);

            SendEmailResponse mockSendEmailResponse = mock(SendEmailResponse.class);
            when(mockSendEmailResponse.getNotificationId()).thenReturn(PROVIDER_NOTIFICATION_ID);

            when(notificationRepository.save(ArgumentMatchers.any(CaseNotification.class)))
                .thenReturn(mockCaseNotification);
            when(notificationClient.sendEmail(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                                                ArgumentMatchers.isNull(), ArgumentMatchers.anyString()))
                .thenReturn(mockSendEmailResponse);

            SendEmailResponse result = notificationService.sendEmail(emailRequest);

            Assertions.assertThat(result).isEqualTo(mockSendEmailResponse);
            verify(notificationClient).sendEmail(
                ArgumentMatchers.eq(TEMPLATE_ID),
                ArgumentMatchers.eq(EMAIL_ADDRESS),
                ArgumentMatchers.isNull(),
                ArgumentMatchers.anyString()
            );
        }

        @Test
        @DisplayName("Should handle empty personalisation in email request")
        void shouldHandleEmptyPersonalisationInEmailRequest() throws NotificationClientException {
            EmailNotificationRequest emailRequest = new EmailNotificationRequest();
            emailRequest.setEmailAddress(EMAIL_ADDRESS);
            emailRequest.setTemplateId(TEMPLATE_ID);
            emailRequest.setPersonalisation(new HashMap<>());

            CaseNotification mockCaseNotification = new CaseNotification();
            mockCaseNotification.setNotificationId(NOTIFICATION_ID);
            mockCaseNotification.setStatus(NotificationStatus.PENDING_SCHEDULE);

            SendEmailResponse mockSendEmailResponse = mock(SendEmailResponse.class);
            when(mockSendEmailResponse.getNotificationId()).thenReturn(PROVIDER_NOTIFICATION_ID);

            when(notificationRepository.save(ArgumentMatchers.any(CaseNotification.class)))
                .thenReturn(mockCaseNotification);
            when(notificationClient.sendEmail(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                                                ArgumentMatchers.anyMap(), ArgumentMatchers.anyString()))
                .thenReturn(mockSendEmailResponse);

            SendEmailResponse result = notificationService.sendEmail(emailRequest);

            Assertions.assertThat(result).isEqualTo(mockSendEmailResponse);
            verify(notificationClient).sendEmail(
                ArgumentMatchers.eq(TEMPLATE_ID),
                ArgumentMatchers.eq(EMAIL_ADDRESS),
                ArgumentMatchers.eq(new HashMap<>()),
                ArgumentMatchers.anyString()
            );
        }

        @Test
        @DisplayName("Should handle unknown notification status gracefully")
        void shouldHandleUnknownNotificationStatusGracefully()
            throws NotificationClientException, InterruptedException {
            final String notificationIdString = PROVIDER_NOTIFICATION_ID.toString();
            Notification mockNotification = mock(Notification.class);
            when(mockNotification.getStatus()).thenReturn("unknown_status");

            CaseNotification mockCaseNotification = new CaseNotification();
            mockCaseNotification.setNotificationId(NOTIFICATION_ID);
            mockCaseNotification.setProviderNotificationId(PROVIDER_NOTIFICATION_ID);
            mockCaseNotification.setStatus(NotificationStatus.SUBMITTED);

            when(notificationClient.getNotificationById(notificationIdString))
                .thenReturn(mockNotification);
            when(notificationRepository.findByProviderNotificationId(PROVIDER_NOTIFICATION_ID))
                .thenReturn(Optional.of(mockCaseNotification));

            Notification result = notificationService.fetchNotificationStatus(notificationIdString);

            Assertions.assertThat(result).isEqualTo(mockNotification);

            verify(notificationRepository, never()).save(ArgumentMatchers.any(CaseNotification.class));
        }
    }
}
