package uk.gov.hmcts.reform.pcs.enforcement.view;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;

import java.util.Optional;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.enforceTheOrder;

@Component
@RequiredArgsConstructor
public class EnforcementOrderView {

    private final DraftCaseDataService draftCaseDataService;

    public void setCaseFields(PCSCase pcsCase, long caseReference, State state) {
        setHasEnforcementDraftData(caseReference, state, pcsCase);
    }

    private void setHasEnforcementDraftData(long caseReference, State state, PCSCase pcsCase) {
        if (State.CASE_ISSUED == state) {
            boolean hasUnsubmittedCaseData = draftCaseDataService
                .hasUnsubmittedCaseData(caseReference, enforceTheOrder);
            if (hasUnsubmittedCaseData) {
                Optional<PCSCase> unsubmittedCaseData = draftCaseDataService
                    .getUnsubmittedCaseData(caseReference, enforceTheOrder);
                unsubmittedCaseData.ifPresent(inflated -> pcsCase.setEnforcementOrder(
                    keepViewFields(inflated.getEnforcementOrder(), pcsCase.getEnforcementOrder())));
                enforcementOrderOf(pcsCase).setHasUnsubmittedEnforcementData(YesOrNo.YES);
            } else {
                enforcementOrderOf(pcsCase).setHasUnsubmittedEnforcementData(YesOrNo.NO);
            }
        }
    }

    /**
     * The draft replaces the case's enforcement order, but the fields below are derived for the
     * case view (or only set during an event) rather than taken from the draft, so keep the
     * current values. A draft with no enforcement order keeps the current order.
     */
    private static EnforcementOrder keepViewFields(EnforcementOrder draftOrder, EnforcementOrder currentOrder) {
        if (draftOrder == null) {
            return currentOrder;
        }
        EnforcementOrder current = currentOrder != null ? currentOrder : new EnforcementOrder();
        draftOrder.setConfirmEvictionSummaryMarkup(current.getConfirmEvictionSummaryMarkup());
        draftOrder.setShowConfirmEvictionJourney(current.getShowConfirmEvictionJourney());
        draftOrder.setFormattedDefendantNames(current.getFormattedDefendantNames());
        draftOrder.setFormattedPropertyAddress(current.getFormattedPropertyAddress());
        return draftOrder;
    }

    private static EnforcementOrder enforcementOrderOf(PCSCase pcsCase) {
        if (pcsCase.getEnforcementOrder() == null) {
            pcsCase.setEnforcementOrder(new EnforcementOrder());
        }
        return pcsCase.getEnforcementOrder();
    }
}
