package uk.gov.hmcts.reform.pcs.notify.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import uk.gov.hmcts.reform.pcs.config.AsyncConfiguration;
import uk.gov.hmcts.reform.pcs.notify.exception.NotificationException;
import uk.gov.hmcts.reform.pcs.notify.model.EmailNotificationRequest;
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
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SpringJUnitConfig(AsyncConfiguration.class)
class NotificationServiceTest {

    @Mock
    private NotificationClient notificationClient;

    private NotificationService notificationService;

    private static final long STATUS_CHECK_DELAY = 100L; // 100ms for faster tests

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationClient, STATUS_CHECK_DELAY);
    }

    @Test
    void testSendEmailSuccess() throws NotificationClientException {
        EmailNotificationRequest emailRequest = new EmailNotificationRequest(
            "test@example.com",
            "templateId",
            new HashMap<>(),
            "reference",
            "emailReplyToId"
        );
        SendEmailResponse sendEmailResponse = mock(SendEmailResponse.class);
        when(sendEmailResponse.getNotificationId())
            .thenReturn(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        when(sendEmailResponse.getReference())
            .thenReturn(Optional.of("reference"));
        when(notificationClient.sendEmail(anyString(), anyString(), anyMap(), anyString()))
            .thenReturn(sendEmailResponse);

        SendEmailResponse response = notificationService.sendEmail(emailRequest);

        assertThat(response).isNotNull();
        assertThat(response.getNotificationId())
            .isEqualTo(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        assertThat(response.getReference()).contains("reference");
        verify(notificationClient).sendEmail(anyString(), anyString(), anyMap(), anyString());
    }

    @Test
    void testSendEmailFailure() throws NotificationClientException {
        EmailNotificationRequest emailRequest = new EmailNotificationRequest(
            "test@example.com",
            "templateId",
            new HashMap<>(),
            "reference",
            "emailReplyToId"
        );
        when(notificationClient.sendEmail(anyString(), anyString(), anyMap(), anyString()))
            .thenThrow(new NotificationClientException("Error"));

        assertThatThrownBy(() -> notificationService.sendEmail(emailRequest))
            .isInstanceOf(NotificationException.class)
            .hasMessage("Email failed to send, please try again.");

        verify(notificationClient).sendEmail(anyString(), anyString(), anyMap(), anyString());
    }

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
}
