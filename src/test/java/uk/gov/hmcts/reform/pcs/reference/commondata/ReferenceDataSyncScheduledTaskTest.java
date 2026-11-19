package uk.gov.hmcts.reform.pcs.reference.commondata;

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReferenceDataSyncScheduledTaskTest {

    private static final String SCHEDULE = "DAILY|03:30";

    @Mock
    private ReferenceDataSyncService referenceDataSyncService;

    private ReferenceDataSyncScheduledTask createTask() {
        return new ReferenceDataSyncScheduledTask(SCHEDULE, 3, Duration.ofSeconds(60), referenceDataSyncService);
    }

    @Test
    void shouldBuildRecurringTaskWithExpectedName() {
        // when
        RecurringTask<Void> task = createTask().referenceDataSyncTask();

        // then
        assertThat(task.getName()).isEqualTo("reference-data-sync-task");
    }

    @Test
    void shouldDelegateToSyncServiceWhenEnabled() {
        // when
        createTask().runSync();

        // then
        verify(referenceDataSyncService).synchronise();
    }

    @Test
    void shouldNotRegisterTaskWhenSyncIsNotConfigured() {
        // when / then
        contextRunner().run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(RecurringTask.class);
        });
    }

    @Test
    void shouldNotRegisterTaskWhenSyncIsDisabled() {
        // when / then
        contextRunner().withPropertyValues("common-data.sync.enabled=false").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(RecurringTask.class);
        });
    }

    @Test
    void shouldRegisterTaskWhenSyncIsEnabled() {
        // when / then
        contextRunner().withPropertyValues("common-data.sync.enabled=true").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(RecurringTask.class);
        });
    }

    private ApplicationContextRunner contextRunner() {
        return new ApplicationContextRunner()
            .withInitializer(context -> context.getBeanFactory()
                .setConversionService(ApplicationConversionService.getSharedInstance()))
            .withUserConfiguration(ReferenceDataSyncScheduledTask.class)
            .withBean(ReferenceDataSyncService.class, () -> referenceDataSyncService)
            .withPropertyValues("common-data.sync.schedule=" + SCHEDULE);
    }

    @Test
    void shouldPropagateFailureSoTheSchedulerCanRetry() {
        // given
        ReferenceDataSyncException failure = new ReferenceDataSyncException(List.of("Facilities"));
        doThrow(failure).when(referenceDataSyncService).synchronise();

        // when / then
        assertThatThrownBy(() -> createTask().runSync()).isSameAs(failure);
    }
}
