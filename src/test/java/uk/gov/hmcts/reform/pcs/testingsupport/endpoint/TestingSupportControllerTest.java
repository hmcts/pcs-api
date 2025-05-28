package uk.gov.hmcts.reform.pcs.testingsupport.endpoint;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.Task;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pcs.notify.model.EmailNotificationRequest;
import uk.gov.hmcts.reform.pcs.notify.service.NotificationService;
import uk.gov.service.notify.SendEmailResponse;

import java.net.URI;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestingSupportControllerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private SchedulerClient schedulerClient;

    @Mock
    private Task<Void> helloWorldTask;

    @InjectMocks
    private TestingSupportController underTest;

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

        ResponseEntity<SendEmailResponse> response = underTest.sendEmail(
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

    @SuppressWarnings("unchecked")
    @Test
    void testScheduleHelloWorldTask_Success() {
        TaskInstance<Void> mockTaskInstance = mock(TaskInstance.class);
        when(helloWorldTask.instance(anyString())).thenReturn(mockTaskInstance);

        ResponseEntity<String> response = underTest.scheduleHelloWorldTask(5,
                                                                           "Bearer token",
                                                                           "ServiceAuthToken");

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).contains("Hello World task scheduled successfully with ID:");
        assertThat(response.getBody()).contains("execution time:");

        ArgumentCaptor<TaskInstance<Void>> taskInstanceCaptor = ArgumentCaptor.forClass(TaskInstance.class);
        ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);

        verify(schedulerClient).scheduleIfNotExists(taskInstanceCaptor.capture(), instantCaptor.capture());

        TaskInstance<Void> capturedTaskInstance = taskInstanceCaptor.getValue();
        Instant scheduledInstant = instantCaptor.getValue();

        assertThat(capturedTaskInstance).isSameAs(mockTaskInstance);
        assertThat(scheduledInstant).isAfterOrEqualTo(Instant.now().plusSeconds(4));
    }

    @SuppressWarnings("unchecked")
    @Test
    void testScheduleHelloWorldTask_Failure() {
        TaskInstance<Void> mockTaskInstance = mock(TaskInstance.class);
        when(helloWorldTask.instance(anyString())).thenReturn(mockTaskInstance);

        doThrow(new RuntimeException("Scheduler failure")).when(schedulerClient)
            .scheduleIfNotExists(any(TaskInstance.class), any(Instant.class));

        ResponseEntity<String> response = underTest.scheduleHelloWorldTask(2,
                                                                           "Bearer token",
                                                                           "ServiceAuthToken");

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).contains("Failed to schedule Hello World task");
        assertThat(response.getBody()).contains("Scheduler failure");
    }
}
