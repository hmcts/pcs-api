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
import uk.gov.hmcts.reform.pcs.notify.model.EmailState;
import uk.gov.hmcts.reform.pcs.notify.exception.PermanentNotificationException;
import uk.gov.hmcts.reform.pcs.notify.exception.TemporaryNotificationException;
import uk.gov.hmcts.reform.pcs.notify.model.EmailNotificationRequest;
import uk.gov.hmcts.reform.pcs.notify.model.NotificationStatus;
import uk.gov.hmcts.reform.pcs.notify.service.NotificationService;
import uk.gov.service.notify.Notification;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailTaskConfigurationTest {

    @Mock
    private NotificationService notificationService;

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
        int maxRetriesSendEmail = 5;
        int maxRetriesCheckEmail = 5;
        Duration sendingBackoffDelay = Duration.ofSeconds(10);
        Duration statusCheckDelay = Duration.ofSeconds(30);
        Duration verifyingBackoffDelay = Duration.ofSeconds(10);
        emailTaskConfiguration = new EmailTaskConfiguration(
            notificationService,
            maxRetriesSendEmail,
            maxRetriesCheckEmail,
            sendingBackoffDelay,
            statusCheckDelay,
            verifyingBackoffDelay
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
            when(sendEmailResponse.getNotificationId()).thenReturn(testUuid);
            when(notificationService.sendEmail(any(EmailNotificationRequest.class)))
                .thenReturn(sendEmailResponse);

            CompletionHandler<EmailState> result = sendEmailTask.execute(taskInstance, executionContext);

            assertThat(result).isInstanceOf(CompletionHandler.OnCompleteReplace.class);

            verify(notificationService).sendEmail(emailRequestCaptor.capture());
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
            when(notificationService.sendEmail(any(EmailNotificationRequest.class)))
                .thenThrow(exception);

            CompletionHandler<EmailState> result = sendEmailTask.execute(taskInstance, executionContext);

            assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
            verify(notificationService).sendEmail(any(EmailNotificationRequest.class));
        }

        @Test
        @DisplayName("Should rethrow temporary notification exception for retry")
        void shouldRethrowTemporaryNotificationException() {
            TemporaryNotificationException exception = new TemporaryNotificationException(
                "Temporary error", new RuntimeException("Underlying cause"));
            when(notificationService.sendEmail(any(EmailNotificationRequest.class)))
                .thenThrow(exception);

            assertThatThrownBy(() -> sendEmailTask.execute(taskInstance, executionContext))
                .isInstanceOf(TemporaryNotificationException.class)
                .hasMessage("Temporary error");

            verify(notificationService).sendEmail(any(EmailNotificationRequest.class));
        }

        @Test
        @DisplayName("Should build email request with all required fields")
        void shouldBuildEmailRequestWithAllFields() {
            when(sendEmailResponse.getNotificationId()).thenReturn(testUuid);
            when(notificationService.sendEmail(any(EmailNotificationRequest.class)))
                .thenReturn(sendEmailResponse);

            sendEmailTask.execute(taskInstance, executionContext);

            verify(notificationService).sendEmail(emailRequestCaptor.capture());
            EmailNotificationRequest request = emailRequestCaptor.getValue();

            assertThat(request.getEmailAddress()).isEqualTo("test@example.com");
            assertThat(request.getTemplateId()).isEqualTo("template-123");
            assertThat(request.getReference()).isEqualTo("ref-123");
            assertThat(request.getEmailReplyToId()).isEqualTo("reply-to-123");
            assertThat(request.getPersonalisation()).isEqualTo(testEmailState.getPersonalisation());
        }

        @Test
        @DisplayName("Should handle null response from notification service")
        void shouldHandleNullResponseFromNotificationService() {
            when(notificationService.sendEmail(any(EmailNotificationRequest.class)))
                .thenReturn(null);

            CompletionHandler<EmailState> result = sendEmailTask.execute(taskInstance, executionContext);

            assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
            verify(notificationService).sendEmail(any(EmailNotificationRequest.class));
        }

        @Test
        @DisplayName("Should handle null notification ID from response")
        void shouldHandleNullNotificationIdFromResponse() {
            when(sendEmailResponse.getNotificationId()).thenReturn(null);
            when(notificationService.sendEmail(any(EmailNotificationRequest.class)))
                .thenReturn(sendEmailResponse);

            CompletionHandler<EmailState> result = sendEmailTask.execute(taskInstance, executionContext);

            assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
            verify(notificationService).sendEmail(any(EmailNotificationRequest.class));
        }

        @Test
        @DisplayName("Should handle email state with null personalisation")
        void shouldHandleEmailStateWithNullPersonalisation() {
            EmailState emailStateWithNullPersonalisation = testEmailState.toBuilder()
                .personalisation(null)
                .build();
            TaskInstance<EmailState> taskInstanceWithNullPersonalisation =
                new TaskInstance<>("send-email-task", "instance-1", emailStateWithNullPersonalisation);

            when(sendEmailResponse.getNotificationId()).thenReturn(testUuid);
            when(notificationService.sendEmail(any(EmailNotificationRequest.class)))
                .thenReturn(sendEmailResponse);

            CompletionHandler<EmailState> result = sendEmailTask.execute(taskInstanceWithNullPersonalisation,
                                                                            executionContext);

            assertThat(result).isInstanceOf(CompletionHandler.OnCompleteReplace.class);
            verify(notificationService).sendEmail(emailRequestCaptor.capture());
            EmailNotificationRequest capturedRequest = emailRequestCaptor.getValue();
            assertThat(capturedRequest.getPersonalisation()).isNull();
        }

        @Test
        @DisplayName("Should handle email state with empty personalisation")
        void shouldHandleEmailStateWithEmptyPersonalisation() {
            EmailState emailStateWithEmptyPersonalisation = testEmailState.toBuilder()
                .personalisation(Map.of())
                .build();
            TaskInstance<EmailState> taskInstanceWithEmptyPersonalisation =
                new TaskInstance<>("send-email-task", "instance-1", emailStateWithEmptyPersonalisation);

            when(sendEmailResponse.getNotificationId()).thenReturn(testUuid);
            when(notificationService.sendEmail(any(EmailNotificationRequest.class)))
                .thenReturn(sendEmailResponse);

            CompletionHandler<EmailState> result = sendEmailTask.execute(taskInstanceWithEmptyPersonalisation,
                                                                            executionContext);

            assertThat(result).isInstanceOf(CompletionHandler.OnCompleteReplace.class);
            verify(notificationService).sendEmail(emailRequestCaptor.capture());
            EmailNotificationRequest capturedRequest = emailRequestCaptor.getValue();
            assertThat(capturedRequest.getPersonalisation()).isEmpty();
        }

        @Test
        @DisplayName("Should handle email state with null optional fields")
        void shouldHandleEmailStateWithNullOptionalFields() {
            EmailState emailStateWithNulls = EmailState.builder()
                .id("test-email-1")
                .emailAddress("test@example.com")
                .templateId("template-123")
                .personalisation(Map.of("name", "John Doe"))
                .reference(null)
                .emailReplyToId(null)
                .build();
            TaskInstance<EmailState> taskInstanceWithNulls =
                new TaskInstance<>("send-email-task", "instance-1", emailStateWithNulls);

            when(sendEmailResponse.getNotificationId()).thenReturn(testUuid);
            when(notificationService.sendEmail(any(EmailNotificationRequest.class)))
                .thenReturn(sendEmailResponse);

            CompletionHandler<EmailState> result = sendEmailTask.execute(taskInstanceWithNulls, executionContext);

            assertThat(result).isInstanceOf(CompletionHandler.OnCompleteReplace.class);
            verify(notificationService).sendEmail(emailRequestCaptor.capture());
            EmailNotificationRequest capturedRequest = emailRequestCaptor.getValue();
            assertThat(capturedRequest.getReference()).isNull();
            assertThat(capturedRequest.getEmailReplyToId()).isNull();
        }
    }

    @Nested
    @DisplayName("VerifyEmailTask Tests")
    class VerifyEmailTaskTests {

        private CustomTask<EmailState> verifyEmailTask;
        private TaskInstance<EmailState> verifyTaskInstance;

        @BeforeEach
        void setUp() {
            verifyEmailTask = emailTaskConfiguration.verifyEmailTask();
            EmailState emailStateWithNotificationId = testEmailState.toBuilder()
                .notificationId(testNotificationId)
                .build();
            verifyTaskInstance = new TaskInstance<>(
                "verify-email-task", "instance-1", emailStateWithNotificationId);
        }

        @Test
        @DisplayName("Should successfully verify delivered email and remove task")
        void shouldSuccessfullyVerifyDeliveredEmail() throws Exception {
            when(notification.getStatus()).thenReturn(NotificationStatus.DELIVERED.toString());

            when(notificationService.fetchNotificationStatus(testNotificationId))
                .thenReturn(notification);

            CompletionHandler<EmailState> result = verifyEmailTask.execute(verifyTaskInstance, executionContext);

            assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
            verify(notificationService).fetchNotificationStatus(testNotificationId);
        }

        @Test
        @DisplayName("Should handle failed email status and remove task")
        void shouldHandleFailedEmailStatus() throws Exception {
            when(notification.getStatus()).thenReturn("failed");
            when(notificationService.fetchNotificationStatus(testNotificationId))
                .thenReturn(notification);

            CompletionHandler<EmailState> result = verifyEmailTask.execute(verifyTaskInstance, executionContext);

            assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
            verify(notificationService).fetchNotificationStatus(testNotificationId);
        }

        @Test
        @DisplayName("Should handle NotificationClientException and remove task")
        void shouldHandleNotificationClientException() throws Exception {
            NotificationClientException exception = new NotificationClientException("API error");
            when(notificationService.fetchNotificationStatus(testNotificationId))
                .thenThrow(exception);

            CompletionHandler<EmailState> result = verifyEmailTask.execute(verifyTaskInstance, executionContext);

            assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
            verify(notificationService).fetchNotificationStatus(testNotificationId);
        }

        @Test
        @DisplayName("Should handle InterruptedException and throw RuntimeException")
        void shouldHandleInterruptedException() throws Exception {
            InterruptedException exception = new InterruptedException("Thread interrupted");
            when(notificationService.fetchNotificationStatus(testNotificationId))
                .thenThrow(exception);

            assertThatThrownBy(() -> verifyEmailTask.execute(verifyTaskInstance, executionContext))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Task interrupted")
                .hasCause(exception);

            verify(notificationService).fetchNotificationStatus(testNotificationId);
        }

        @Test
        @DisplayName("Should verify with correct notification ID")
        void shouldVerifyWithCorrectNotificationId() throws Exception {
            when(notification.getStatus()).thenReturn(NotificationStatus.DELIVERED.toString());
            when(notificationService.fetchNotificationStatus(testNotificationId))
                .thenReturn(notification);

            verifyEmailTask.execute(verifyTaskInstance, executionContext);

            verify(notificationService).fetchNotificationStatus(testNotificationId);
            Mockito.verifyNoMoreInteractions(notificationService);
        }

        @Test
        @DisplayName("Should handle different notification statuses")
        void shouldHandleDifferentNotificationStatuses() throws Exception {
            String[] statuses = {"sending", "pending", "failed", "technical-failure", "temporary-failure"};

            for (String status : statuses) {
                when(notification.getStatus()).thenReturn(status);
                when(notificationService.fetchNotificationStatus(testNotificationId))
                    .thenReturn(notification);

                CompletionHandler<EmailState> result = verifyEmailTask.execute(verifyTaskInstance, executionContext);

                assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
            }

            verify(notificationService, Mockito.times(statuses.length)).fetchNotificationStatus(testNotificationId);
        }

        @Test
        @DisplayName("Should handle email state without notification ID")
        void shouldHandleEmailStateWithoutNotificationId() throws Exception {
            EmailState emailStateWithoutNotificationId = testEmailState.toBuilder()
                .notificationId(null)
                .build();
            TaskInstance<EmailState> taskInstanceWithoutNotificationId =
                new TaskInstance<>("verify-email-task", "instance-1", emailStateWithoutNotificationId);

            when(notificationService.fetchNotificationStatus(null))
                .thenThrow(new NotificationClientException("Invalid notification ID"));

            CompletionHandler<EmailState> result = verifyEmailTask.execute(taskInstanceWithoutNotificationId,
                                                                            executionContext);

            assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
            verify(notificationService).fetchNotificationStatus(null);
        }

        @Test
        @DisplayName("Should handle case-insensitive delivered status")
        void shouldHandleCaseInsensitiveDeliveredStatus() throws Exception {
            String[] deliveredStatuses = {"DELIVERED", "delivered", "Delivered", "DELIVERED "};

            for (String status : deliveredStatuses) {
                when(notification.getStatus()).thenReturn(status);
                when(notificationService.fetchNotificationStatus(testNotificationId))
                    .thenReturn(notification);

                CompletionHandler<EmailState> result = verifyEmailTask.execute(verifyTaskInstance, executionContext);

                assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
            }

            verify(notificationService, Mockito.times(deliveredStatuses.length))
                .fetchNotificationStatus(testNotificationId);
        }

        @Test
        @DisplayName("Should maintain thread interruption flag when handling InterruptedException")
        void shouldMaintainThreadInterruptionFlag() throws Exception {
            InterruptedException exception = new InterruptedException("Thread interrupted");
            when(notificationService.fetchNotificationStatus(testNotificationId))
                .thenThrow(exception);

            assertThatThrownBy(() -> verifyEmailTask.execute(verifyTaskInstance, executionContext))
                .isInstanceOf(RuntimeException.class);

            assertThat(Thread.currentThread().isInterrupted()).isTrue();
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

        @Test
        @DisplayName("Should verify task names match between descriptors")
        void shouldVerifyTaskNamesMatchBetweenDescriptors() {
            assertThat(EmailTaskConfiguration.sendEmailTask.getTaskName())
                .isEqualTo("send-email-task");
            assertThat(EmailTaskConfiguration.verifyEmailTask.getTaskName())
                .isEqualTo("verify-email-task");
        }
    }
}
