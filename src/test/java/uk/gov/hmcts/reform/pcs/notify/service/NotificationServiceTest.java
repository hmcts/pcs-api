package uk.gov.hmcts.reform.pcs.notify.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import uk.gov.hmcts.reform.pcs.config.AsyncConfiguration;
import uk.gov.hmcts.reform.pcs.notify.domain.CaseNotification;
import uk.gov.hmcts.reform.pcs.notify.exception.NotificationException;
import uk.gov.hmcts.reform.pcs.notify.model.EmailNotificationRequest;
import uk.gov.hmcts.reform.pcs.notify.model.NotificationStatus;
import uk.gov.hmcts.reform.pcs.notify.repository.NotificationRepository;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;
import uk.gov.service.notify.Notification;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SpringJUnitConfig(AsyncConfiguration.class)
class NotificationServiceTest {

    @Mock
    private NotificationClient notificationClient;

    @Mock
    private NotificationRepository notificationRepository;

    private NotificationService notificationService;

    private static final long STATUS_CHECK_DELAY = 100L;
    private static final int MAX_STATUS_CHECK_RETRIES = 3;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
            notificationClient, notificationRepository, STATUS_CHECK_DELAY, MAX_STATUS_CHECK_RETRIES);
    }

    @DisplayName("Should successfully send email when input data is valid")
    @Test
    void testSendEmailSuccess() throws NotificationClientException {
        final EmailNotificationRequest emailRequest = new EmailNotificationRequest(
            "test@example.com",
            "templateId",
            new HashMap<>(),
            "reference",
            "emailReplyToId"
        );
        
        CaseNotification caseNotification = new CaseNotification();
        UUID notificationId = UUID.randomUUID();
        caseNotification.setNotificationId(notificationId);
        
        SendEmailResponse sendEmailResponse = mock(SendEmailResponse.class);
        UUID providerNotificationId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        
        Notification govNotifyResponse = mock(Notification.class);
        when(govNotifyResponse.getStatus()).thenReturn("delivered");
        
        when(notificationRepository.save(any(CaseNotification.class))).thenReturn(caseNotification);
        when(sendEmailResponse.getNotificationId()).thenReturn(providerNotificationId);
        when(sendEmailResponse.getReference()).thenReturn(Optional.of("reference"));
        when(notificationClient.sendEmail(anyString(), anyString(), anyMap(), anyString()))
            .thenReturn(sendEmailResponse);
        when(notificationClient.getNotificationById(providerNotificationId.toString()))
            .thenReturn(govNotifyResponse);
        when(notificationRepository.findById(any(UUID.class))).thenReturn(Optional.of(caseNotification));
        when(notificationRepository.findByProviderNotificationId(providerNotificationId))
            .thenReturn(Optional.of(caseNotification));

        SendEmailResponse response = notificationService.sendEmail(emailRequest);

        assertThat(response).isNotNull();
        assertThat(response.getNotificationId()).isEqualTo(providerNotificationId);
        assertThat(response.getReference()).contains("reference");
        verify(notificationClient).sendEmail(anyString(), anyString(), anyMap(), anyString());
        // The save method is called multiple times internally
        verify(notificationRepository, times(3)).save(any(CaseNotification.class));
    }

    @DisplayName("Should throw notification exception when email sending fails")
    @Test
    void testSendEmailFailure() throws NotificationClientException {
        final EmailNotificationRequest emailRequest = new EmailNotificationRequest(
            "test@example.com",
            "templateId",
            new HashMap<>(),
            "reference",
            "emailReplyToId"
        );
        when(notificationRepository.save(any(CaseNotification.class))).thenReturn(mock(CaseNotification.class));
        when(notificationClient.sendEmail(anyString(), anyString(), anyMap(), anyString()))
            .thenThrow(new NotificationClientException("Error"));

        assertThatThrownBy(() -> notificationService.sendEmail(emailRequest))
            .isInstanceOf(NotificationException.class)
            .hasMessage("Email failed to send, please try again.");

        verify(notificationClient).sendEmail(anyString(), anyString(), anyMap(), anyString());
        verify(notificationRepository).save(any(CaseNotification.class));
    }

    @DisplayName("Should save case notification when end point is called successfully")
    @Test
    void shouldSaveCaseNotificationWhenEndPointIsCalled() {
        String recipient = "test@example.com";
        String status = "pending-schedule";
        UUID caseId = UUID.randomUUID();
        String type = "Email";

        CaseNotification testCaseNotification = new CaseNotification();
        testCaseNotification.setStatus(status);
        testCaseNotification.setRecipient(recipient);
        testCaseNotification.setCaseId(caseId);
        testCaseNotification.setType(type);

        when(notificationRepository.save(any(CaseNotification.class))).thenReturn(testCaseNotification);
        CaseNotification saved = notificationService.createCaseNotification(recipient, type, caseId);

        assertThat(saved).isNotNull();
        assertThat(saved.getCaseId()).isEqualTo(testCaseNotification.getCaseId());
        assertThat(saved.getRecipient()).isEqualTo(testCaseNotification.getRecipient());
        verify(notificationRepository).save(any(CaseNotification.class));
    }

    @DisplayName("Should throw notification exception when saving of notification fails")
    @Test
    void shouldThrowNotificationExceptionWhenSavingFails() throws DataIntegrityViolationException {
        String recipient = "test@example.com";
        String type = "Email";
        UUID caseId = UUID.randomUUID();

        when(notificationRepository.save(any(CaseNotification.class)))
            .thenThrow(new DataIntegrityViolationException("Constraint violation"));

        assertThatThrownBy(() -> 
            notificationService.createCaseNotification(recipient, type, caseId)
        ).isInstanceOf(NotificationException.class).hasMessage("Failed to save Case Notification.");
        verify(notificationRepository).save(any(CaseNotification.class));
    }

    @DisplayName("Should check notification status delivered")
    @Test
    void testCheckNotificationStatusDelivered() throws NotificationClientException, 
            InterruptedException, ExecutionException, TimeoutException {
        String notificationId = UUID.randomUUID().toString();
        Notification notification = mock(Notification.class);
        
        when(notification.getStatus()).thenReturn("delivered");
        when(notificationClient.getNotificationById(notificationId)).thenReturn(notification);

        CompletableFuture<Notification> future = notificationService.checkNotificationStatus(notificationId);
        Notification result = future.get(6, TimeUnit.SECONDS);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("delivered");
        verify(notificationClient).getNotificationById(notificationId);
    }

    @DisplayName("Should check notification status pending")
    @Test
    void testCheckNotificationStatusPending() throws NotificationClientException, 
            InterruptedException, ExecutionException, TimeoutException {
        String notificationId = UUID.randomUUID().toString();
        Notification notification = mock(Notification.class);

        when(notification.getStatus()).thenReturn("pending");
        when(notificationClient.getNotificationById(notificationId)).thenReturn(notification);

        CompletableFuture<Notification> future = notificationService.checkNotificationStatus(notificationId);
        Notification result = future.get(6, TimeUnit.SECONDS);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("pending");
        verify(notificationClient).getNotificationById(notificationId);
    }

    @DisplayName("Should handle error when checking notification status")
    @Test
    void testCheckNotificationStatusError() throws NotificationClientException {
        String notificationId = UUID.randomUUID().toString();
        when(notificationClient.getNotificationById(notificationId))
            .thenThrow(new NotificationClientException("Failed to fetch notification status"));

        CompletableFuture<Notification> future = notificationService.checkNotificationStatus(notificationId);
        
        assertThatThrownBy(() -> future.get(6, TimeUnit.SECONDS))
            .isInstanceOf(ExecutionException.class)
            .satisfies(thrown -> {
                assertThat(thrown.getCause())
                    .isInstanceOf(NotificationClientException.class)
                    .hasMessage("Failed to fetch notification status");
            });

        verify(notificationClient).getNotificationById(notificationId);
    }

    @DisplayName("Should update notification status after checking Gov Notify")
    @Test
    void shouldUpdateNotificationStatusAfterCheck() throws NotificationClientException {
        final String recipient = "test@example.com";
        final String type = "Email";
        final UUID caseId = UUID.randomUUID();
        final UUID notificationId = UUID.randomUUID();
        final UUID providerNotificationId = UUID.randomUUID();
        final String status = "delivered";
        
        CaseNotification savedNotification = new CaseNotification();
        savedNotification.setNotificationId(notificationId);
        savedNotification.setProviderNotificationId(providerNotificationId);
        savedNotification.setStatus(NotificationStatus.SENDING.toString());
        savedNotification.setRecipient(recipient);
        savedNotification.setType(type);
        savedNotification.setCaseId(caseId);
        
        Notification notification = mock(Notification.class);
        when(notification.getStatus()).thenReturn(status);
        when(notificationClient.getNotificationById(providerNotificationId.toString())).thenReturn(notification);
        
        when(notificationRepository.findByProviderNotificationId(providerNotificationId))
            .thenReturn(Optional.of(savedNotification));
        when(notificationRepository.save(any(CaseNotification.class))).thenReturn(savedNotification);
        
        notificationService.scheduleStatusCheck(providerNotificationId.toString(), 0);
        
        verify(notificationRepository).findByProviderNotificationId(providerNotificationId);
        verify(notificationRepository).save(savedNotification);
        assertThat(savedNotification.getStatus()).isEqualTo(status);
    }
}
