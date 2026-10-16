package uk.gov.hmcts.reform.pcs.ccd.event.enforcetheorder;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.Submit;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.EnforcementOrderService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeType;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeesAndPayTaskData;
import uk.gov.hmcts.reform.pcs.feesandpay.service.FeeService;

import java.time.Instant;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.domain.State.PENDING_ENFORCEMENT_ISSUED;
import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.SelectEnforcementType.getSelectEnforcementTypeFromName;
import static uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentCallbackHandlerType.ENFORCEMENT;
import static uk.gov.hmcts.reform.pcs.feesandpay.task.FeesAndPayTaskComponent.FEES_AND_PAY_TASK_DESCRIPTOR;

@Component("enforcementSubmitEventHandler")
@Slf4j
@RequiredArgsConstructor
public class EnforcementSubmitEvent implements Submit<PCSCase, State> {

    private final EnforcementOrderService enforcementOrderService;
    private final SchedulerClient schedulerClient;
    private final PcsCaseService pcsCaseService;
    private final PartyService partyService;
    private final FeeService feeService;

    @Override
    public SubmitResponse<State> submit(EventPayload<PCSCase, State> payload) {
        long caseReference = payload.caseReference();
        PCSCase pcsCase = payload.caseData();
        EnforcementOrder enforcementOrder = pcsCase.getEnforcementOrder();
        EnforcementFee enforcementFee = getEnforcementFee(enforcementOrder);
        FeeDetails feeDetails = feeService.getFee(enforcementFee.feeType());
        enforcementOrderService.saveAndClearDraftData(caseReference, enforcementOrder);
        scheduleFeePayment(caseReference, getClaimantParty(caseReference), feeDetails);
        log.debug("Saved submitted enforcement order data and deleted draft data for case reference {}", caseReference);
        return SubmitResponse.<State>builder()
            .confirmationBody(getPaymentConfirmationMarkdown(enforcementFee.amount(), caseReference))
            .state(PENDING_ENFORCEMENT_ISSUED).build();
    }

    private record EnforcementFee(FeeType feeType, String amount) {}

    private EnforcementFee getEnforcementFee(EnforcementOrder enforcementOrder) {
        SelectEnforcementType type = getSelectEnforcementType(enforcementOrder);
        return switch (type) {
            case WARRANT -> new EnforcementFee(
                FeeType.ENFORCEMENT_WARRANT_FEE,
                enforcementOrder.getWarrantFeeAmount()
            );
            case WRIT -> new EnforcementFee(
                FeeType.ENFORCEMENT_WRIT_FEE,
                enforcementOrder.getWritFeeAmount()
            );
            case WARRANT_OF_RESTITUTION, WRIT_OF_RESTITUTION ->
                throw new IllegalArgumentException("Unimplemented enforcement type: " + type);
        };
    }

    private PartyEntity getClaimantParty(long caseReference) {
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);
        return partyService.getPrimaryClaimantPartyEntity(pcsCaseEntity);
    }

    void scheduleFeePayment(long caseReference, PartyEntity partyEntity, FeeDetails feeDetails) {
        FeesAndPayTaskData taskData = FeesAndPayTaskData.builder()
            .feeDetails(feeDetails)
            .ccdCaseNumber(String.valueOf(caseReference))
            .caseReference(caseReference)
            .responsiblePartyId(partyEntity.getId())
            .responsiblePartyName(partyService.getPartyName(partyEntity))
            .paymentCallbackHandlerType(ENFORCEMENT)
            .build();

        schedulerClient.scheduleIfNotExists(FEES_AND_PAY_TASK_DESCRIPTOR.instance(UUID.randomUUID().toString())
                                                .data(taskData).scheduledTo(Instant.now())
        );
    }

    private static @NonNull SelectEnforcementType getSelectEnforcementType(EnforcementOrder enforcementOrder) {
        String enforcementType = enforcementOrder.getChooseEnforcementType().getValueCode();
        return getSelectEnforcementTypeFromName(enforcementType);
    }

    private static String getPaymentConfirmationMarkdown(String enforcementFee, long caseReference) {
        return """
            ---
            <div class="govuk-panel govuk-panel--confirmation govuk-!-padding-top-3 govuk-!-padding-bottom-3">
            <span class="govuk-panel__title govuk-!-font-size-36">Pay %s for HM Courts and Tribunals Service to
            review your application</span>
            </div>

            <div class="govuk-body">
            We have saved your application, but you still need to pay for it before we can review it.
            This costs %s
            </div>

            <div class="govuk-body">
            We cannot review your application until you pay this fee.
            </div>

            <div class="govuk-body">
            <a href="/cases/case-details/%d#Service%%20Request"
                    class="govuk-link govuk-link--no-visited-state">Pay your application fee</a>.
            </div>
            """.formatted(enforcementFee, enforcementFee, caseReference);
    }

}
