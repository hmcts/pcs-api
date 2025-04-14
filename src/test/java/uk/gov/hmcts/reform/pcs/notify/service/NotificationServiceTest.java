package uk.gov.hmcts.reform.pcs.notify.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import uk.gov.hmcts.reform.pcs.notify.domain.CaseNotification;
import uk.gov.hmcts.reform.pcs.notify.exception.NotificationException;
import uk.gov.hmcts.reform.pcs.notify.model.EmailNotificationRequest;
import uk.gov.hmcts.reform.pcs.notify.repository.NotificationRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationClient notificationClient;

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

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
        when(notificationRepository.save(any(CaseNotification.class))).thenReturn(mock(CaseNotification.class));
        when(sendEmailResponse.getNotificationId())
            .thenReturn(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        when(sendEmailResponse.getReference())
            .thenReturn(Optional.of("reference"));
        when(notificationClient.sendEmail(anyString(), anyString(), anyMap(), anyString()))
            .thenReturn(sendEmailResponse);

        SendEmailResponse response = notificationService.sendEmail(emailRequest);

        assertThat(response).isNotNull();
        assertThat(response.getNotificationId()).isEqualTo(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
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
        when(notificationRepository.save(any(CaseNotification.class))).thenReturn(mock(CaseNotification.class));
        when(notificationClient.sendEmail(anyString(), anyString(), anyMap(), anyString()))
            .thenThrow(new NotificationClientException("Error"));

        assertThatThrownBy(() -> notificationService.sendEmail(emailRequest))
            .isInstanceOf(NotificationException.class)
            .hasMessage("Email failed to send, please try again.");

        verify(notificationClient).sendEmail(anyString(), anyString(), anyMap(), anyString());
    }

    @Test
    void testSaveCaseNotification() {
        String recipient = "test@example.com";
        String status = "Schedule Pending";
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

    @Test
    void testIfNotificationExceptionThrownWhenSavingFails() throws DataIntegrityViolationException {
        String recipient = "test@example.com";
        String type = "Email";
        UUID caseId = UUID.randomUUID();

        when(notificationRepository.save(any(CaseNotification.class)))
            .thenThrow(new DataIntegrityViolationException("Constraint violation"));

        assertThatThrownBy(() -> notificationService.createCaseNotification(recipient, type, caseId))
            .isInstanceOf(NotificationException.class).hasMessage("Failed to save Case Notification.");
        verify(notificationRepository).save(any(CaseNotification.class));
    }

}
