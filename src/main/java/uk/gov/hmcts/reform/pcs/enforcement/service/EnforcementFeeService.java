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

@Service
@RequiredArgsConstructor
public class EnforcementFeeService {

    private final FeeService feeService;
    private final PartyService partyService;
    private final SchedulerClient schedulerClient;

    /**
     * The current fee for the enforcement type.
     *
     * @throws FeeNotFoundException if the Fees Register cannot be reached or has no such fee
     */
    public FeeDetails lookUpFee(SelectEnforcementType enforcementType) {
        FeeType feeType = EnforcementApplicationFee.findFeeType(enforcementType)
            .orElseThrow(() -> new IllegalArgumentException("There is no fee for a " + enforcementType));
        return feeService.getFee(feeType);
    }

    /**
     * A failure to queue the fees-and-pay task is thrown; call this through {@link EnforcementSideEffects}.
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

        // The real time, not the injected clock's: db-scheduler decides when a task is due against the
        // system clock, so a task scheduled from a clock the tests have moved forwards would never be due.
        schedulerClient.scheduleIfNotExists(
            FEES_AND_PAY_TASK_DESCRIPTOR
                .instance(instanceId(enforcementId))
                .data(taskData)
                .scheduledTo(Instant.now())
        );
    }

    static String instanceId(UUID enforcementId) {
        return "enforcement-" + enforcementId;
    }
}
