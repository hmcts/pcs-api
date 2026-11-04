package uk.gov.hmcts.reform.pcs.enforcement.view;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.FeatureCaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.enforcement.accesscontrol.EnforcementViewerService;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementSummary;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.service.CurrentEnforcementService;
import uk.gov.hmcts.reform.pcs.enforcement.service.DefendantEnforcementRules;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Cuts the case view down to what defendants may see of enforcement ({@link DefendantEnforcementRules}).
 * Runs last of the enforcement views, and replaces the enforcement fields rather than filtering them, so any
 * field added later is left unset for defendants.
 */
@Component
@RequiredArgsConstructor
public class DefendantEnforcementView {

    private static final String ENFORCEMENT_CATEGORY_ID = FeatureCaseFileCategory.ENFORCEMENT.getId();

    private final EnforcementViewerService enforcementViewerService;
    private final CurrentEnforcementService currentEnforcementService;
    private final EnforcementStatusTabView enforcementStatusTabView;

    public void restrictForDefendants(PcsCaseEntity pcsCaseEntity, PCSCase pcsCase, long caseReference) {
        if (pcsCase == null) {
            return;
        }
        boolean hasEnforcementDocuments = hasEnforcementDocuments(pcsCase.getAllDocuments());
        if (pcsCase.getEnforcementOrder() == null && !hasEnforcementDocuments) {
            return;
        }
        List<EnforcementOrderEntity> enforcements = currentEnforcementService.findEnforcementsCurrentFirst(
            pcsCaseEntity);
        if (enforcements.isEmpty() && !hasEnforcementDocuments) {
            EnforcementOrder enforcementOrder = pcsCase.getEnforcementOrder();
            if (enforcementOrder.getEventAvailability() != null
                && !enforcementViewerService.isInternalWithoutCaseRoles()
                && enforcementViewerService.currentViewer(pcsCaseEntity, pcsCase, caseReference).isRestricted()) {
                enforcementOrder.setEventAvailability(null);
            }
            return;
        }
        if (!enforcementViewerService.currentViewer(pcsCaseEntity, pcsCase, caseReference).isRestricted()) {
            return;
        }

        pcsCase.setEnforcementOrder(defendantView(pcsCase.getEnforcementOrder(), enforcements));
        if (hasEnforcementDocuments) {
            Set<String> visibleDocumentIds = DefendantEnforcementRules.documentIdsVisibleToDefendants(enforcements)
                .stream()
                .map(UUID::toString)
                .collect(Collectors.toUnmodifiableSet());
            pcsCase.setAllDocuments(pcsCase.getAllDocuments().stream()
                                        .filter(document -> !isEnforcement(document)
                                            || visibleDocumentIds.contains(document.getId()))
                                        .toList());
        }
    }

    private EnforcementOrder defendantView(EnforcementOrder enforcementOrder,
                                           List<EnforcementOrderEntity> enforcements) {
        List<EnforcementOrderEntity> visibleEnforcements = DefendantEnforcementRules.visibleToDefendants(enforcements);
        if (visibleEnforcements.isEmpty()) {
            return null;
        }
        Set<String> visibleIds = visibleEnforcements.stream()
            .map(EnforcementOrderEntity::getId)
            .filter(Objects::nonNull)
            .map(UUID::toString)
            .collect(Collectors.toUnmodifiableSet());
        List<ListValue<EnforcementSummary>> summaries =
            enforcementOrder == null || enforcementOrder.getEnforcements() == null
                ? List.of()
                : enforcementOrder.getEnforcements();
        List<ListValue<EnforcementSummary>> visible = summaries.stream()
            .filter(item -> item != null && visibleIds.contains(item.getId()))
            .toList();
        String statusTab = enforcementStatusTabView.defendantTabMarkdown(visibleEnforcements);
        if (visible.isEmpty() && statusTab == null) {
            return null;
        }
        EnforcementOrder defendantView = new EnforcementOrder();
        defendantView.setEnforcements(visible.isEmpty() ? null : visible);
        defendantView.setEnforcementStatusTabMarkdown(statusTab);
        return defendantView;
    }

    private static boolean hasEnforcementDocuments(List<ListValue<Document>> allDocuments) {
        return allDocuments != null && allDocuments.stream().anyMatch(DefendantEnforcementView::isEnforcement);
    }

    private static boolean isEnforcement(ListValue<Document> document) {
        return document != null
            && document.getValue() != null
            && ENFORCEMENT_CATEGORY_ID.equals(document.getValue().getCategoryId());
    }
}
