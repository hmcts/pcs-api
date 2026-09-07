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
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.ccd.model.AccessCodeTaskData;
import uk.gov.hmcts.reform.pcs.ccd.model.FeePaymentStatusChangeTaskData;
import uk.gov.hmcts.reform.pcs.ccd.service.DefendantAccessCodeService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.claimform.ClaimFormScheduler;
import uk.gov.hmcts.reform.pcs.ccd.task.FeePaymentPaidNotificationTaskComponent;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentCallbackHandlerType;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CTSC_ADMIN;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CTSC_TEAM_LEADER;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.HEARING_CENTRE_ADMIN;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.HEARING_CENTRE_TEAM_LEADER;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.JUDGE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.LEADERSHIP_JUDGE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.WLU_ADMIN;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.WLU_TEAM_LEADER;
import static uk.gov.hmcts.reform.pcs.ccd.task.AccessCodeGenerationComponent.ACCESS_CODE_TASK_DESCRIPTOR;

@ExtendWith(MockitoExtension.class)
class ClaimIssuePaymentTest extends BaseEventTest {

    private static final Integer FEE_PAYMENT_ID = 42;

    @Mock
    private SchedulerClient schedulerClient;

    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private ClaimFormScheduler claimFormScheduler;

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
        lenient().when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCaseWithClaimFeePayment());
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
    void shouldScheduleClaimIssuedNotificationWhenDateIssuedNotSet() {
        callSubmitHandler(PCSCase.builder().build());

        ArgumentCaptor<SchedulableInstance<?>> captor = ArgumentCaptor.forClass(SchedulableInstance.class);
        verify(schedulerClient).scheduleIfNotExists(captor.capture());

        SchedulableInstance<?> scheduled = captor.getValue();
        assertThat(scheduled.getTaskInstance().getTaskName())
            .isEqualTo(FeePaymentPaidNotificationTaskComponent.FEE_PAYMENT_PAID_TASK_DESCRIPTOR.getTaskName());
        FeePaymentStatusChangeTaskData data =
            (FeePaymentStatusChangeTaskData) scheduled.getTaskInstance().getData();
        assertThat(data.getFeePaymentId()).isEqualTo(FEE_PAYMENT_ID);
    }

    @Test
    void shouldScheduleOneAccessCodeLetterTaskPerDefendantOnCaseIssued() {
        UUID defendantOne = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID defendantTwo = UUID.fromString("22222222-2222-2222-2222-222222222222");
        when(defendantAccessCodeService.findDefendantPartyIdsNeedingAccessCode(TEST_CASE_REFERENCE))
            .thenReturn(List.of(defendantOne, defendantTwo));

        callSubmitHandler(PCSCase.builder().build());

        ArgumentCaptor<SchedulableInstance<?>> captor = ArgumentCaptor.forClass(SchedulableInstance.class);
        verify(schedulerClient, times(3)).scheduleIfNotExists(captor.capture());

        List<SchedulableInstance<?>> scheduled = captor.getAllValues();
        List<SchedulableInstance<?>> accessCodeTasks = scheduled.stream()
            .filter(instance -> ACCESS_CODE_TASK_DESCRIPTOR.getTaskName()
                .equals(instance.getTaskInstance().getTaskName()))
            .toList();
        assertThat(accessCodeTasks).hasSize(2);

        assertThat(accessCodeTasks).extracting(instance -> instance.getTaskInstance().getId())
            .containsExactlyInAnyOrder(
                TEST_CASE_REFERENCE + ":" + defendantOne,
                TEST_CASE_REFERENCE + ":" + defendantTwo);

        assertThat(accessCodeTasks)
            .extracting(instance -> (AccessCodeTaskData) instance.getTaskInstance().getData())
            .allSatisfy(data ->
                assertThat(data.getCaseReference()).isEqualTo(String.valueOf(TEST_CASE_REFERENCE)))
            .extracting(AccessCodeTaskData::getDefendantPartyId)
            .containsExactlyInAnyOrder(defendantOne.toString(), defendantTwo.toString());

        assertThat(scheduled)
            .filteredOn(instance -> FeePaymentPaidNotificationTaskComponent.FEE_PAYMENT_PAID_TASK_DESCRIPTOR
                .getTaskName().equals(instance.getTaskInstance().getTaskName()))
            .hasSize(1);
    }

    @Test
    void shouldScheduleOnlyClaimIssuedNotificationWhenNoDefendantsNeedAccessCode() {
        when(defendantAccessCodeService.findDefendantPartyIdsNeedingAccessCode(TEST_CASE_REFERENCE))
            .thenReturn(List.of());

        callSubmitHandler(PCSCase.builder().build());

        verify(pcsCaseService).setCaseIssuedDate(TEST_CASE_REFERENCE);
        ArgumentCaptor<SchedulableInstance<?>> captor = ArgumentCaptor.forClass(SchedulableInstance.class);
        verify(schedulerClient).scheduleIfNotExists(captor.capture());
        assertThat(captor.getValue().getTaskInstance().getTaskName())
            .isEqualTo(FeePaymentPaidNotificationTaskComponent.FEE_PAYMENT_PAID_TASK_DESCRIPTOR.getTaskName());
    }

    @Test
    void shouldSkipClaimIssuedNotificationWhenNoClaimFeePaymentFound() {
        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCaseWithoutFeePayments());

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
        verify(pcsCaseService, never()).loadCase(anyLong());
        verify(schedulerClient, never()).scheduleIfNotExists(any());
    }

    @Test
    void shouldGrantReadAccessToInternalServiceRequestRoles() {
        assertThat(configuredEvent.getGrants().get(CTSC_ADMIN)).contains(Permission.R);
        assertThat(configuredEvent.getGrants().get(CTSC_TEAM_LEADER)).contains(Permission.R);
        assertThat(configuredEvent.getGrants().get(HEARING_CENTRE_ADMIN)).contains(Permission.R);
        assertThat(configuredEvent.getGrants().get(HEARING_CENTRE_TEAM_LEADER)).contains(Permission.R);
        assertThat(configuredEvent.getGrants().get(JUDGE)).contains(Permission.R);
        assertThat(configuredEvent.getGrants().get(LEADERSHIP_JUDGE)).contains(Permission.R);
        assertThat(configuredEvent.getGrants().get(WLU_ADMIN)).contains(Permission.R);
        assertThat(configuredEvent.getGrants().get(WLU_TEAM_LEADER)).contains(Permission.R);
    }

    @Test
    void shouldSetCaseIssuedStateOnSubmit() {
        PCSCase pcsCase = PCSCase.builder().build();

        SubmitResponse<State> response = callSubmitHandler(pcsCase);

        assertThat(response.getState()).isEqualTo(State.CASE_ISSUED);
        verify(claimFormScheduler).scheduleClaimFormGeneration(anyLong());
    }

    private PcsCaseEntity pcsCaseWithClaimFeePayment() {
        ClaimEntity claimEntity = new ClaimEntity();
        claimEntity.setFeePayments(new ArrayList<>(List.of(
            FeePaymentEntity.builder()
                .id(FEE_PAYMENT_ID)
                .paymentCallbackHandlerType(PaymentCallbackHandlerType.CLAIM)
                .build()
        )));

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .caseReference(TEST_CASE_REFERENCE)
            .build();
        pcsCaseEntity.setClaims(new ArrayList<>(List.of(claimEntity)));
        return pcsCaseEntity;
    }

    private PcsCaseEntity pcsCaseWithoutFeePayments() {
        ClaimEntity claimEntity = new ClaimEntity();
        claimEntity.setFeePayments(new ArrayList<>());

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .caseReference(TEST_CASE_REFERENCE)
            .build();
        pcsCaseEntity.setClaims(new ArrayList<>(List.of(claimEntity)));
        return pcsCaseEntity;
    }
}
