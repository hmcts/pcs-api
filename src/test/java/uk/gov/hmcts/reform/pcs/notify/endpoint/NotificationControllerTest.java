package uk.gov.hmcts.reform.pcs.notify.endpoint;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pcs.notify.model.EmailNotificationRequest;
import uk.gov.hmcts.reform.pcs.notify.service.NotificationService;
import uk.gov.service.notify.SendEmailResponse;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotifyController notifyController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

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

        assertNotNull(response);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertNotNull(response.getBody());

        SendEmailResponse responseBody = response.getBody();
        assertEquals(notificationId, responseBody.getNotificationId());
        assertEquals(Optional.of("reference"), responseBody.getReference());
        assertEquals(Optional.of(unsubscribeUrl), responseBody.getOneClickUnsubscribeURL());
        assertEquals(templateId, responseBody.getTemplateId());
        assertEquals(1, responseBody.getTemplateVersion());
        assertEquals("/template/uri", responseBody.getTemplateUri());
        assertEquals("Email body content", responseBody.getBody());
        assertEquals("Email subject", responseBody.getSubject());
        assertEquals(Optional.of("noreply@example.com"), responseBody.getFromEmail());

        verify(notificationService, times(1)).sendEmail(emailRequest);
    }
}
