package uk.gov.hmcts.reform.pcs.notify.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.reform.pcs.notify.exception.NotificationException;
import uk.gov.hmcts.reform.pcs.notify.model.EmailNotificationRequest;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceTest {

    @Mock
    private NotificationClient notificationClient;

    @InjectMocks
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
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
        when(sendEmailResponse.getReference()).thenReturn(Optional.of("reference"));
        when(notificationClient.sendEmail(anyString(), anyString(), anyMap(), anyString()))
            .thenReturn(sendEmailResponse);

        SendEmailResponse response = notificationService.sendEmail(emailRequest);

        assertThat(response).isNotNull();
        assertThat(response.getNotificationId()).isEqualTo(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        verify(notificationClient, times(1))
            .sendEmail(anyString(), anyString(), anyMap(), anyString());
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

        verify(notificationClient, times(1))
            .sendEmail(anyString(), anyString(), anyMap(), anyString());
    }
}
