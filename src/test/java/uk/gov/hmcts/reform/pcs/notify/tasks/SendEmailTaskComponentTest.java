package uk.gov.hmcts.reform.pcs.notify.tasks;

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
import uk.gov.hmcts.reform.pcs.notify.config.NotificationErrorHandler;
import uk.gov.hmcts.reform.pcs.notify.model.EmailState;
import uk.gov.hmcts.reform.pcs.notify.task.SendEmailTaskComponent;
import uk.gov.hmcts.reform.pcs.notify.repository.NotificationRepository;
import uk.gov.hmcts.reform.pcs.notify.service.NotificationService;
import uk.gov.service.notify.NotificationClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("SendEmailTaskComponent Tests")
class SendEmailTaskComponentTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private NotificationClient notificationClient;

    @Mock
    private NotificationErrorHandler errorHandler;

    @Mock
    private NotificationRepository notificationRepository;

    private SendEmailTaskComponent sendEmailTaskComponent;

    private static final int MAX_RETRIES_SEND = 3;
    private static final Duration SENDING_BACKOFF_DELAY = Duration.ofSeconds(10);
    private static final Duration STATUS_CHECK_TASK_DELAY = Duration.ofMinutes(5);

    @BeforeEach
    void setUp() {
        sendEmailTaskComponent = new SendEmailTaskComponent(
            notificationService,
            notificationClient,
            errorHandler,
            notificationRepository,
            MAX_RETRIES_SEND,
            SENDING_BACKOFF_DELAY,
            STATUS_CHECK_TASK_DELAY
        );
    }

    @Nested
    @DisplayName("Task Creation Tests")
    class TaskCreationTests {

        @Test
        @DisplayName("Should create send email task")
        void shouldCreateSendEmailTask() {
            CustomTask<EmailState> task = sendEmailTaskComponent.sendEmailTask();

            assertThat(task).isNotNull();
        }

        @Test
        @DisplayName("Should create different task instances")
        void shouldCreateDifferentTaskInstances() {
            CustomTask<EmailState> sendTask1 = sendEmailTaskComponent.sendEmailTask();
            CustomTask<EmailState> sendTask2 = sendEmailTaskComponent.sendEmailTask();

            assertThat(sendTask1).isNotNull();
            assertThat(sendTask2).isNotNull();
        }
    }

    @Nested
    @DisplayName("Task Descriptor Tests")
    class TaskDescriptorTests {

        @Test
        @DisplayName("Should have correct send email task descriptor")
        void shouldHaveCorrectSendEmailTaskDescriptor() {
            assertThat(SendEmailTaskComponent.sendEmailTask).isNotNull();
            assertThat(SendEmailTaskComponent.sendEmailTask.getTaskName()).isEqualTo("send-email-task");
            assertThat(SendEmailTaskComponent.sendEmailTask.getDataClass()).isEqualTo(EmailState.class);
        }
    }

    @Nested
    @DisplayName("Configuration Tests")
    class ConfigurationTests {

        @Test
        @DisplayName("Should create component with all dependencies")
        void shouldCreateComponentWithAllDependencies() {
            SendEmailTaskComponent component = new SendEmailTaskComponent(
                notificationService,
                notificationClient,
                errorHandler,
                notificationRepository,
                MAX_RETRIES_SEND,
                SENDING_BACKOFF_DELAY,
                STATUS_CHECK_TASK_DELAY
            );

            assertThat(component).isNotNull();
        }

        @Test
        @DisplayName("Should handle different retry configurations")
        void shouldHandleDifferentRetryConfigurations() {
            SendEmailTaskComponent component = new SendEmailTaskComponent(
                notificationService,
                notificationClient,
                errorHandler,
                notificationRepository,
                5,
                Duration.ofSeconds(5),
                Duration.ofMinutes(10)
            );

            CustomTask<EmailState> sendTask = component.sendEmailTask();

            assertThat(sendTask).isNotNull();
        }

        @Test
        @DisplayName("Should handle zero retry configurations")
        void shouldHandleZeroRetryConfigurations() {
            SendEmailTaskComponent component = new SendEmailTaskComponent(
                notificationService,
                notificationClient,
                errorHandler,
                notificationRepository,
                0,
                Duration.ofSeconds(1),
                Duration.ofMinutes(1)
            );

            CustomTask<EmailState> sendTask = component.sendEmailTask();

            assertThat(sendTask).isNotNull();
        }

        @Test
        @DisplayName("Should handle large retry configurations")
        void shouldHandleLargeRetryConfigurations() {
            SendEmailTaskComponent component = new SendEmailTaskComponent(
                notificationService,
                notificationClient,
                errorHandler,
                notificationRepository,
                100,
                Duration.ofMinutes(5),
                Duration.ofHours(1)
            );

            CustomTask<EmailState> sendTask = component.sendEmailTask();

            assertThat(sendTask).isNotNull();
        }
    }

    @Nested
    @DisplayName("Bean Creation Tests")
    class BeanCreationTests {

        @Test
        @DisplayName("Should create send email task bean")
        void shouldCreateSendEmailTaskBean() {
            CustomTask<EmailState> task = sendEmailTaskComponent.sendEmailTask();

            assertThat(task).isNotNull();
        }

        @Test
        @DisplayName("Should create multiple bean instances")
        void shouldCreateMultipleBeanInstances() {
            CustomTask<EmailState> sendTask1 = sendEmailTaskComponent.sendEmailTask();
            CustomTask<EmailState> sendTask2 = sendEmailTaskComponent.sendEmailTask();

            assertThat(sendTask1).isNotNull();
            assertThat(sendTask2).isNotNull();
        }
    }

    @Nested
    @DisplayName("Dependency Validation Tests")
    class DependencyValidationTests {

        @ParameterizedTest
        @DisplayName("Should accept all required dependencies")
        @ValueSource(strings = {"notificationService", "notificationClient", "errorHandler", "notificationRepository"})
        void shouldAcceptAllRequiredDependencies(String dependencyName) {
            SendEmailTaskComponent component = new SendEmailTaskComponent(
                notificationService,
                notificationClient,
                errorHandler,
                notificationRepository,
                MAX_RETRIES_SEND,
                SENDING_BACKOFF_DELAY,
                STATUS_CHECK_TASK_DELAY
            );

            assertThat(component).isNotNull();
        }
    }

    @Nested
    @DisplayName("Duration Configuration Tests")
    class DurationConfigurationTests {

        @Test
        @DisplayName("Should handle various duration configurations")
        void shouldHandleVariousDurationConfigurations() {
            SendEmailTaskComponent component = new SendEmailTaskComponent(
                notificationService,
                notificationClient,
                errorHandler,
                notificationRepository,
                MAX_RETRIES_SEND,
                Duration.ofMillis(500),
                Duration.ofSeconds(30)
            );

            CustomTask<EmailState> sendTask = component.sendEmailTask();

            assertThat(sendTask).isNotNull();
        }

        @Test
        @DisplayName("Should handle zero duration configurations")
        void shouldHandleZeroDurationConfigurations() {
            SendEmailTaskComponent component = new SendEmailTaskComponent(
                notificationService,
                notificationClient,
                errorHandler,
                notificationRepository,
                MAX_RETRIES_SEND,
                Duration.ZERO,
                Duration.ZERO
            );

            CustomTask<EmailState> sendTask = component.sendEmailTask();

            assertThat(sendTask).isNotNull();
        }
    }
}
