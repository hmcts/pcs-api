package uk.gov.hmcts.reform.pcs.notify.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.reform.pcs.notify.exception.NotificationException;
import uk.gov.hmcts.reform.pcs.notify.model.NotificationResponse;
import uk.gov.hmcts.reform.pcs.notify.model.SendEmail;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        SendEmail emailRequest = new SendEmail(
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

        NotificationResponse response = notificationService.sendEmail(emailRequest);

        assertNotNull(response);
        assertEquals(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"), response.getNotificationId());
        verify(notificationClient, times(1))
            .sendEmail(anyString(), anyString(), anyMap(), anyString());
    }

    @Test
    void testSendEmailFailure() throws NotificationClientException {
        SendEmail emailRequest = new SendEmail(
            "test@example.com",
            "templateId",
            new HashMap<>(),
            "reference",
            "emailReplyToId"
        );
        when(notificationClient.sendEmail(anyString(), anyString(), anyMap(), anyString()))
            .thenThrow(new NotificationClientException("Error"));

        NotificationException exception = assertThrows(NotificationException.class, () ->
            notificationService.sendEmail(emailRequest));

        assertEquals("uk.gov.service.notify.NotificationClientException: Error", exception.getMessage());
        verify(notificationClient, times(1))
            .sendEmail(anyString(), anyString(), anyMap(), anyString());
    }
}
