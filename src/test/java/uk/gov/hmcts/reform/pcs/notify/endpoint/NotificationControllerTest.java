package uk.gov.hmcts.reform.pcs.notify.endpoint;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pcs.notify.model.NotificationResponse;
import uk.gov.hmcts.reform.pcs.notify.model.SendEmail;
import uk.gov.hmcts.reform.pcs.notify.service.NotificationService;

import java.net.URI;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
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

        SendEmail emailRequest = new SendEmail(
            "test@example.com",
            "templateId",
            null,
            "reference",
            "emailReplyToId"
        );

        NotificationResponse mockResponse = NotificationResponse.builder()
            .notificationId(notificationId)
            .reference("reference")
            .oneClickUnsubscribeURL(unsubscribeUrl)
            .templateId(templateId)
            .templateVersion(1)
            .templateUri("/template/uri")
            .body("Email body content")
            .subject("Email subject")
            .fromEmail("noreply@example.com")
            .build();

        when(notificationService.sendEmail(any(SendEmail.class))).thenReturn(mockResponse);

        ResponseEntity<NotificationResponse> response = notifyController.sendEmail(
            "Bearer token",
            "ServiceAuthToken",
            emailRequest
        );

        assertNotNull(response);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertNotNull(response.getBody());

        NotificationResponse responseBody = response.getBody();
        assertEquals(notificationId, responseBody.getNotificationId());
        assertEquals("reference", responseBody.getReference());
        assertEquals(unsubscribeUrl, responseBody.getOneClickUnsubscribeURL());
        assertEquals(templateId, responseBody.getTemplateId());
        assertEquals(1, responseBody.getTemplateVersion());
        assertEquals("/template/uri", responseBody.getTemplateUri());
        assertEquals("Email body content", responseBody.getBody());
        assertEquals("Email subject", responseBody.getSubject());
        assertEquals("noreply@example.com", responseBody.getFromEmail());

        verify(notificationService, times(1)).sendEmail(emailRequest);
    }
}
