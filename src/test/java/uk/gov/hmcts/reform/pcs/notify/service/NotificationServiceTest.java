package uk.gov.hmcts.reform.pcs.notify.service;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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

    @Mock
    private NotificationErrorHandler errorHandler;

    private NotificationService notificationService;

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
            errorHandler
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
            when(mockSendEmailResponse.getNotificationId()).thenReturn(PROVIDER_NOTIFICATION_ID);
            when(notificationRepository.save(any(CaseNotification.class)))
                .thenReturn(mockCaseNotification);
            when(notificationClient.sendEmail(
                anyString(), anyString(),
                ArgumentMatchers.anyMap(), anyString()))
                .thenReturn(mockSendEmailResponse);

            SendEmailResponse result = notificationService.sendEmail(emailRequest);

            assertThat(result).isEqualTo(mockSendEmailResponse);

            verify(notificationClient).sendEmail(
                eq(TEMPLATE_ID),
                eq(EMAIL_ADDRESS),
                eq(personalisation),
                anyString()
            );

            verify(notificationRepository, times(2)).save(any(CaseNotification.class));

            verifyNoInteractions(errorHandler);

            ArgumentCaptor<CaseNotification> notificationCaptor = ArgumentCaptor.forClass(CaseNotification.class);
            verify(notificationRepository, times(2)).save(notificationCaptor.capture());

            CaseNotification initialNotification = notificationCaptor.getAllValues().getFirst();
            assertThat(initialNotification.getRecipient()).isEqualTo(EMAIL_ADDRESS);
            assertThat(initialNotification.getType()).isEqualTo("Email");
            assertThat(initialNotification.getStatus()).isEqualTo(NotificationStatus.PENDING_SCHEDULE);

            CaseNotification updatedNotification = notificationCaptor.getAllValues().get(1);
            assertThat(updatedNotification.getStatus()).isEqualTo(NotificationStatus.SUBMITTED);
            assertThat(updatedNotification.getProviderNotificationId()).isEqualTo(PROVIDER_NOTIFICATION_ID);
            assertThat(updatedNotification.getLastUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should delegate exception handling to error handler")
        void shouldDelegateExceptionHandlingToErrorHandler() throws NotificationClientException {
            when(notificationRepository.save(any(CaseNotification.class)))
                .thenReturn(mockCaseNotification);
            NotificationClientException clientException = mock(NotificationClientException.class);
            when(notificationClient.sendEmail(
                anyString(), anyString(),
                ArgumentMatchers.anyMap(), anyString()))
                .thenThrow(clientException);

            doThrow(new PermanentNotificationException("Email failed to send.", clientException))
                .when(errorHandler)
                .handleSendEmailException(
                    eq(clientException),
                    eq(mockCaseNotification),
                    anyString(),
                    any()
                );

            assertThatThrownBy(() -> notificationService.sendEmail(emailRequest))
                .isInstanceOf(PermanentNotificationException.class)
                .hasMessage("Email failed to send.")
                .hasCause(clientException);

            verify(errorHandler).handleSendEmailException(
                eq(clientException),
                eq(mockCaseNotification),
                anyString(),
                any()
            );

            verify(notificationRepository).save(any(CaseNotification.class));
        }

        @Test
        @DisplayName("Should throw NotificationException when database save fails")
        void shouldThrowNotificationExceptionWhenDatabaseSaveFails() {
            DataAccessException databaseException = mock(DataAccessException.class);
            when(databaseException.getMessage()).thenReturn("Database connection failed");
            when(notificationRepository.save(any(CaseNotification.class)))
                .thenThrow(databaseException);

            assertThatThrownBy(() -> notificationService.sendEmail(emailRequest))
                .isInstanceOf(NotificationException.class)
                .hasMessage("Failed to save Case Notification.")
                .hasCause(databaseException);

            verifyNoInteractions(notificationClient);
            verifyNoInteractions(errorHandler);
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
            when(notificationRepository.save(any(CaseNotification.class)))
                .thenReturn(mockCaseNotification);

            Notification result = notificationService.fetchNotificationStatus(NOTIFICATION_ID_STRING);

            assertThat(result).isEqualTo(mockNotification);

            verify(notificationClient).getNotificationById(NOTIFICATION_ID_STRING);
            verify(notificationRepository).findByProviderNotificationId(PROVIDER_NOTIFICATION_ID);
            verify(notificationRepository).save(any(CaseNotification.class));

            verifyNoInteractions(errorHandler);
        }

        @Test
        @DisplayName("Should delegate fetch exception handling to error handler")
        void shouldDelegateFetchExceptionHandlingToErrorHandler() throws NotificationClientException {
            NotificationClientException clientException = mock(NotificationClientException.class);
            when(notificationClient.getNotificationById(NOTIFICATION_ID_STRING))
                .thenThrow(clientException);

            doThrow(new NotificationException("Failed to fetch notification, please try again.", clientException))
                .when(errorHandler)
                .handleFetchException(clientException, NOTIFICATION_ID_STRING);

            assertThatThrownBy(() -> notificationService.fetchNotificationStatus(NOTIFICATION_ID_STRING))
                .isInstanceOf(NotificationException.class)
                .hasMessage("Failed to fetch notification, please try again.")
                .hasCause(clientException);

            verify(errorHandler).handleFetchException(clientException, NOTIFICATION_ID_STRING);
            verifyNoInteractions(notificationRepository);
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

            assertThat(result).isEqualTo(mockNotification);

            verify(notificationClient).getNotificationById(NOTIFICATION_ID_STRING);
            verify(notificationRepository).findByProviderNotificationId(PROVIDER_NOTIFICATION_ID);
            verify(notificationRepository, never()).save(any(CaseNotification.class));
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

            assertThat(result).isEqualTo(mockNotification);
            verify(notificationClient).getNotificationById(NOTIFICATION_ID_STRING);
        }

        @Test
        @DisplayName("Should handle unknown notification status gracefully")
        void shouldHandleUnknownNotificationStatusGracefully()
            throws NotificationClientException, InterruptedException {
            when(mockNotification.getStatus()).thenReturn("unknown_status");

            when(notificationClient.getNotificationById(NOTIFICATION_ID_STRING))
                .thenReturn(mockNotification);
            when(notificationRepository.findByProviderNotificationId(PROVIDER_NOTIFICATION_ID))
                .thenReturn(Optional.of(mockCaseNotification));

            Notification result = notificationService.fetchNotificationStatus(NOTIFICATION_ID_STRING);

            assertThat(result).isEqualTo(mockNotification);
            verify(notificationRepository, never()).save(any(CaseNotification.class));
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

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

            when(notificationRepository.save(any(CaseNotification.class)))
                .thenReturn(mockCaseNotification);
            when(notificationClient.sendEmail(
                anyString(), anyString(),
                ArgumentMatchers.isNull(), anyString()))
                .thenReturn(mockSendEmailResponse);

            SendEmailResponse result = notificationService.sendEmail(emailRequest);

            assertThat(result).isEqualTo(mockSendEmailResponse);
            verify(notificationClient).sendEmail(
                eq(TEMPLATE_ID),
                eq(EMAIL_ADDRESS),
                ArgumentMatchers.isNull(),
                anyString()
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

            when(notificationRepository.save(any(CaseNotification.class)))
                .thenReturn(mockCaseNotification);
            when(notificationClient.sendEmail(
                anyString(), anyString(),
                ArgumentMatchers.anyMap(), anyString()))
                .thenReturn(mockSendEmailResponse);

            SendEmailResponse result = notificationService.sendEmail(emailRequest);

            assertThat(result).isEqualTo(mockSendEmailResponse);
            verify(notificationClient).sendEmail(
                eq(TEMPLATE_ID),
                eq(EMAIL_ADDRESS),
                eq(new HashMap<>()),
                anyString()
            );
        }
    }
}
