package uk.gov.hmcts.reform.pcs.feesandpay.event;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.SchedulableInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.ccd.model.AccessCodeTaskData;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.pcs.ccd.task.AccessCodeGenerationComponent.ACCESS_CODE_TASK_DESCRIPTOR;

@ExtendWith(MockitoExtension.class)
class ClaimIssuePaymentTest extends BaseEventTest {

    @Mock
    private SchedulerClient schedulerClient;

    @BeforeEach
    void setUp() {
        setEventUnderTest(new ClaimIssuePayment(schedulerClient));
    }

    @Test
    void shouldTransitionToCaseIssued() {
        SubmitResponse<State> response = callSubmitHandler(PCSCase.builder().build());

        assertThat(response.getState()).isEqualTo(State.CASE_ISSUED);
    }

    @Test
    void shouldScheduleAccessCodePinPackGenerationOnCaseIssued() {
        callSubmitHandler(PCSCase.builder().build());

        ArgumentCaptor<SchedulableInstance<?>> captor = ArgumentCaptor.forClass(SchedulableInstance.class);
        verify(schedulerClient).scheduleIfNotExists(captor.capture());

        List<SchedulableInstance<?>> scheduled = captor.getAllValues();
        assertThat(scheduled).hasSize(1);

        SchedulableInstance<?> instance = scheduled.getFirst();
        assertThat(instance.getTaskInstance().getTaskName())
            .isEqualTo(ACCESS_CODE_TASK_DESCRIPTOR.getTaskName());

        AccessCodeTaskData taskData = (AccessCodeTaskData) instance.getTaskInstance().getData();
        assertThat(taskData.getCaseReference()).isEqualTo(String.valueOf(TEST_CASE_REFERENCE));
    }
}
