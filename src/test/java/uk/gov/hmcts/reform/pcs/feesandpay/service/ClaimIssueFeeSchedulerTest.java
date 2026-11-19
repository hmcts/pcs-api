package uk.gov.hmcts.reform.pcs.feesandpay.service;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.SchedulableInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.feesandpay.model.BulkPbaDetails;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeType;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeesAndPayTaskData;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentCallbackHandlerType;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimIssueFeeSchedulerTest {

    private static final long CASE_REFERENCE = 1234L;

    @Mock
    private FeeService feeService;
    @Mock
    private PartyService partyService;
    @Mock
    private SchedulerClient schedulerClient;
    @Mock
    private PartyEntity claimantParty;

    @InjectMocks
    private ClaimIssueFeeScheduler underTest;

    @Test
    void shouldScheduleFeesAndPayTaskWithoutPbaDetails() {
        // Given
        FeeDetails feeDetails = stubFeeAndParty();

        // When
        FeeDetails result = underTest.scheduleCaseIssueFeePayment(CASE_REFERENCE, claimantParty);

        // Then
        assertThat(result).isSameAs(feeDetails);
        SchedulableInstance<FeesAndPayTaskData> scheduled = captureScheduled();
        assertThat(scheduled.getTaskInstance().getTaskName()).isEqualTo("fees-and-pay-task");
        FeesAndPayTaskData data = scheduled.getTaskInstance().getData();
        assertThat(data.getFeeDetails()).isSameAs(feeDetails);
        assertThat(data.getCaseReference()).isEqualTo(CASE_REFERENCE);
        assertThat(data.getCcdCaseNumber()).isEqualTo("1234");
        assertThat(data.getResponsiblePartyName()).isEqualTo("Claimant Ltd");
        assertThat(data.getPaymentCallbackHandlerType()).isEqualTo(PaymentCallbackHandlerType.CLAIM);
        assertThat(data.getPbaAccountNumber()).isNull();
        assertThat(data.getPbaCustomerReference()).isNull();
        assertThat(data.getOrganisationName()).isNull();
        assertThat(data.getBulkClaimSubmissionId()).isNull();
    }

    @Test
    void shouldTreatNullPbaDetailsAsUiPath() {
        // Given
        stubFeeAndParty();

        // When
        underTest.scheduleCaseIssueFeePayment(CASE_REFERENCE, claimantParty, null);

        // Then
        assertThat(captureScheduled().getTaskInstance().getData().getPbaAccountNumber()).isNull();
    }

    @Test
    void shouldCopyPbaDetailsToTaskData() {
        // Given
        stubFeeAndParty();
        UUID submissionId = UUID.randomUUID();

        // When
        underTest.scheduleCaseIssueFeePayment(CASE_REFERENCE, claimantParty,
            new BulkPbaDetails("PBA0123456", "CUST-REF", "Some Org", submissionId));

        // Then
        FeesAndPayTaskData data = captureScheduled().getTaskInstance().getData();
        assertThat(data.getPbaAccountNumber()).isEqualTo("PBA0123456");
        assertThat(data.getPbaCustomerReference()).isEqualTo("CUST-REF");
        assertThat(data.getOrganisationName()).isEqualTo("Some Org");
        assertThat(data.getBulkClaimSubmissionId()).isEqualTo(submissionId);
    }

    private FeeDetails stubFeeAndParty() {
        FeeDetails feeDetails = FeeDetails.builder().feeAmount(new BigDecimal("123.40")).build();
        when(feeService.getFee(FeeType.CASE_ISSUE_FEE)).thenReturn(feeDetails);
        when(claimantParty.getId()).thenReturn(UUID.randomUUID());
        when(partyService.getPartyName(claimantParty)).thenReturn("Claimant Ltd");
        return feeDetails;
    }

    @SuppressWarnings("unchecked")
    private SchedulableInstance<FeesAndPayTaskData> captureScheduled() {
        ArgumentCaptor<SchedulableInstance<FeesAndPayTaskData>> captor = ArgumentCaptor.forClass(
            SchedulableInstance.class);
        verify(schedulerClient).scheduleIfNotExists(captor.capture());
        return captor.getValue();
    }

}
