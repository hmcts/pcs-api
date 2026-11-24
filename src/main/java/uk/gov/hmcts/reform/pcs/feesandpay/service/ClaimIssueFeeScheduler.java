package uk.gov.hmcts.reform.pcs.feesandpay.service;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.feesandpay.model.BulkPbaDetails;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeType;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeesAndPayTaskData;

import java.time.Instant;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentCallbackHandlerType.CLAIM;
import static uk.gov.hmcts.reform.pcs.feesandpay.task.FeesAndPayTaskComponent.FEES_AND_PAY_TASK_DESCRIPTOR;

@Service
@RequiredArgsConstructor
public class ClaimIssueFeeScheduler {

    private final FeeService feeService;
    private final PartyService partyService;
    private final SchedulerClient schedulerClient;

    public FeeDetails scheduleCaseIssueFeePayment(long caseReference, PartyEntity claimantParty) {
        return scheduleCaseIssueFeePayment(caseReference, claimantParty, null);
    }

    /**
     * Schedules creation of the claim issue fee service request. When {@code pbaDetails} is supplied the fee is
     * subsequently paid automatically from that PBA account.
     */
    public FeeDetails scheduleCaseIssueFeePayment(long caseReference, PartyEntity claimantParty,
                                                  BulkPbaDetails pbaDetails) {
        FeeDetails feeDetails = feeService.getFee(FeeType.CASE_ISSUE_FEE);
        FeesAndPayTaskData.FeesAndPayTaskDataBuilder taskData = FeesAndPayTaskData.builder()
            .feeDetails(feeDetails)
            .ccdCaseNumber(String.valueOf(caseReference))
            .caseReference(caseReference)
            .responsiblePartyId(claimantParty.getId())
            .responsiblePartyName(partyService.getPartyName(claimantParty))
            .paymentCallbackHandlerType(CLAIM);

        if (pbaDetails != null) {
            taskData
                .pbaAccountNumber(pbaDetails.pbaAccountNumber())
                .pbaCustomerReference(pbaDetails.pbaCustomerReference())
                .organisationName(pbaDetails.organisationName())
                .bulkClaimSubmissionId(pbaDetails.bulkClaimSubmissionId());
        }

        schedulerClient.scheduleIfNotExists(
            FEES_AND_PAY_TASK_DESCRIPTOR
                .instance(UUID.randomUUID().toString())
                .data(taskData.build())
                .scheduledTo(Instant.now())
        );

        return feeDetails;
    }

}
