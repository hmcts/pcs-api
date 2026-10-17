package uk.gov.hmcts.reform.pcs.enforcement.view;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementSummary;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.service.CurrentEnforcementService;

import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.enforceTheOrder;
import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;

@Component
@RequiredArgsConstructor
public class EnforcementOrderView {

    private static final Set<SelectEnforcementType> WARRANTS =
        EnumSet.of(SelectEnforcementType.WARRANT, SelectEnforcementType.WARRANT_OF_RESTITUTION);

    private static final Set<SelectEnforcementType> WRITS =
        EnumSet.of(SelectEnforcementType.WRIT, SelectEnforcementType.WRIT_OF_RESTITUTION);

    private final DraftCaseDataService draftCaseDataService;
    private final CurrentEnforcementService currentEnforcementService;

    public void setCaseFields(PCSCase pcsCase, long caseReference, State state) {
        setHasEnforcementDraftData(caseReference, state, pcsCase);
    }

    /**
     * Sets the status of the claim's current enforcement (the live one if there is one, otherwise the one
     * whose status changed most recently) and the list of every enforcement on the claim, newest first.
     * Leaves both unset when the claim has no enforcement.
     */
    public void setEnforcementStatusFields(PcsCaseEntity pcsCaseEntity, PCSCase pcsCase) {
        if (pcsCase == null) {
            return;
        }
        List<EnforcementOrderEntity> enforcements =
            currentEnforcementService.findEnforcementsCurrentFirst(pcsCaseEntity);
        if (enforcements.isEmpty()) {
            return;
        }
        EnforcementOrder enforcementOrder = enforcementOrderOf(pcsCase);
        enforcementOrder.setEnforcementStatus(enforcements.getFirst().getStatus());
        enforcementOrder.setEnforcements(enforcements.stream()
                                             .map(EnforcementOrderView::toSummaryListValue)
                                             .toList());
    }

    private static ListValue<EnforcementSummary> toSummaryListValue(EnforcementOrderEntity enforcement) {
        SelectEnforcementType type = SelectEnforcementType.from(enforcement.getEnforcementOrder());
        EnforcementSummary summary = EnforcementSummary.builder()
            .enforcementType(type)
            .enforcementNumber(enforcement.getEnforcementNumber())
            .status(enforcement.getStatus())
            .statusUpdatedDate(toUkDate(enforcement.getStatusUpdatedAt()))
            .scheduledEvictionDate(WARRANTS.contains(type) ? enforcement.getBailiffDate() : null)
            .highCourtTransferDate(WRITS.contains(type) ? toUkDate(enforcement.getApprovedAt()) : null)
            .build();
        return ListValue.<EnforcementSummary>builder()
            .id(enforcement.getId() != null ? enforcement.getId().toString() : null)
            .value(summary)
            .build();
    }

    private static LocalDate toUkDate(Instant instant) {
        return instant != null ? instant.atZone(UK_ZONE_ID).toLocalDate() : null;
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
     * The draft replaces the case's enforcement order, but the fields below are derived for the case view
     * rather than taken from the draft, so keep the current values. A draft with no enforcement order keeps
     * the current order.
     *
     * <p>The formatted property address and defendant names are the other way round: only enforceTheOrder's
     * start callback and the first page's mid event write them, so the draft holds the real values and the
     * case view has none. Keeping the draft's values is what makes them survive into
     * resumeEnforcementOrder, which has no start callback to work them out again.</p>
     */
    private static EnforcementOrder keepViewFields(EnforcementOrder draftOrder, EnforcementOrder currentOrder) {
        if (draftOrder == null) {
            return currentOrder;
        }
        EnforcementOrder current = currentOrder != null ? currentOrder : new EnforcementOrder();
        draftOrder.setConfirmEvictionSummaryMarkup(current.getConfirmEvictionSummaryMarkup());
        draftOrder.setShowConfirmEvictionJourney(current.getShowConfirmEvictionJourney());
        draftOrder.setFormattedEvictionDate(current.getFormattedEvictionDate());
        draftOrder.setEnforcementStatus(current.getEnforcementStatus());
        draftOrder.setEnforcements(current.getEnforcements());
        if (draftOrder.getFormattedDefendantNames() == null) {
            draftOrder.setFormattedDefendantNames(current.getFormattedDefendantNames());
        }
        if (draftOrder.getFormattedPropertyAddress() == null) {
            draftOrder.setFormattedPropertyAddress(current.getFormattedPropertyAddress());
        }
        return draftOrder;
    }

    private static EnforcementOrder enforcementOrderOf(PCSCase pcsCase) {
        if (pcsCase.getEnforcementOrder() == null) {
            pcsCase.setEnforcementOrder(new EnforcementOrder());
        }
        return pcsCase.getEnforcementOrder();
    }
}
