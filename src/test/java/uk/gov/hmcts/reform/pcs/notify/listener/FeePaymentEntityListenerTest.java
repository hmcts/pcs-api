package uk.gov.hmcts.reform.pcs.notify.listener;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.SchedulableInstance;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.model.FeePaymentStatusChangeTaskData;
import uk.gov.hmcts.reform.pcs.ccd.task.FeePaymentPaidNotificationTaskComponent;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class FeePaymentEntityListenerTest {

    @Mock
    private SchedulerClient schedulerClient;

    @InjectMocks
    private FeePaymentEntityListener underTest;

    private FeePaymentEntity feePaymentEntity;
    private ClaimEntity claim;
    private PartyEntity party;
    private ClaimPartyEntity claimParty;

    @BeforeEach
    void setUp() {
        claim = new ClaimEntity();
        claim.setId(UUID.randomUUID());

        party = new PartyEntity();
        party.setId(UUID.randomUUID());

        claimParty = ClaimPartyEntity.builder()
            .claim(claim)
            .party(party)
            .role(PartyRole.CLAIMANT)
            .build();
        party.setClaimParties(Set.of(claimParty));

        feePaymentEntity = FeePaymentEntity.builder()
            .id(UUID.randomUUID())
            .claim(claim)
            .party(party)
            .paymentStatus(PaymentStatus.NOT_PAID)
            .build();
    }

    @Test
    void shouldSetPreviousPaymentStatusOnPostLoad() {
        underTest.onPostLoad(feePaymentEntity);
        assertThat(feePaymentEntity.getPreviousPaymentStatus()).isEqualTo(PaymentStatus.NOT_PAID);
    }

    @Test
    void shouldScheduleTaskWhenStatusChangesToPaidForClaimant() {
        underTest.onPostLoad(feePaymentEntity);
        feePaymentEntity.setPaymentStatus(PaymentStatus.PAID);

        underTest.onPostUpdate(feePaymentEntity);

        ArgumentCaptor<SchedulableInstance<?>> taskInstanceCaptor = ArgumentCaptor.forClass(SchedulableInstance.class);
        verify(schedulerClient).scheduleIfNotExists(taskInstanceCaptor.capture());

        SchedulableInstance<?> schedulableInstance = taskInstanceCaptor.getValue();
        assertThat(schedulableInstance.getTaskName())
            .isEqualTo(FeePaymentPaidNotificationTaskComponent.FEE_PAYMENT_PAID_TASK_DESCRIPTOR.getTaskName());
        TaskInstance<?> taskInstance = schedulableInstance.getTaskInstance();
        FeePaymentStatusChangeTaskData data = (FeePaymentStatusChangeTaskData) taskInstance.getData();
        assertThat(data.getFeePaymentId()).isEqualTo(feePaymentEntity.getId());
    }

    @Test
    void shouldNotScheduleTaskWhenStatusDoesNotChange() {
        underTest.onPostLoad(feePaymentEntity);

        underTest.onPostUpdate(feePaymentEntity);

        verifyNoInteractions(schedulerClient);
    }

    @Test
    void shouldNotScheduleTaskWhenStatusChangesToNotPaid() {
        feePaymentEntity.setPaymentStatus(PaymentStatus.PAID);
        underTest.onPostLoad(feePaymentEntity);
        feePaymentEntity.setPaymentStatus(PaymentStatus.NOT_PAID);

        underTest.onPostUpdate(feePaymentEntity);

        verifyNoInteractions(schedulerClient);
    }

    @Test
    void shouldNotScheduleTaskWhenNotClaimant() {
        claimParty = ClaimPartyEntity.builder()
            .claim(claim)
            .party(party)
            .role(PartyRole.DEFENDANT)
            .build();
        party.setClaimParties(Set.of(claimParty));
        underTest.onPostLoad(feePaymentEntity);
        feePaymentEntity.setPaymentStatus(PaymentStatus.PAID);

        underTest.onPostUpdate(feePaymentEntity);

        verifyNoInteractions(schedulerClient);
    }

    @Test
    void shouldNotScheduleTaskWhenPartyIsNull() {
        feePaymentEntity.setParty(null);
        underTest.onPostLoad(feePaymentEntity);
        feePaymentEntity.setPaymentStatus(PaymentStatus.PAID);

        underTest.onPostUpdate(feePaymentEntity);

        verifyNoInteractions(schedulerClient);
    }

    @Test
    void shouldScheduleTaskWhenClaimIdsAreDifferentInstancesButSameValue() {
        UUID claimId = claim.getId();
        UUID sameValueClaimId = UUID.fromString(claimId.toString());

        assertThat(sameValueClaimId).isNotSameAs(claimId);
        assertThat(sameValueClaimId).isEqualTo(claimId);

        ClaimEntity differentClaimInstanceSameId = new ClaimEntity();
        differentClaimInstanceSameId.setId(sameValueClaimId);

        claimParty = ClaimPartyEntity.builder()
            .claim(differentClaimInstanceSameId)
            .party(party)
            .role(PartyRole.CLAIMANT)
            .build();
        party.setClaimParties(Set.of(claimParty));

        underTest.onPostLoad(feePaymentEntity);
        feePaymentEntity.setPaymentStatus(PaymentStatus.PAID);

        underTest.onPostUpdate(feePaymentEntity);

        verify(schedulerClient).scheduleIfNotExists(any());
    }
}
