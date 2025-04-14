package uk.gov.hmcts.reform.pcs.notify.endpoint;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pcs.notify.model.EmailNotificationRequest;
import uk.gov.hmcts.reform.pcs.notify.service.NotificationService;
import uk.gov.service.notify.SendEmailResponse;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotifyController notifyController;

    @Test
    void testSendEmail_Success() {
        UUID notificationId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        URI unsubscribeUrl = URI.create("https://unsubscribe.example.com");

        EmailNotificationRequest emailRequest = new EmailNotificationRequest(
            "test@example.com",
            "templateId",
            null,
            "reference",
            "emailReplyToId"
        );

        SendEmailResponse mockResponse = mock(SendEmailResponse.class);
        when(mockResponse.getNotificationId()).thenReturn(notificationId);
        when(mockResponse.getReference()).thenReturn(Optional.of("reference"));
        when(mockResponse.getOneClickUnsubscribeURL()).thenReturn(Optional.of(unsubscribeUrl));
        when(mockResponse.getTemplateId()).thenReturn(templateId);
        when(mockResponse.getTemplateVersion()).thenReturn(1);
        when(mockResponse.getTemplateUri()).thenReturn("/template/uri");
        when(mockResponse.getBody()).thenReturn("Email body content");
        when(mockResponse.getSubject()).thenReturn("Email subject");
        when(mockResponse.getFromEmail()).thenReturn(Optional.of("noreply@example.com"));

        when(notificationService.sendEmail(any(EmailNotificationRequest.class))).thenReturn(mockResponse);

        ResponseEntity<SendEmailResponse> response = notifyController.sendEmail(
            "Bearer token",
            "ServiceAuthToken",
            emailRequest
        );

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();

        SendEmailResponse responseBody = response.getBody();
        assertThat(responseBody.getNotificationId()).isEqualTo(notificationId);
        assertThat(responseBody.getReference()).contains("reference");
        assertThat(responseBody.getOneClickUnsubscribeURL()).contains(unsubscribeUrl);
        assertThat(responseBody.getTemplateId()).isEqualTo(templateId);
        assertThat(responseBody.getTemplateVersion()).isEqualTo(1);
        assertThat(responseBody.getTemplateUri()).isEqualTo("/template/uri");
        assertThat(responseBody.getBody()).isEqualTo("Email body content");
        assertThat(responseBody.getSubject()).isEqualTo("Email subject");
        assertThat(responseBody.getFromEmail()).contains("noreply@example.com");

        verify(notificationService).sendEmail(emailRequest);
    }
}
