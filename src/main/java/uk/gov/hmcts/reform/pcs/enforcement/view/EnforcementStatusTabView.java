package uk.gov.hmcts.reform.pcs.enforcement.view;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.service.CurrentEnforcementService;
import uk.gov.hmcts.reform.pcs.enforcement.service.WarrantReturnService;
import uk.gov.hmcts.reform.pcs.enforcement.view.EnforcementStatusTabMarkdown.Entry;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * Sets the "Enforcement status" tab for the case view. The tab appears once a warrant or writ has been
 * generated, which is when a caseworker approves the application (12-enforcement-status-tab.feature).
 */
@Component
@RequiredArgsConstructor
public class EnforcementStatusTabView {

    /**
     * Newest first: by when the warrant or writ was generated. One generated before approval times were
     * recorded comes last.
     */
    static final Comparator<EnforcementOrderEntity> NEWEST_FIRST = Comparator.comparing(
        EnforcementOrderEntity::getApprovedAt, Comparator.nullsFirst(Comparator.<Instant>naturalOrder())).reversed();

    private final CurrentEnforcementService currentEnforcementService;
    private final WarrantReturnService warrantReturnService;

    /**
     * Leaves the tab unset, which hides it, when the claim has no generated warrant or writ. It is cleared
     * rather than left alone, because a draft "Enforce the order" may have replaced the enforcement order on the
     * case view with an old copy.
     */
    public void setEnforcementStatusTab(PcsCaseEntity pcsCaseEntity, PCSCase pcsCase) {
        if (pcsCaseEntity == null || pcsCase == null) {
            return;
        }
        List<Entry> entries = entries(currentEnforcementService.findEnforcementsCurrentFirst(pcsCaseEntity));
        if (entries.isEmpty()) {
            if (pcsCase.getEnforcementOrder() != null) {
                pcsCase.getEnforcementOrder().setEnforcementStatusTabMarkdown(null);
            }
            return;
        }
        enforcementOrderOf(pcsCase).setEnforcementStatusTabMarkdown(EnforcementStatusTabMarkdown.render(entries));
    }

    /**
     * The tab for a defendant, rendered from the enforcements they may see only
     * ({@link uk.gov.hmcts.reform.pcs.enforcement.service.DefendantEnforcementRules}) and without the
     * claimant's sections; null, which hides the tab, when none of them has been generated.
     */
    public String defendantTabMarkdown(List<EnforcementOrderEntity> visibleToDefendants) {
        List<Entry> entries = entries(visibleToDefendants);
        return entries.isEmpty() ? null : EnforcementStatusTabMarkdown.renderForDefendants(entries);
    }

    private List<Entry> entries(List<EnforcementOrderEntity> enforcements) {
        return enforcements.stream()
            .filter(enforcement -> enforcement.getStatus() != null && enforcement.getStatus().isGenerated())
            .sorted(NEWEST_FIRST)
            .map(enforcement -> new Entry(enforcement,
                                          warrantReturnService.findLatestReturn(enforcement).orElse(null)))
            .toList();
    }

    private static EnforcementOrder enforcementOrderOf(PCSCase pcsCase) {
        if (pcsCase.getEnforcementOrder() == null) {
            pcsCase.setEnforcementOrder(new EnforcementOrder());
        }
        return pcsCase.getEnforcementOrder();
    }
}
