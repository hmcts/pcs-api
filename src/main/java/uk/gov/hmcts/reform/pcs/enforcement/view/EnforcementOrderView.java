package uk.gov.hmcts.reform.pcs.enforcement.view;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementSummary;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.repository.EnforcementOrderRepository;

import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.enforceTheOrder;
import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;

@Component
@RequiredArgsConstructor
public class EnforcementOrderView {

    /**
     * The live enforcement first (a claim has at most one, and it is always the newest), then the rest by when
     * their status last changed, most recent first. Rows with no status change time come last: they predate
     * the status model.
     */
    private static final Comparator<EnforcementOrderEntity> CURRENT_FIRST =
        Comparator.comparing((EnforcementOrderEntity enforcement) -> enforcement.getStatus().isLive())
            .thenComparing(EnforcementOrderEntity::getStatusUpdatedAt,
                           Comparator.nullsFirst(Comparator.<Instant>naturalOrder()))
            .reversed();

    private static final Set<SelectEnforcementType> WARRANTS =
        EnumSet.of(SelectEnforcementType.WARRANT, SelectEnforcementType.WARRANT_OF_RESTITUTION);

    private final DraftCaseDataService draftCaseDataService;
    private final EnforcementOrderRepository enforcementOrderRepository;

    public void setCaseFields(PCSCase pcsCase, long caseReference, State state) {
        setHasEnforcementDraftData(caseReference, state, pcsCase);
    }

    /**
     * Sets the status of the claim's current enforcement (the live one if there is one, otherwise the one
     * whose status changed most recently) and the list of every enforcement on the claim, newest first.
     * Leaves both unset when the claim has no enforcement.
     */
    public void setEnforcementStatusFields(PcsCaseEntity pcsCaseEntity, PCSCase pcsCase) {
        if (pcsCaseEntity == null || pcsCase == null) {
            return;
        }
        List<ClaimEntity> claims = pcsCaseEntity.getClaims();
        if (claims == null || claims.isEmpty()) {
            return;
        }
        List<EnforcementOrderEntity> enforcements = enforcementOrderRepository
            .findByClaimId(claims.getFirst().getId()).stream()
            .filter(enforcement -> enforcement.getStatus() != null)
            .sorted(CURRENT_FIRST)
            .toList();
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
        SelectEnforcementType type = enforcementTypeOf(enforcement.getEnforcementOrder());
        EnforcementSummary summary = EnforcementSummary.builder()
            .enforcementType(type)
            .status(enforcement.getStatus())
            .statusUpdatedDate(Optional.ofNullable(enforcement.getStatusUpdatedAt())
                                   .map(updatedAt -> updatedAt.atZone(UK_ZONE_ID).toLocalDate())
                                   .orElse(null))
            .scheduledEvictionDate(WARRANTS.contains(type) ? enforcement.getBailiffDate() : null)
            .build();
        return ListValue.<EnforcementSummary>builder()
            .id(enforcement.getId() != null ? enforcement.getId().toString() : null)
            .value(summary)
            .build();
    }

    /**
     * The type chosen in "Enforce the order", or null if the stored application does not record a known one.
     */
    private static SelectEnforcementType enforcementTypeOf(EnforcementOrder enforcementOrder) {
        if (enforcementOrder == null) {
            return null;
        }
        String code = Optional.ofNullable(enforcementOrder.getChooseEnforcementType())
            .map(DynamicStringList::getValueCode)
            .orElse(null);
        if (code == null) {
            return enforcementOrder.getSelectEnforcementType();
        }
        return Arrays.stream(SelectEnforcementType.values())
            .filter(type -> type.name().equals(code))
            .findFirst()
            .orElse(null);
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
        draftOrder.setEnforcements(current.getEnforcements());
        return draftOrder;
    }

    private static EnforcementOrder enforcementOrderOf(PCSCase pcsCase) {
        if (pcsCase.getEnforcementOrder() == null) {
            pcsCase.setEnforcementOrder(new EnforcementOrder());
        }
        return pcsCase.getEnforcementOrder();
    }
}
