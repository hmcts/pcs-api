package uk.gov.hmcts.reform.pcs.enforcement.service;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.SchedulableInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.feesandpay.exception.FeeNotFoundException;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeType;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeesAndPayTaskData;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentCallbackHandlerType;
import uk.gov.hmcts.reform.pcs.feesandpay.service.FeeService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnforcementFeeServiceTest {

    private static final long CASE_REFERENCE = 1234567890123456L;
    private static final UUID ENFORCEMENT_ID = UUID.fromString("0f0e0d0c-0b0a-0908-0706-050403020100");
    private static final UUID CLAIMANT_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final FeeDetails FEE = FeeDetails.builder()
        .code("FEE0380")
        .feeAmount(new BigDecimal("148.00"))
        .version(5)
        .build();

    @Mock
    private FeeService feeService;
    @Mock
    private PartyService partyService;
    @Mock
    private SchedulerClient schedulerClient;

    @InjectMocks
    private EnforcementFeeService underTest;

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @ParameterizedTest
    @CsvSource({"WARRANT, ENFORCEMENT_WARRANT_FEE", "WRIT, ENFORCEMENT_WRIT_FEE"})
    void shouldLookUpTheTypesFee(SelectEnforcementType enforcementType, FeeType feeType) {
        when(feeService.getFee(feeType)).thenReturn(FEE);

        assertThat(underTest.lookUpFee(enforcementType)).isSameAs(FEE);
    }

    @ParameterizedTest
    @EnumSource(value = SelectEnforcementType.class, names = {"WARRANT_OF_RESTITUTION", "WRIT_OF_RESTITUTION"})
    void shouldRefuseToLookUpAFeeForATypeWithNone(SelectEnforcementType enforcementType) {
        assertThatThrownBy(() -> underTest.lookUpFee(enforcementType))
            .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(feeService);
    }

    @Test
    void shouldPassOnAFeeLookUpFailure() {
        when(feeService.getFee(FeeType.ENFORCEMENT_WRIT_FEE)).thenThrow(new FeeNotFoundException("down"));

        assertThatThrownBy(() -> underTest.lookUpFee(SelectEnforcementType.WRIT))
            .isInstanceOf(FeeNotFoundException.class);
    }

    @Test
    void shouldScheduleAServiceRequestForTheEnforcementPaidByTheClaimant() {
        PcsCaseEntity pcsCase = givenACaseWithAClaimant();

        underTest.requestPaymentAfterCommit(pcsCase, ENFORCEMENT_ID, FEE);

        SchedulableInstance<?> instance = scheduled(1).getFirst();
        assertThat(instance.getTaskInstance().getTaskName()).isEqualTo("fees-and-pay-task");
        assertThat(instance.getTaskInstance().getId()).isEqualTo("enforcement-" + ENFORCEMENT_ID);
        assertThat(instance.getTaskInstance().getData()).isEqualTo(FeesAndPayTaskData.builder()
            .feeDetails(FEE)
            .caseReference(CASE_REFERENCE)
            .ccdCaseNumber(String.valueOf(CASE_REFERENCE))
            .responsiblePartyId(CLAIMANT_ID)
            .responsiblePartyName("Possession Claims Solicitor Org")
            .paymentCallbackHandlerType(PaymentCallbackHandlerType.ENFORCEMENT)
            .relatedEntityId(ENFORCEMENT_ID)
            .build());
    }

    @Test
    void shouldWaitForTheTransactionToCommit() {
        PcsCaseEntity pcsCase = givenACaseWithAClaimant();
        TransactionSynchronizationManager.initSynchronization();

        underTest.requestPaymentAfterCommit(pcsCase, ENFORCEMENT_ID, FEE);
        verifyNoInteractions(schedulerClient);

        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
        synchronizations.forEach(TransactionSynchronization::afterCommit);
        assertThat(scheduled(1)).hasSize(1);
    }

    @Test
    void shouldNotThrowWhenSchedulingFailsAfterTheCommit() {
        PcsCaseEntity pcsCase = givenACaseWithAClaimant();
        doThrow(new IllegalStateException("database down")).when(schedulerClient).scheduleIfNotExists(any());

        underTest.requestPaymentAfterCommit(pcsCase, ENFORCEMENT_ID, FEE);

        verify(schedulerClient).scheduleIfNotExists(any());
    }

    private PcsCaseEntity givenACaseWithAClaimant() {
        PcsCaseEntity pcsCase = PcsCaseEntity.builder().caseReference(CASE_REFERENCE).build();
        PartyEntity claimant = PartyEntity.builder().id(CLAIMANT_ID).orgName("Possession Claims Solicitor Org").build();
        when(partyService.getPrimaryClaimantPartyEntity(pcsCase)).thenReturn(claimant);
        when(partyService.getPartyName(claimant)).thenReturn("Possession Claims Solicitor Org");
        return pcsCase;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<SchedulableInstance<?>> scheduled(int count) {
        ArgumentCaptor<SchedulableInstance> captor = ArgumentCaptor.forClass(SchedulableInstance.class);
        verify(schedulerClient, times(count)).scheduleIfNotExists(captor.capture());
        return (List) captor.getAllValues();
    }
}
