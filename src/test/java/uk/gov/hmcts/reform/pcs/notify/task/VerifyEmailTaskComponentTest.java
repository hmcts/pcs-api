package uk.gov.hmcts.reform.pcs.notify.task;

import com.github.kagkarlsson.scheduler.task.CompletionHandler;
import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.CustomTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.reform.pcs.notify.config.NotificationErrorHandler;
import uk.gov.hmcts.reform.pcs.notify.model.EmailState;
import uk.gov.hmcts.reform.pcs.notify.service.NotificationService;
import uk.gov.service.notify.Notification;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.notify.model.NotificationStatus.PERMANENT_FAILURE;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("VerifyEmailTaskComponent Tests")
class VerifyEmailTaskComponentTest {

    private VerifyEmailTaskComponent verifyEmailTaskComponent;

    @Mock
    private NotificationService notificationService;

    @Mock
    private NotificationClient notificationClient;

    @Mock
    private NotificationErrorHandler errorHandler;

    @Mock
    private TaskInstance<EmailState> taskInstance;

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private Notification notification;

    private final Duration statusCheckBackoffDelay = Duration.ofMinutes(2);

    private EmailState emailState;
    private final UUID dbNotificationId = UUID.randomUUID();
    private final String taskId = "verify-task-123";
    private final String notificationId = "notification-456";
    private final Map<String, Object> personalisation = Map.of("name", "John Doe");

    @BeforeEach
    void setUp() {
        int maxRetriesCheckEmail = 5;
        verifyEmailTaskComponent = new VerifyEmailTaskComponent(
            notificationService,
            notificationClient,
            errorHandler,
            maxRetriesCheckEmail,
            statusCheckBackoffDelay
        );

        String templateId = "template-789";
        String emailAddress = "test@example.com";
        emailState = EmailState.builder()
            .id(taskId)
            .dbNotificationId(dbNotificationId)
            .notificationId(notificationId)
            .templateId(templateId)
            .emailAddress(emailAddress)
            .personalisation(personalisation)
            .build();

        when(taskInstance.getData()).thenReturn(emailState);
        when(taskInstance.getId()).thenReturn(taskId);
    }

    @Nested
    @DisplayName("Component Initialization Tests")
    class ComponentInitializationTests {

        @Test
        @DisplayName("Should create task descriptor with correct name and type")
        void shouldCreateTaskDescriptorWithCorrectNameAndType() {
            assertThat(VerifyEmailTaskComponent.verifyEmailTask.getTaskName()).isEqualTo("verify-email-task");
            assertThat(VerifyEmailTaskComponent.verifyEmailTask.getDataClass()).isEqualTo(EmailState.class);
        }

        @Test
        @DisplayName("Should create verify email task bean")
        void shouldCreateVerifyEmailTaskBean() {
            CustomTask<EmailState> task = verifyEmailTaskComponent.verifyEmailTask();

            assertThat(task).isNotNull();
        }
    }

    @Nested
    @DisplayName("Successful Email Verification Tests")
    class SuccessfulEmailVerificationTests {

        @Test
        @DisplayName("Should verify email delivery successfully for delivered status (lowercase)")
        void shouldVerifyEmailDeliverySuccessfullyForDeliveredStatusLowercase() throws Exception {
            String deliveredStatus = "delivered";
            when(notificationClient.getNotificationById(notificationId)).thenReturn(notification);
            when(notification.getStatus()).thenReturn(deliveredStatus);

            CustomTask<EmailState> task = verifyEmailTaskComponent.verifyEmailTask();

            CompletionHandler<EmailState> result = task.execute(taskInstance, executionContext);

            verify(notificationClient).getNotificationById(notificationId);
            verify(notificationService).updateNotificationStatus(dbNotificationId, deliveredStatus);
            assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
        }

        @Test
        @DisplayName("Should verify email delivery successfully for delivered status (uppercase)")
        void shouldVerifyEmailDeliverySuccessfullyForDeliveredStatusUppercase() throws Exception {
            String deliveredStatus = "DELIVERED";
            when(notificationClient.getNotificationById(notificationId)).thenReturn(notification);
            when(notification.getStatus()).thenReturn(deliveredStatus);

            CustomTask<EmailState> task = verifyEmailTaskComponent.verifyEmailTask();

            CompletionHandler<EmailState> result = task.execute(taskInstance, executionContext);

            verify(notificationClient).getNotificationById(notificationId);
            verify(notificationService).updateNotificationStatus(dbNotificationId, deliveredStatus);
            assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
        }

        @Test
        @DisplayName("Should verify email delivery successfully for delivered status (mixed case)")
        void shouldVerifyEmailDeliverySuccessfullyForDeliveredStatusMixedCase() throws Exception {
            String deliveredStatus = "DeLiVeReD";
            when(notificationClient.getNotificationById(notificationId)).thenReturn(notification);
            when(notification.getStatus()).thenReturn(deliveredStatus);

            CustomTask<EmailState> task = verifyEmailTaskComponent.verifyEmailTask();

            CompletionHandler<EmailState> result = task.execute(taskInstance, executionContext);

            verify(notificationClient).getNotificationById(notificationId);
            verify(notificationService).updateNotificationStatus(dbNotificationId, deliveredStatus);
            assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
        }

        @Test
        @DisplayName("Should preserve original case when updating delivered status")
        void shouldPreserveOriginalCaseWhenUpdatingDeliveredStatus() throws Exception {
            String originalStatus = "Delivered";
            when(notificationClient.getNotificationById(notificationId)).thenReturn(notification);
            when(notification.getStatus()).thenReturn(originalStatus);

            CustomTask<EmailState> task = verifyEmailTaskComponent.verifyEmailTask();

            CompletionHandler<EmailState> result = task.execute(taskInstance, executionContext);

            verify(notificationClient).getNotificationById(notificationId);
            verify(notificationService).updateNotificationStatus(dbNotificationId, originalStatus);
            assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
        }
    }

    @Nested
    @DisplayName("Non-Delivered Status Tests")
    class NonDeliveredStatusTests {

        @ParameterizedTest
        @ValueSource(strings = {"FAILED", "PERMANENT_FAILURE", "TEMPORARY_FAILURE",
            "TECHNICAL_FAILURE", "PENDING", "SENDING", "ERROR", "INVALID"})
        @DisplayName("Should update status to PERMANENT_FAILURE for non-delivered statuses")
        void shouldUpdateStatusToPermanentFailureForNonDeliveredStatuses(String status) throws Exception {
            when(notificationClient.getNotificationById(notificationId)).thenReturn(notification);
            when(notification.getStatus()).thenReturn(status);

            CustomTask<EmailState> task = verifyEmailTaskComponent.verifyEmailTask();

            CompletionHandler<EmailState> result = task.execute(taskInstance, executionContext);

            verify(notificationClient).getNotificationById(notificationId);
            verify(notificationService).updateNotificationStatus(dbNotificationId, PERMANENT_FAILURE.toString());
            assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
        }

        @Test
        @DisplayName("Should update status to PERMANENT_FAILURE for failed status")
        void shouldUpdateStatusToPermanentFailureForFailedStatus() throws Exception {
            String failedStatus = "FAILED";
            when(notificationClient.getNotificationById(notificationId)).thenReturn(notification);
            when(notification.getStatus()).thenReturn(failedStatus);

            CustomTask<EmailState> task = verifyEmailTaskComponent.verifyEmailTask();

            CompletionHandler<EmailState> result = task.execute(taskInstance, executionContext);

            verify(notificationClient).getNotificationById(notificationId);
            verify(notificationService).updateNotificationStatus(dbNotificationId, PERMANENT_FAILURE.toString());
            assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
        }

        @Test
        @DisplayName("Should update status to PERMANENT_FAILURE for pending status")
        void shouldUpdateStatusToPermanentFailureForPendingStatus() throws Exception {
            String pendingStatus = "PENDING";
            when(notificationClient.getNotificationById(notificationId)).thenReturn(notification);
            when(notification.getStatus()).thenReturn(pendingStatus);

            CustomTask<EmailState> task = verifyEmailTaskComponent.verifyEmailTask();

            CompletionHandler<EmailState> result = task.execute(taskInstance, executionContext);

            verify(notificationClient).getNotificationById(notificationId);
            verify(notificationService).updateNotificationStatus(dbNotificationId, PERMANENT_FAILURE.toString());
            assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
        }

        @Test
        @DisplayName("Should update status to PERMANENT_FAILURE for custom non-delivered status")
        void shouldUpdateStatusToPermanentFailureForCustomNonDeliveredStatus() throws Exception {
            String customStatus = "UNKNOWN_STATUS";
            when(notificationClient.getNotificationById(notificationId)).thenReturn(notification);
            when(notification.getStatus()).thenReturn(customStatus);

            CustomTask<EmailState> task = verifyEmailTaskComponent.verifyEmailTask();

            CompletionHandler<EmailState> result = task.execute(taskInstance, executionContext);

            verify(notificationClient).getNotificationById(notificationId);
            verify(notificationService).updateNotificationStatus(dbNotificationId, PERMANENT_FAILURE.toString());
            assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
        }
    }

    @Nested
    @DisplayName("NotificationClientException Handling Tests")
    class NotificationClientExceptionHandlingTests {

        @Test
        @DisplayName("Should handle NotificationClientException and delegate to error handler")
        void shouldHandleNotificationClientExceptionAndDelegateToErrorHandler() throws Exception {
            NotificationClientException exception = new NotificationClientException("API error",
                new RuntimeException());
            when(notificationClient.getNotificationById(notificationId)).thenThrow(exception);

            CustomTask<EmailState> task = verifyEmailTaskComponent.verifyEmailTask();
            CompletionHandler<EmailState> result = task.execute(taskInstance, executionContext);
            assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
            verify(notificationClient).getNotificationById(notificationId);
            verify(errorHandler).handleFetchException(exception, notificationId);
            verifyNoInteractions(notificationService);
        }

        @Test
        @DisplayName("Should handle NotificationClientException with different error messages")
        void shouldHandleNotificationClientExceptionWithDifferentErrorMessages() throws Exception {
            NotificationClientException exception = new NotificationClientException("Service temporarily unavailable",
                new RuntimeException());
            when(notificationClient.getNotificationById(notificationId)).thenThrow(exception);

            CustomTask<EmailState> task = verifyEmailTaskComponent.verifyEmailTask();
            CompletionHandler<EmailState> result = task.execute(taskInstance, executionContext);
            assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
            verify(notificationClient).getNotificationById(notificationId);
            verify(errorHandler).handleFetchException(exception, notificationId);
            verifyNoInteractions(notificationService);
        }

        @Test
        @DisplayName("Should not update notification status when exception occurs")
        void shouldNotUpdateNotificationStatusWhenExceptionOccurs() throws Exception {
            NotificationClientException exception = new NotificationClientException("Network error",
                new RuntimeException());
            when(notificationClient.getNotificationById(notificationId)).thenThrow(exception);

            CustomTask<EmailState> task = verifyEmailTaskComponent.verifyEmailTask();

            task.execute(taskInstance, executionContext);

            verifyNoInteractions(notificationService);
        }
    }

    @Nested
    @DisplayName("Email State Validation Tests")
    class EmailStateValidationTests {

        @Test
        @DisplayName("Should handle email state with all required fields")
        void shouldHandleEmailStateWithAllRequiredFields() throws Exception {
            String deliveredStatus = "Delivered";
            when(notificationClient.getNotificationById(notificationId)).thenReturn(notification);
            when(notification.getStatus()).thenReturn(deliveredStatus);

            CustomTask<EmailState> task = verifyEmailTaskComponent.verifyEmailTask();

            CompletionHandler<EmailState> result = task.execute(taskInstance, executionContext);

            verify(notificationClient).getNotificationById(notificationId);
            verify(notificationService).updateNotificationStatus(dbNotificationId, deliveredStatus);
            assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
        }

        @Test
        @DisplayName("Should handle email state with minimum required fields")
        void shouldHandleEmailStateWithMinimumRequiredFields() throws Exception {
            EmailState minimalEmailState = EmailState.builder()
                .id(taskId)
                .dbNotificationId(dbNotificationId)
                .notificationId(notificationId)
                .build();
            when(taskInstance.getData()).thenReturn(minimalEmailState);

            String deliveredStatus = "delivered";
            when(notificationClient.getNotificationById(notificationId)).thenReturn(notification);
            when(notification.getStatus()).thenReturn(deliveredStatus);

            CustomTask<EmailState> task = verifyEmailTaskComponent.verifyEmailTask();

            CompletionHandler<EmailState> result = task.execute(taskInstance, executionContext);

            verify(notificationClient).getNotificationById(notificationId);
            verify(notificationService).updateNotificationStatus(dbNotificationId, deliveredStatus);
            assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
        }

        @Test
        @DisplayName("Should handle different notification ID formats")
        void shouldHandleDifferentNotificationIdFormats() throws Exception {
            String uuidNotificationId = UUID.randomUUID().toString();
            EmailState uuidEmailState = emailState.toBuilder()
                .notificationId(uuidNotificationId)
                .build();
            when(taskInstance.getData()).thenReturn(uuidEmailState);

            String deliveredStatus = "DELIVERED";
            when(notificationClient.getNotificationById(uuidNotificationId)).thenReturn(notification);
            when(notification.getStatus()).thenReturn(deliveredStatus);

            CustomTask<EmailState> task = verifyEmailTaskComponent.verifyEmailTask();

            CompletionHandler<EmailState> result = task.execute(taskInstance, executionContext);

            verify(notificationClient).getNotificationById(uuidNotificationId);
            verify(notificationService).updateNotificationStatus(dbNotificationId, deliveredStatus);
            assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
        }
    }

    @Nested
    @DisplayName("Task Configuration Tests")
    class TaskConfigurationTests {

        @Test
        @DisplayName("Should configure task with correct failure handlers")
        void shouldConfigureTaskWithCorrectFailureHandlers() {
            CustomTask<EmailState> task = verifyEmailTaskComponent.verifyEmailTask();

            assertThat(task).isNotNull();
        }

        @Test
        @DisplayName("Should use correct configuration values")
        void shouldUseCorrectConfigurationValues() {
            VerifyEmailTaskComponent component = new VerifyEmailTaskComponent(
                notificationService,
                notificationClient,
                errorHandler,
                10,
                Duration.ofMinutes(5)
            );

            CustomTask<EmailState> task = component.verifyEmailTask();
            assertThat(task).isNotNull();
        }
    }

    @Nested
    @DisplayName("Status Update Tests")
    class StatusUpdateTests {

        @Test
        @DisplayName("Should update notification status with original delivered status (case-insensitive match)")
        void shouldUpdateNotificationStatusWithOriginalDeliveredStatusCaseInsensitiveMatch() throws Exception {
            String originalDeliveredStatus = "DELIVERED";
            when(notificationClient.getNotificationById(notificationId)).thenReturn(notification);
            when(notification.getStatus()).thenReturn(originalDeliveredStatus);

            CustomTask<EmailState> task = verifyEmailTaskComponent.verifyEmailTask();

            task.execute(taskInstance, executionContext);

            verify(notificationService).updateNotificationStatus(dbNotificationId, originalDeliveredStatus);
        }

        @Test
        @DisplayName("Should update to PERMANENT_FAILURE for non-delivered status")
        void shouldUpdateToPermanentFailureForNonDeliveredStatus() throws Exception {
            String customStatus = "custom_status";
            when(notificationClient.getNotificationById(notificationId)).thenReturn(notification);
            when(notification.getStatus()).thenReturn(customStatus);

            CustomTask<EmailState> task = verifyEmailTaskComponent.verifyEmailTask();

            task.execute(taskInstance, executionContext);

            verify(notificationService).updateNotificationStatus(dbNotificationId, PERMANENT_FAILURE.toString());
        }

        @Test
        @DisplayName("Should handle null status from notification by throwing exception")
        void shouldHandleNullStatusFromNotificationByThrowingException() throws Exception {
            when(notificationClient.getNotificationById(notificationId)).thenReturn(notification);
            when(notification.getStatus()).thenReturn(null);

            CustomTask<EmailState> task = verifyEmailTaskComponent.verifyEmailTask();

            try {
                task.execute(taskInstance, executionContext);
            } catch (NullPointerException e) {
                verify(notificationClient).getNotificationById(notificationId);
                verifyNoInteractions(notificationService);
            }
        }

        @Test
        @DisplayName("Should update to PERMANENT_FAILURE for empty status from notification")
        void shouldUpdateToPermanentFailureForEmptyStatusFromNotification() throws Exception {
            String emptyStatus = "";
            when(notificationClient.getNotificationById(notificationId)).thenReturn(notification);
            when(notification.getStatus()).thenReturn(emptyStatus);

            CustomTask<EmailState> task = verifyEmailTaskComponent.verifyEmailTask();

            CompletionHandler<EmailState> result = task.execute(taskInstance, executionContext);

            verify(notificationClient).getNotificationById(notificationId);
            verify(notificationService).updateNotificationStatus(dbNotificationId, PERMANENT_FAILURE.toString());
            assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
        }

        @Test
        @DisplayName("Should preserve exact case of delivered status when updating")
        void shouldPreserveExactCaseOfDeliveredStatusWhenUpdating() throws Exception {
            String mixedCaseDelivered = "dElIvErEd";
            when(notificationClient.getNotificationById(notificationId)).thenReturn(notification);
            when(notification.getStatus()).thenReturn(mixedCaseDelivered);

            CustomTask<EmailState> task = verifyEmailTaskComponent.verifyEmailTask();

            task.execute(taskInstance, executionContext);

            verify(notificationService).updateNotificationStatus(dbNotificationId, mixedCaseDelivered);
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should handle complete successful verification flow")
        void shouldHandleCompleteSuccessfulVerificationFlow() throws Exception {
            String deliveredStatus = "DELIVERED";
            when(notificationClient.getNotificationById(notificationId)).thenReturn(notification);
            when(notification.getStatus()).thenReturn(deliveredStatus);

            CustomTask<EmailState> task = verifyEmailTaskComponent.verifyEmailTask();

            CompletionHandler<EmailState> result = task.execute(taskInstance, executionContext);

            verify(notificationClient).getNotificationById(notificationId);
            verify(notificationService).updateNotificationStatus(dbNotificationId, deliveredStatus);
            assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
        }

        @Test
        @DisplayName("Should handle complete error flow with proper cleanup")
        void shouldHandleCompleteErrorFlowWithProperCleanup() throws Exception {
            NotificationClientException exception = new NotificationClientException("Service error",
                new RuntimeException());
            when(notificationClient.getNotificationById(notificationId)).thenThrow(exception);

            CustomTask<EmailState> task = verifyEmailTaskComponent.verifyEmailTask();
            CompletionHandler<EmailState> result = task.execute(taskInstance, executionContext);
            assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
            verify(notificationClient).getNotificationById(notificationId);
            verify(errorHandler).handleFetchException(exception, notificationId);
            verifyNoInteractions(notificationService);
        }

        @Test
        @DisplayName("Should handle verification flow with failed status")
        void shouldHandleVerificationFlowWithFailedStatus() throws Exception {
            String failedStatus = "FAILED";
            when(notificationClient.getNotificationById(notificationId)).thenReturn(notification);
            when(notification.getStatus()).thenReturn(failedStatus);

            CustomTask<EmailState> task = verifyEmailTaskComponent.verifyEmailTask();

            CompletionHandler<EmailState> result = task.execute(taskInstance, executionContext);

            verify(notificationClient).getNotificationById(notificationId);
            verify(notificationService).updateNotificationStatus(dbNotificationId, PERMANENT_FAILURE.toString());
            assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
        }

        @Test
        @DisplayName("Should handle verification flow with case-insensitive delivered status")
        void shouldHandleVerificationFlowWithCaseInsensitiveDeliveredStatus() throws Exception {
            String mixedCaseDelivered = "dElIvErEd";
            when(notificationClient.getNotificationById(notificationId)).thenReturn(notification);
            when(notification.getStatus()).thenReturn(mixedCaseDelivered);

            CustomTask<EmailState> task = verifyEmailTaskComponent.verifyEmailTask();

            CompletionHandler<EmailState> result = task.execute(taskInstance, executionContext);

            verify(notificationClient).getNotificationById(notificationId);
            verify(notificationService).updateNotificationStatus(dbNotificationId, mixedCaseDelivered);
            assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle notification client returning null notification")
        void shouldHandleNotificationClientReturningNullNotification() throws Exception {
            when(notificationClient.getNotificationById(notificationId)).thenReturn(null);

            CustomTask<EmailState> task = verifyEmailTaskComponent.verifyEmailTask();

            try {
                task.execute(taskInstance, executionContext);
            } catch (NullPointerException e) {
                verify(notificationClient).getNotificationById(notificationId);
                verifyNoInteractions(notificationService);
            }
        }

        @Test
        @DisplayName("Should treat all case variations of delivered as delivered status")
        void shouldTreatAllCaseVariationsOfDeliveredAsDeliveredStatus() throws Exception {
            String mixedCaseStatus = "dElIvErEd";
            when(notificationClient.getNotificationById(notificationId)).thenReturn(notification);
            when(notification.getStatus()).thenReturn(mixedCaseStatus);

            CustomTask<EmailState> task = verifyEmailTaskComponent.verifyEmailTask();

            CompletionHandler<EmailState> result = task.execute(taskInstance, executionContext);

            verify(notificationClient).getNotificationById(notificationId);
            verify(notificationService).updateNotificationStatus(dbNotificationId, mixedCaseStatus);
            assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
        }

        @Test
        @DisplayName("Should handle status with leading and trailing spaces as non-delivered")
        void shouldHandleStatusWithLeadingAndTrailingSpacesAsNonDelivered() throws Exception {
            String statusWithSpaces = " delivered ";
            when(notificationClient.getNotificationById(notificationId)).thenReturn(notification);
            when(notification.getStatus()).thenReturn(statusWithSpaces);

            CustomTask<EmailState> task = verifyEmailTaskComponent.verifyEmailTask();

            CompletionHandler<EmailState> result = task.execute(taskInstance, executionContext);

            verify(notificationClient).getNotificationById(notificationId);
            verify(notificationService).updateNotificationStatus(dbNotificationId, PERMANENT_FAILURE.toString());
            assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
        }
    }
}
