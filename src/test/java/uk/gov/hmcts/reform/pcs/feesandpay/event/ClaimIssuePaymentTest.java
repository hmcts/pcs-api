package uk.gov.hmcts.reform.pcs.feesandpay.event;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.SchedulableInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.ccd.model.AccessCodeTaskData;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.pcs.ccd.task.AccessCodeGenerationComponent.ACCESS_CODE_TASK_DESCRIPTOR;

@ExtendWith(MockitoExtension.class)
class ClaimIssuePaymentTest extends BaseEventTest {

    @Mock
    private SchedulerClient schedulerClient;

    @Mock
    private PcsCaseService pcsCaseService;

    @InjectMocks
    private ClaimIssuePayment paymentEvent;

    @BeforeEach
    void setUp() {
        setEventUnderTest(paymentEvent);
    }

    @Test
    void shouldTransitionToCaseIssued() {
        SubmitResponse<State> response = callSubmitHandler(PCSCase.builder().build());

        assertThat(response.getState()).isEqualTo(State.CASE_ISSUED);
    }

    @Test
    void shouldSetCaseIssuedDateOnSubmitWhenDateIssuedNotSet() {
        callSubmitHandler(PCSCase.builder().build());

        verify(pcsCaseService).setCaseIssuedDate(TEST_CASE_REFERENCE);
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

    @Test
    void shouldDoNothingOnSubmitWhenDateIssuedAlreadySet() {
        PCSCase pcsCase = PCSCase.builder()
            .dateIssued(LocalDateTime.of(2026, 1, 1, 9, 0, 0))
            .build();

        callSubmitHandler(pcsCase);

        verify(pcsCaseService, never()).setCaseIssuedDate(TEST_CASE_REFERENCE);
        verify(schedulerClient, never()).scheduleIfNotExists(any());
    }
}
