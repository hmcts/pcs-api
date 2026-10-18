package uk.gov.hmcts.reform.pcs.enforcement.service;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementApplicationFee;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.enforcement.util.AfterCommit;
import uk.gov.hmcts.reform.pcs.feesandpay.exception.FeeNotFoundException;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeType;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeesAndPayTaskData;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentCallbackHandlerType;
import uk.gov.hmcts.reform.pcs.feesandpay.service.FeeService;

import java.time.Instant;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.feesandpay.task.FeesAndPayTaskComponent.FEES_AND_PAY_TASK_DESCRIPTOR;

/**
 * Takes an enforcement application's fee through PCS's standard payments pattern, as claim issue, counterclaims
 * and general applications do: the fees-and-pay task raises a Payments service request for the fee, which the
 * claimant pays from the case's Service Request tab, and the Payments callback for it is handled by
 * {@link EnforcementPaymentCallbackHandler}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnforcementFeeService {

    private final FeeService feeService;
    private final PartyService partyService;
    private final SchedulerClient schedulerClient;

    /**
     * The current fee for applying for a type that has one.
     *
     * @throws FeeNotFoundException if the Fees Register cannot be reached or has no such fee
     * @throws IllegalArgumentException if the type has no fee
     */
    public FeeDetails lookUpFee(SelectEnforcementType enforcementType) {
        FeeType feeType = EnforcementApplicationFee.findFeeType(enforcementType)
            .orElseThrow(() -> new IllegalArgumentException("There is no fee for a " + enforcementType));
        return feeService.getFee(feeType);
    }

    /**
     * Raises a Payments service request for the enforcement's fee, with the claim's claimant as the party paying.
     *
     * <p>The fees-and-pay task is scheduled once the event's transaction has committed, so no service request is
     * raised for an application that is rolled back, and nothing here talks to Payments, so a Payments outage
     * cannot fail the event: the task retries on its own. A failure to schedule is logged rather than thrown,
     * because it happens after the commit and the event has already succeeded.</p>
     */
    public void requestPaymentAfterCommit(PcsCaseEntity pcsCase, UUID enforcementId, FeeDetails feeDetails) {
        long caseReference = pcsCase.getCaseReference();
        PartyEntity claimant = partyService.getPrimaryClaimantPartyEntity(pcsCase);
        FeesAndPayTaskData taskData = FeesAndPayTaskData.builder()
            .feeDetails(feeDetails)
            .ccdCaseNumber(String.valueOf(caseReference))
            .caseReference(caseReference)
            .responsiblePartyId(claimant.getId())
            .responsiblePartyName(partyService.getPartyName(claimant))
            .paymentCallbackHandlerType(PaymentCallbackHandlerType.ENFORCEMENT)
            .relatedEntityId(enforcementId)
            .build();

        AfterCommit.run(() -> schedule(enforcementId, taskData));
    }

    private void schedule(UUID enforcementId, FeesAndPayTaskData taskData) {
        try {
            schedulerClient.scheduleIfNotExists(
                FEES_AND_PAY_TASK_DESCRIPTOR
                    .instance(instanceId(enforcementId))
                    .data(taskData)
                    .scheduledTo(Instant.now())
            );
        } catch (RuntimeException ex) {
            log.error("Failed to schedule the fee service request for enforcement {}", enforcementId, ex);
        }
    }

    /**
     * One service request per enforcement, so scheduling it twice is a no-op.
     */
    static String instanceId(UUID enforcementId) {
        return "enforcement-" + enforcementId;
    }
}
