package uk.gov.hmcts.reform.pcs.notify.config;

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
import uk.gov.hmcts.reform.pcs.notify.model.EmailState;
import uk.gov.hmcts.reform.pcs.notify.repository.NotificationRepository;
import uk.gov.hmcts.reform.pcs.notify.service.NotificationService;
import uk.gov.service.notify.NotificationClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailTaskConfiguration Tests")
class EmailTaskConfigurationTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private NotificationClient notificationClient;

    @Mock
    private NotificationErrorHandler errorHandler;

    @Mock
    private NotificationRepository notificationRepository;

    private EmailTaskConfiguration emailTaskConfiguration;

    private static final int MAX_RETRIES_SEND = 3;
    private static final int MAX_RETRIES_CHECK = 2;
    private static final Duration SENDING_BACKOFF_DELAY = Duration.ofSeconds(10);
    private static final Duration STATUS_CHECK_TASK_DELAY = Duration.ofMinutes(5);
    private static final Duration STATUS_CHECK_BACKOFF_DELAY = Duration.ofSeconds(30);

    @BeforeEach
    void setUp() {
        emailTaskConfiguration = new EmailTaskConfiguration(
            notificationService,
            notificationClient,
            errorHandler,
            notificationRepository,
            MAX_RETRIES_SEND,
            MAX_RETRIES_CHECK,
            SENDING_BACKOFF_DELAY,
            STATUS_CHECK_TASK_DELAY,
            STATUS_CHECK_BACKOFF_DELAY
        );
    }

    @Nested
    @DisplayName("Task Creation Tests")
    class TaskCreationTests {

        @Test
        @DisplayName("Should create send email task")
        void shouldCreateSendEmailTask() {
            CustomTask<EmailState> task = emailTaskConfiguration.sendEmailTask();

            assertThat(task).isNotNull();
        }

        @Test
        @DisplayName("Should create verify email task")
        void shouldCreateVerifyEmailTask() {
            CustomTask<EmailState> task = emailTaskConfiguration.verifyEmailTask();

            assertThat(task).isNotNull();
        }

        @Test
        @DisplayName("Should create different task instances")
        void shouldCreateDifferentTaskInstances() {
            CustomTask<EmailState> sendTask1 = emailTaskConfiguration.sendEmailTask();
            CustomTask<EmailState> sendTask2 = emailTaskConfiguration.sendEmailTask();
            CustomTask<EmailState> verifyTask = emailTaskConfiguration.verifyEmailTask();

            assertThat(sendTask1).isNotNull();
            assertThat(sendTask2).isNotNull();
            assertThat(verifyTask).isNotNull();
            assertThat(sendTask1).isNotSameAs(verifyTask);
        }
    }

    @Nested
    @DisplayName("Task Descriptor Tests")
    class TaskDescriptorTests {

        @Test
        @DisplayName("Should have correct send email task descriptor")
        void shouldHaveCorrectSendEmailTaskDescriptor() {
            assertThat(EmailTaskConfiguration.sendEmailTask).isNotNull();
            assertThat(EmailTaskConfiguration.sendEmailTask.getTaskName()).isEqualTo("send-email-task");
            assertThat(EmailTaskConfiguration.sendEmailTask.getDataClass()).isEqualTo(EmailState.class);
        }

        @Test
        @DisplayName("Should have correct verify email task descriptor")
        void shouldHaveCorrectVerifyEmailTaskDescriptor() {
            assertThat(EmailTaskConfiguration.verifyEmailTask).isNotNull();
            assertThat(EmailTaskConfiguration.verifyEmailTask.getTaskName()).isEqualTo("verify-email-task");
            assertThat(EmailTaskConfiguration.verifyEmailTask.getDataClass()).isEqualTo(EmailState.class);
        }

        @Test
        @DisplayName("Should have different task descriptors for send and verify tasks")
        void shouldHaveDifferentTaskDescriptorsForSendAndVerifyTasks() {
            assertThat(EmailTaskConfiguration.sendEmailTask)
                .isNotEqualTo(EmailTaskConfiguration.verifyEmailTask);
            assertThat(EmailTaskConfiguration.sendEmailTask.getTaskName())
                .isNotEqualTo(EmailTaskConfiguration.verifyEmailTask.getTaskName());
        }

        @Test
        @DisplayName("Should have same data class for both task descriptors")
        void shouldHaveSameDataClassForBothTaskDescriptors() {
            assertThat(EmailTaskConfiguration.sendEmailTask.getDataClass())
                .isEqualTo(EmailTaskConfiguration.verifyEmailTask.getDataClass());
            assertThat(EmailTaskConfiguration.sendEmailTask.getDataClass())
                .isEqualTo(EmailState.class);
        }
    }

    @Nested
    @DisplayName("Configuration Tests")
    class ConfigurationTests {

        @Test
        @DisplayName("Should create configuration with all dependencies")
        void shouldCreateConfigurationWithAllDependencies() {
            EmailTaskConfiguration config = new EmailTaskConfiguration(
                notificationService,
                notificationClient,
                errorHandler,
                notificationRepository,
                MAX_RETRIES_SEND,
                MAX_RETRIES_CHECK,
                SENDING_BACKOFF_DELAY,
                STATUS_CHECK_TASK_DELAY,
                STATUS_CHECK_BACKOFF_DELAY
            );

            assertThat(config).isNotNull();
        }

        @Test
        @DisplayName("Should handle different retry configurations")
        void shouldHandleDifferentRetryConfigurations() {
            EmailTaskConfiguration config = new EmailTaskConfiguration(
                notificationService,
                notificationClient,
                errorHandler,
                notificationRepository,
                5,
                3,
                Duration.ofSeconds(5),
                Duration.ofMinutes(10),
                Duration.ofSeconds(15)
            );

            CustomTask<EmailState> sendTask = config.sendEmailTask();
            CustomTask<EmailState> verifyTask = config.verifyEmailTask();

            assertThat(sendTask).isNotNull();
            assertThat(verifyTask).isNotNull();
        }

        @Test
        @DisplayName("Should handle zero retry configurations")
        void shouldHandleZeroRetryConfigurations() {
            EmailTaskConfiguration config = new EmailTaskConfiguration(
                notificationService,
                notificationClient,
                errorHandler,
                notificationRepository,
                0,
                0,
                Duration.ofSeconds(1),
                Duration.ofMinutes(1),
                Duration.ofSeconds(1)
            );

            CustomTask<EmailState> sendTask = config.sendEmailTask();
            CustomTask<EmailState> verifyTask = config.verifyEmailTask();

            assertThat(sendTask).isNotNull();
            assertThat(verifyTask).isNotNull();
        }

        @Test
        @DisplayName("Should handle large retry configurations")
        void shouldHandleLargeRetryConfigurations() {
            EmailTaskConfiguration config = new EmailTaskConfiguration(
                notificationService,
                notificationClient,
                errorHandler,
                notificationRepository,
                100,
                50,
                Duration.ofMinutes(5),
                Duration.ofHours(1),
                Duration.ofMinutes(10)
            );

            CustomTask<EmailState> sendTask = config.sendEmailTask();
            CustomTask<EmailState> verifyTask = config.verifyEmailTask();

            assertThat(sendTask).isNotNull();
            assertThat(verifyTask).isNotNull();
        }
    }

    @Nested
    @DisplayName("Bean Creation Tests")
    class BeanCreationTests {

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
        @DisplayName("Should create multiple bean instances")
        void shouldCreateMultipleBeanInstances() {
            CustomTask<EmailState> sendTask1 = emailTaskConfiguration.sendEmailTask();
            CustomTask<EmailState> sendTask2 = emailTaskConfiguration.sendEmailTask();
            CustomTask<EmailState> verifyTask1 = emailTaskConfiguration.verifyEmailTask();
            CustomTask<EmailState> verifyTask2 = emailTaskConfiguration.verifyEmailTask();

            assertThat(sendTask1).isNotNull();
            assertThat(sendTask2).isNotNull();
            assertThat(verifyTask1).isNotNull();
            assertThat(verifyTask2).isNotNull();
        }
    }

    @Nested
    @DisplayName("Dependency Validation Tests")
    class DependencyValidationTests {

        @ParameterizedTest
        @DisplayName("Should accept all required dependencies")
        @ValueSource(strings = {"notificationService", "notificationClient", "errorHandler", "notificationRepository"})
        void shouldAcceptAllRequiredDependencies(String dependencyName) {
            EmailTaskConfiguration config = new EmailTaskConfiguration(
                notificationService,
                notificationClient,
                errorHandler,
                notificationRepository,
                MAX_RETRIES_SEND,
                MAX_RETRIES_CHECK,
                SENDING_BACKOFF_DELAY,
                STATUS_CHECK_TASK_DELAY,
                STATUS_CHECK_BACKOFF_DELAY
            );

            assertThat(config).isNotNull();
        }
    }

    @Nested
    @DisplayName("Duration Configuration Tests")
    class DurationConfigurationTests {

        @Test
        @DisplayName("Should handle various duration configurations")
        void shouldHandleVariousDurationConfigurations() {
            EmailTaskConfiguration config = new EmailTaskConfiguration(
                notificationService,
                notificationClient,
                errorHandler,
                notificationRepository,
                MAX_RETRIES_SEND,
                MAX_RETRIES_CHECK,
                Duration.ofMillis(500),
                Duration.ofSeconds(30),
                Duration.ofMillis(100)
            );

            CustomTask<EmailState> sendTask = config.sendEmailTask();
            CustomTask<EmailState> verifyTask = config.verifyEmailTask();

            assertThat(sendTask).isNotNull();
            assertThat(verifyTask).isNotNull();
        }

        @Test
        @DisplayName("Should handle zero duration configurations")
        void shouldHandleZeroDurationConfigurations() {
            EmailTaskConfiguration config = new EmailTaskConfiguration(
                notificationService,
                notificationClient,
                errorHandler,
                notificationRepository,
                MAX_RETRIES_SEND,
                MAX_RETRIES_CHECK,
                Duration.ZERO,
                Duration.ZERO,
                Duration.ZERO
            );

            CustomTask<EmailState> sendTask = config.sendEmailTask();
            CustomTask<EmailState> verifyTask = config.verifyEmailTask();

            assertThat(sendTask).isNotNull();
            assertThat(verifyTask).isNotNull();
        }
    }
}
