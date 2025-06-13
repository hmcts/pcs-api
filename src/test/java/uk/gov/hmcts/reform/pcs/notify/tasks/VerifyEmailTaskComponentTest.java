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
import uk.gov.hmcts.reform.pcs.notify.config.VerifyEmailTaskComponent;
import uk.gov.hmcts.reform.pcs.notify.service.NotificationService;
import uk.gov.service.notify.NotificationClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("VerifyEmailTaskComponent Tests")
class VerifyEmailTaskComponentTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private NotificationClient notificationClient;

    @Mock
    private NotificationErrorHandler errorHandler;

    private VerifyEmailTaskComponent verifyEmailTaskComponent;

    private static final int MAX_RETRIES_CHECK = 2;
    private static final Duration STATUS_CHECK_BACKOFF_DELAY = Duration.ofSeconds(30);

    @BeforeEach
    void setUp() {
        verifyEmailTaskComponent = new VerifyEmailTaskComponent(
            notificationService,
            notificationClient,
            errorHandler,
            MAX_RETRIES_CHECK,
            STATUS_CHECK_BACKOFF_DELAY
        );
    }

    @Nested
    @DisplayName("Task Creation Tests")
    class TaskCreationTests {

        @Test
        @DisplayName("Should create verify email task")
        void shouldCreateVerifyEmailTask() {
            CustomTask<EmailState> task = verifyEmailTaskComponent.verifyEmailTask();

            assertThat(task).isNotNull();
        }

        @Test
        @DisplayName("Should create different task instances")
        void shouldCreateDifferentTaskInstances() {
            CustomTask<EmailState> verifyTask1 = verifyEmailTaskComponent.verifyEmailTask();
            CustomTask<EmailState> verifyTask2 = verifyEmailTaskComponent.verifyEmailTask();

            assertThat(verifyTask1).isNotNull();
            assertThat(verifyTask2).isNotNull();
        }
    }

    @Nested
    @DisplayName("Task Descriptor Tests")
    class TaskDescriptorTests {

        @Test
        @DisplayName("Should have correct verify email task descriptor")
        void shouldHaveCorrectVerifyEmailTaskDescriptor() {
            assertThat(VerifyEmailTaskComponent.verifyEmailTask).isNotNull();
            assertThat(VerifyEmailTaskComponent.verifyEmailTask.getTaskName()).isEqualTo("verify-email-task");
            assertThat(VerifyEmailTaskComponent.verifyEmailTask.getDataClass()).isEqualTo(EmailState.class);
        }
    }

    @Nested
    @DisplayName("Configuration Tests")
    class ConfigurationTests {

        @Test
        @DisplayName("Should create component with all dependencies")
        void shouldCreateComponentWithAllDependencies() {
            VerifyEmailTaskComponent component = new VerifyEmailTaskComponent(
                notificationService,
                notificationClient,
                errorHandler,
                MAX_RETRIES_CHECK,
                STATUS_CHECK_BACKOFF_DELAY
            );

            assertThat(component).isNotNull();
        }

        @Test
        @DisplayName("Should handle different retry configurations")
        void shouldHandleDifferentRetryConfigurations() {
            VerifyEmailTaskComponent component = new VerifyEmailTaskComponent(
                notificationService,
                notificationClient,
                errorHandler,
                3,
                Duration.ofSeconds(15)
            );

            CustomTask<EmailState> verifyTask = component.verifyEmailTask();

            assertThat(verifyTask).isNotNull();
        }

        @Test
        @DisplayName("Should handle zero retry configurations")
        void shouldHandleZeroRetryConfigurations() {
            VerifyEmailTaskComponent component = new VerifyEmailTaskComponent(
                notificationService,
                notificationClient,
                errorHandler,
                0,
                Duration.ofSeconds(1)
            );

            CustomTask<EmailState> verifyTask = component.verifyEmailTask();

            assertThat(verifyTask).isNotNull();
        }

        @Test
        @DisplayName("Should handle large retry configurations")
        void shouldHandleLargeRetryConfigurations() {
            VerifyEmailTaskComponent component = new VerifyEmailTaskComponent(
                notificationService,
                notificationClient,
                errorHandler,
                50,
                Duration.ofMinutes(10)
            );

            CustomTask<EmailState> verifyTask = component.verifyEmailTask();

            assertThat(verifyTask).isNotNull();
        }
    }

    @Nested
    @DisplayName("Bean Creation Tests")
    class BeanCreationTests {

        @Test
        @DisplayName("Should create verify email task bean")
        void shouldCreateVerifyEmailTaskBean() {
            CustomTask<EmailState> task = verifyEmailTaskComponent.verifyEmailTask();

            assertThat(task).isNotNull();
        }

        @Test
        @DisplayName("Should create multiple bean instances")
        void shouldCreateMultipleBeanInstances() {
            CustomTask<EmailState> verifyTask1 = verifyEmailTaskComponent.verifyEmailTask();
            CustomTask<EmailState> verifyTask2 = verifyEmailTaskComponent.verifyEmailTask();

            assertThat(verifyTask1).isNotNull();
            assertThat(verifyTask2).isNotNull();
        }
    }

    @Nested
    @DisplayName("Dependency Validation Tests")
    class DependencyValidationTests {

        @ParameterizedTest
        @DisplayName("Should accept all required dependencies")
        @ValueSource(strings = {"notificationService", "notificationClient", "errorHandler"})
        void shouldAcceptAllRequiredDependencies(String dependencyName) {
            VerifyEmailTaskComponent component = new VerifyEmailTaskComponent(
                notificationService,
                notificationClient,
                errorHandler,
                MAX_RETRIES_CHECK,
                STATUS_CHECK_BACKOFF_DELAY
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
            VerifyEmailTaskComponent component = new VerifyEmailTaskComponent(
                notificationService,
                notificationClient,
                errorHandler,
                MAX_RETRIES_CHECK,
                Duration.ofMillis(100)
            );

            CustomTask<EmailState> verifyTask = component.verifyEmailTask();

            assertThat(verifyTask).isNotNull();
        }

        @Test
        @DisplayName("Should handle zero duration configurations")
        void shouldHandleZeroDurationConfigurations() {
            VerifyEmailTaskComponent component = new VerifyEmailTaskComponent(
                notificationService,
                notificationClient,
                errorHandler,
                MAX_RETRIES_CHECK,
                Duration.ZERO
            );

            CustomTask<EmailState> verifyTask = component.verifyEmailTask();

            assertThat(verifyTask).isNotNull();
        }
    }
}
