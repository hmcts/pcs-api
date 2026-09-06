package uk.gov.hmcts.reform.pcs.ccd.service;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.SchedulableInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.model.AccessCodeTaskData;
import uk.gov.hmcts.reform.pcs.ccd.service.claimform.ClaimFormScheduler;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.task.AccessCodeGenerationComponent.ACCESS_CODE_TASK_DESCRIPTOR;

@ExtendWith(MockitoExtension.class)
class CaseIssueServiceTest {

    private static final long CASE_REFERENCE = 1234L;

    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private ClaimService claimService;
    @Mock
    private ClaimFormScheduler claimFormScheduler;
    @Mock
    private DefendantAccessCodeService defendantAccessCodeService;
    @Mock
    private SchedulerClient schedulerClient;

    @InjectMocks
    private CaseIssueService underTest;

    private ClaimEntity claim;

    @BeforeEach
    void setUp() {
        claim = new ClaimEntity();
        PcsCaseEntity pcsCase = PcsCaseEntity.builder()
            .caseReference(CASE_REFERENCE)
            .claims(List.of(claim))
            .build();
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCase);
    }

    @Test
    void issuesTheCaseAndSchedulesClaimFormWhenNotYetIssued() {
        when(defendantAccessCodeService.findDefendantPartyIdsNeedingAccessCode(CASE_REFERENCE))
            .thenReturn(List.of());

        underTest.issueCaseIfNotIssued(CASE_REFERENCE);

        verify(claimService).setClaimIssuedDate(claim);
        verify(claimFormScheduler).scheduleClaimFormGeneration(CASE_REFERENCE);
        verify(schedulerClient, never()).scheduleIfNotExists(any());
    }

    @Test
    void schedulesOneAccessCodeLetterTaskPerDefendant() {
        UUID defendantOne = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID defendantTwo = UUID.fromString("22222222-2222-2222-2222-222222222222");
        when(defendantAccessCodeService.findDefendantPartyIdsNeedingAccessCode(CASE_REFERENCE))
            .thenReturn(List.of(defendantOne, defendantTwo));

        underTest.issueCaseIfNotIssued(CASE_REFERENCE);

        ArgumentCaptor<SchedulableInstance<?>> captor = ArgumentCaptor.forClass(SchedulableInstance.class);
        verify(schedulerClient, times(2)).scheduleIfNotExists(captor.capture());
        List<SchedulableInstance<?>> scheduled = captor.getAllValues();

        assertThat(scheduled).allSatisfy(instance ->
            assertThat(instance.getTaskInstance().getTaskName())
                .isEqualTo(ACCESS_CODE_TASK_DESCRIPTOR.getTaskName()));
        assertThat(scheduled).extracting(instance -> instance.getTaskInstance().getId())
            .containsExactlyInAnyOrder(
                CASE_REFERENCE + ":" + defendantOne,
                CASE_REFERENCE + ":" + defendantTwo);
        assertThat(scheduled)
            .extracting(instance -> (AccessCodeTaskData) instance.getTaskInstance().getData())
            .allSatisfy(data ->
                assertThat(data.getCaseReference()).isEqualTo(String.valueOf(CASE_REFERENCE)))
            .extracting(AccessCodeTaskData::getDefendantPartyId)
            .containsExactlyInAnyOrder(defendantOne.toString(), defendantTwo.toString());
    }

    @Test
    void doesNothingWhenTheCaseIsAlreadyIssued() {
        claim.setClaimIssuedDate(LocalDateTime.of(2026, 1, 1, 9, 0, 0));

        underTest.issueCaseIfNotIssued(CASE_REFERENCE);

        verifyNoInteractions(claimService, claimFormScheduler, schedulerClient, defendantAccessCodeService);
    }
}
