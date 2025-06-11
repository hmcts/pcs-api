package uk.gov.hmcts.reform.pcs.notify.config;

import com.github.kagkarlsson.scheduler.task.CompletionHandler;
import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.CustomTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.notify.exception.PermanentNotificationException;
import uk.gov.hmcts.reform.pcs.notify.exception.TemporaryNotificationException;
import uk.gov.hmcts.reform.pcs.notify.model.EmailNotificationRequest;
import uk.gov.hmcts.reform.pcs.notify.model.NotificationStatus;
import uk.gov.hmcts.reform.pcs.notify.service.NotificationService;
import uk.gov.service.notify.Notification;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class EmailTaskConfigurationTest {

    @Mock
    private NotificationService notificationService;

    private final int maxRetries = 3;
    private final long backoffDelay = 10L;
    private final long statusCheckDelay = 30L;

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private SendEmailResponse sendEmailResponse;

    @Mock
    private Notification notification;

    private EmailTaskConfiguration emailTaskConfiguration;

    @Captor
    private ArgumentCaptor<EmailNotificationRequest> emailRequestCaptor;

    private EmailState testEmailState;
    private TaskInstance<EmailState> taskInstance;
    private final String testNotificationId = "test-notification-id-123";
    private final UUID testUuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    @BeforeEach
    void setUp() {
        emailTaskConfiguration = new EmailTaskConfiguration(
            notificationService,
            maxRetries,
            backoffDelay,
            statusCheckDelay
        );

        testEmailState = EmailState.builder()
            .id("test-email-1")
            .emailAddress("test@example.com")
            .templateId("template-123")
            .personalisation(Map.of("name", "John Doe"))
            .reference("ref-123")
            .emailReplyToId("reply-to-123")
            .build();

        taskInstance = new TaskInstance<>("send-email-task", "instance-1", testEmailState);
    }

    @Nested
    @DisplayName("SendEmailTask Tests")
    class SendEmailTaskTests {

        private CustomTask<EmailState> sendEmailTask;

        @BeforeEach
        void setUp() {
            sendEmailTask = emailTaskConfiguration.sendEmailTask();
        }

        @Test
        @DisplayName("Should successfully send email and schedule verification task")
        void shouldSuccessfullySendEmailAndScheduleVerification() {
            Mockito.when(sendEmailResponse.getNotificationId()).thenReturn(testUuid);
            Mockito.when(notificationService.sendEmail(any(EmailNotificationRequest.class)))
                .thenReturn(sendEmailResponse);

            CompletionHandler<EmailState> result = sendEmailTask.execute(taskInstance, executionContext);

            assertThat(result).isInstanceOf(CompletionHandler.OnCompleteReplace.class);

            Mockito.verify(notificationService).sendEmail(emailRequestCaptor.capture());
            EmailNotificationRequest capturedRequest = emailRequestCaptor.getValue();

            assertThat(capturedRequest.getEmailAddress()).isEqualTo("test@example.com");
            assertThat(capturedRequest.getTemplateId()).isEqualTo("template-123");
            assertThat(capturedRequest.getPersonalisation()).containsEntry("name", "John Doe");
            assertThat(capturedRequest.getReference()).isEqualTo("ref-123");
            assertThat(capturedRequest.getEmailReplyToId()).isEqualTo("reply-to-123");
        }

        @Test
        @DisplayName("Should handle permanent notification exception by removing task")
        void shouldHandlePermanentNotificationException() {
            PermanentNotificationException exception = new PermanentNotificationException(
                "Permanent error", new RuntimeException("Underlying cause"));
            Mockito.when(notificationService.sendEmail(any(EmailNotificationRequest.class)))
                .thenThrow(exception);

            CompletionHandler<EmailState> result = sendEmailTask.execute(taskInstance, executionContext);

            assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
            Mockito.verify(notificationService).sendEmail(any(EmailNotificationRequest.class));
        }

        @Test
        @DisplayName("Should rethrow temporary notification exception for retry")
        void shouldRethrowTemporaryNotificationException() {
            TemporaryNotificationException exception = new TemporaryNotificationException(
                "Temporary error", new RuntimeException("Underlying cause"));
            Mockito.when(notificationService.sendEmail(any(EmailNotificationRequest.class)))
                .thenThrow(exception);

            assertThatThrownBy(() -> sendEmailTask.execute(taskInstance, executionContext))
                .isInstanceOf(TemporaryNotificationException.class)
                .hasMessage("Temporary error");

            Mockito.verify(notificationService).sendEmail(any(EmailNotificationRequest.class));
        }

        @Test
        @DisplayName("Should build email request with all required fields")
        void shouldBuildEmailRequestWithAllFields() {
            Mockito.when(sendEmailResponse.getNotificationId()).thenReturn(testUuid);
            Mockito.when(notificationService.sendEmail(any(EmailNotificationRequest.class)))
                .thenReturn(sendEmailResponse);

            sendEmailTask.execute(taskInstance, executionContext);

            Mockito.verify(notificationService).sendEmail(emailRequestCaptor.capture());
            EmailNotificationRequest request = emailRequestCaptor.getValue();

            assertThat(request.getEmailAddress()).isEqualTo("test@example.com");
            assertThat(request.getTemplateId()).isEqualTo("template-123");
            assertThat(request.getReference()).isEqualTo("ref-123");
            assertThat(request.getEmailReplyToId()).isEqualTo("reply-to-123");
            assertThat(request.getPersonalisation()).isEqualTo(testEmailState.personalisation);
        }
    }

    @Nested
    @DisplayName("VerifyEmailTask Tests")
    class VerifyEmailTaskTests {

        private CustomTask<EmailState> verifyEmailTask;
        private EmailState emailStateWithNotificationId;
        private TaskInstance<EmailState> verifyTaskInstance;

        @BeforeEach
        void setUp() {
            verifyEmailTask = emailTaskConfiguration.verifyEmailTask();
            emailStateWithNotificationId = testEmailState.toBuilder()
                .notificationId(testNotificationId)
                .build();
            verifyTaskInstance = new TaskInstance<>(
                "verify-email-task", "instance-1", emailStateWithNotificationId);
        }

        @Test
        @DisplayName("Should successfully verify delivered email and remove task")
        void shouldSuccessfullyVerifyDeliveredEmail() throws Exception {
            // Given
            Mockito.when(notification.getStatus()).thenReturn(NotificationStatus.DELIVERED.toString());
            Mockito.when(notificationService.fetchNotificationStatus(testNotificationId))
                .thenReturn(notification);

            // When
            CompletionHandler<EmailState> result = verifyEmailTask.execute(verifyTaskInstance, executionContext);

            // Then
            assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
            Mockito.verify(notificationService).fetchNotificationStatus(testNotificationId);
        }

        @Test
        @DisplayName("Should handle failed email status and remove task")
        void shouldHandleFailedEmailStatus() throws Exception {
            Mockito.when(notification.getStatus()).thenReturn("failed");
            Mockito.when(notificationService.fetchNotificationStatus(testNotificationId))
                .thenReturn(notification);

            CompletionHandler<EmailState> result = verifyEmailTask.execute(verifyTaskInstance, executionContext);

            assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
            Mockito.verify(notificationService).fetchNotificationStatus(testNotificationId);
        }

        @Test
        @DisplayName("Should handle NotificationClientException and remove task")
        void shouldHandleNotificationClientException() throws Exception {
            NotificationClientException exception = new NotificationClientException("API error");
            Mockito.when(notificationService.fetchNotificationStatus(testNotificationId))
                .thenThrow(exception);

            CompletionHandler<EmailState> result = verifyEmailTask.execute(verifyTaskInstance, executionContext);

            assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
            Mockito.verify(notificationService).fetchNotificationStatus(testNotificationId);
        }

        @Test
        @DisplayName("Should handle InterruptedException and throw RuntimeException")
        void shouldHandleInterruptedException() throws Exception {
            InterruptedException exception = new InterruptedException("Thread interrupted");
            Mockito.when(notificationService.fetchNotificationStatus(testNotificationId))
                .thenThrow(exception);

            assertThatThrownBy(() -> verifyEmailTask.execute(verifyTaskInstance, executionContext))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Task interrupted")
                .hasCause(exception);

            Mockito.verify(notificationService).fetchNotificationStatus(testNotificationId);
        }

        @Test
        @DisplayName("Should verify with correct notification ID")
        void shouldVerifyWithCorrectNotificationId() throws Exception {
            Mockito.when(notification.getStatus()).thenReturn(NotificationStatus.DELIVERED.toString());
            Mockito.when(notificationService.fetchNotificationStatus(testNotificationId))
                .thenReturn(notification);

            verifyEmailTask.execute(verifyTaskInstance, executionContext);

            Mockito.verify(notificationService).fetchNotificationStatus(testNotificationId);
            Mockito.verifyNoMoreInteractions(notificationService);
        }
    }

    @Nested
    @DisplayName("Task Configuration Tests")
    class TaskConfigurationTests {

        @Test
        @DisplayName("Should have correct task descriptors")
        void shouldHaveCorrectTaskDescriptors() {
            assertThat(EmailTaskConfiguration.sendEmailTask.getTaskName()).isEqualTo("send-email-task");
            assertThat(EmailTaskConfiguration.sendEmailTask.getDataClass()).isEqualTo(EmailState.class);

            assertThat(EmailTaskConfiguration.verifyEmailTask.getTaskName()).isEqualTo("verify-email-task");
            assertThat(EmailTaskConfiguration.verifyEmailTask.getDataClass()).isEqualTo(EmailState.class);
        }

        @Test
        @DisplayName("Should create send email task bean")
        void shouldCreateSendEmailTaskBean() {
            CustomTask<EmailState> task = emailTaskConfiguration.sendEmailTask();

            assertThat(task).isNotNull();
        }

        @Test
        @DisplayName("Should create verify email task bean")
        void shouldCreateVerifyEmailTaskBean() {
            CustomTask<EmailState> task = emailTaskConfiguration.verifyEmailTask();

            assertThat(task).isNotNull();
        }
    }
}
