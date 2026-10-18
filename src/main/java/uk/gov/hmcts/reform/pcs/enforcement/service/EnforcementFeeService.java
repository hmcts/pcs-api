package uk.gov.hmcts.reform.pcs.enforcement.service;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementApplicationFee;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;
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
     * <p>The fees-and-pay task is written in the event's transaction, which db-scheduler joins, so no service
     * request is raised for an application that is rolled back and a committed application always has one
     * queued. Nothing here talks to Payments, so a Payments outage cannot fail the event: the task retries on its
     * own. A failure to write the task is thrown, so a submit handler has to call this through
     * {@link EnforcementSideEffects} to answer it as an error rather than a 500.</p>
     */
    public void requestPayment(PcsCaseEntity pcsCase, UUID enforcementId, FeeDetails feeDetails) {
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

        // The real time, not the injected clock's: db-scheduler decides when a task is due against the real
        // system clock, so a task scheduled from a clock the end to end tests have moved forwards would wait
        // for the real time to catch up.
        schedulerClient.scheduleIfNotExists(
            FEES_AND_PAY_TASK_DESCRIPTOR
                .instance(instanceId(enforcementId))
                .data(taskData)
                .scheduledTo(Instant.now())
        );
    }

    /**
     * One service request per enforcement, so scheduling it twice is a no-op.
     */
    static String instanceId(UUID enforcementId) {
        return "enforcement-" + enforcementId;
    }
}
