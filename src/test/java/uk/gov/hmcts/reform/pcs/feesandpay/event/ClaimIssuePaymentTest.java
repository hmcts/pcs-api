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
import uk.gov.hmcts.reform.pcs.ccd.service.DefendantAccessCodeService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.task.AccessCodeGenerationComponent.ACCESS_CODE_TASK_DESCRIPTOR;

@ExtendWith(MockitoExtension.class)
class ClaimIssuePaymentTest extends BaseEventTest {

    @Mock
    private SchedulerClient schedulerClient;

    @Mock
    private PcsCaseService pcsCaseService;

    @Mock
    private DefendantAccessCodeService defendantAccessCodeService;

    @InjectMocks
    private ClaimIssuePayment paymentEvent;

    @BeforeEach
    void setUp() {
        setEventUnderTest(paymentEvent);
        // Default: no defendants need a code (tests that schedule override this).
        lenient().when(defendantAccessCodeService.findDefendantPartyIdsNeedingAccessCode(anyLong()))
            .thenReturn(List.of());
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
    void shouldScheduleOneAccessCodeLetterTaskPerDefendantOnCaseIssued() {
        UUID defendantOne = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID defendantTwo = UUID.fromString("22222222-2222-2222-2222-222222222222");
        when(defendantAccessCodeService.findDefendantPartyIdsNeedingAccessCode(TEST_CASE_REFERENCE))
            .thenReturn(List.of(defendantOne, defendantTwo));

        callSubmitHandler(PCSCase.builder().build());

        ArgumentCaptor<SchedulableInstance<?>> captor = ArgumentCaptor.forClass(SchedulableInstance.class);
        verify(schedulerClient, times(2)).scheduleIfNotExists(captor.capture());

        List<SchedulableInstance<?>> scheduled = captor.getAllValues();
        assertThat(scheduled).hasSize(2);

        // One task per defendant, instance keyed by caseRef:partyId, payload carrying both ids.
        assertThat(scheduled).allSatisfy(instance ->
            assertThat(instance.getTaskInstance().getTaskName())
                .isEqualTo(ACCESS_CODE_TASK_DESCRIPTOR.getTaskName()));

        assertThat(scheduled).extracting(instance -> instance.getTaskInstance().getId())
            .containsExactlyInAnyOrder(
                TEST_CASE_REFERENCE + ":" + defendantOne,
                TEST_CASE_REFERENCE + ":" + defendantTwo);

        assertThat(scheduled)
            .extracting(instance -> (AccessCodeTaskData) instance.getTaskInstance().getData())
            .allSatisfy(data ->
                assertThat(data.getCaseReference()).isEqualTo(String.valueOf(TEST_CASE_REFERENCE)))
            .extracting(AccessCodeTaskData::getDefendantPartyId)
            .containsExactlyInAnyOrder(defendantOne.toString(), defendantTwo.toString());
    }

    @Test
    void shouldScheduleNoTasksWhenNoDefendantsNeedAccessCode() {
        when(defendantAccessCodeService.findDefendantPartyIdsNeedingAccessCode(TEST_CASE_REFERENCE))
            .thenReturn(List.of());

        callSubmitHandler(PCSCase.builder().build());

        verify(pcsCaseService).setCaseIssuedDate(TEST_CASE_REFERENCE);
        verify(schedulerClient, never()).scheduleIfNotExists(any());
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
