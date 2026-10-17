package uk.gov.hmcts.reform.pcs.enforcement.view;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.repository.EnforcementOrderRepository;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.enforceTheOrder;

@Component
@RequiredArgsConstructor
public class EnforcementOrderView {

    private static final Comparator<EnforcementOrderEntity> CURRENT_FIRST =
        Comparator.comparing((EnforcementOrderEntity enforcement) -> enforcement.getStatus().isLive())
            .thenComparing(EnforcementOrderEntity::getStatusUpdatedAt,
                           Comparator.nullsFirst(Comparator.<Instant>naturalOrder()))
            .reversed();

    private final DraftCaseDataService draftCaseDataService;
    private final EnforcementOrderRepository enforcementOrderRepository;

    public void setCaseFields(PCSCase pcsCase, long caseReference, State state) {
        setHasEnforcementDraftData(caseReference, state, pcsCase);
    }

    /**
     * Shows the status of the claim's current enforcement: the live one if there is one, otherwise the one
     * whose status changed most recently. Leaves the field unset when the claim has no enforcement.
     */
    public void setEnforcementStatus(PcsCaseEntity pcsCaseEntity, PCSCase pcsCase) {
        if (pcsCaseEntity == null || pcsCase == null) {
            return;
        }
        List<ClaimEntity> claims = pcsCaseEntity.getClaims();
        if (claims == null || claims.isEmpty()) {
            return;
        }
        enforcementOrderRepository.findByClaimId(claims.getFirst().getId()).stream()
            .filter(enforcement -> enforcement.getStatus() != null)
            .min(CURRENT_FIRST)
            .map(EnforcementOrderEntity::getStatus)
            .ifPresent(status -> enforcementOrderOf(pcsCase).setEnforcementStatus(status));
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
        draftOrder.setEnforcementStatus(current.getEnforcementStatus());
        return draftOrder;
    }

    private static EnforcementOrder enforcementOrderOf(PCSCase pcsCase) {
        if (pcsCase.getEnforcementOrder() == null) {
            pcsCase.setEnforcementOrder(new EnforcementOrder());
        }
        return pcsCase.getEnforcementOrder();
    }
}
