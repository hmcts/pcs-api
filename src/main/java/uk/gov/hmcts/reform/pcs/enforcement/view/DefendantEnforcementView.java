package uk.gov.hmcts.reform.pcs.enforcement.view;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.FeatureCaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.UserRoleService;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementSummary;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.service.CurrentEnforcementService;
import uk.gov.hmcts.reform.pcs.enforcement.service.DefendantEnforcementRules;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Cuts the case view down to what defendants and their legal representatives may see of enforcement
 * ({@link DefendantEnforcementRules}). Internal users, judiciary and the claimant side see everything.
 *
 * <p>CCD field access cannot do this on its own: it is fixed per role, whereas what a defendant may see depends
 * on each warrant's notice of eviction, and a defendant's legal representative holds the same
 * {@code caseworker-pcs-solicitor} access profile as the claimant's. So this runs last of the enforcement
 * views, for defendants only, and replaces the enforcement fields with a copy holding just the visible
 * warrants: their items on {@code enforcements}, and the "Enforcement status" tab rendered from them without
 * the claimant's sections. Every other enforcement field, including both "Risk assessment" tabs and any field
 * added later, is left unset for defendants. The "Enforcement status" tab's show condition then hides the tab
 * until a warrant is visible. The event flags ({@code eventAvailability}) are left unset too, so defendants are
 * offered no enforcement event.
 *
 * <p>The enforcement documents are taken out of {@code allDocuments}, the only case field that carries them,
 * except a visible warrant's notice of eviction. CCD categories cannot be restricted by role, so the
 * Enforcement folder itself is still shown to defendants, empty until a notice is served.
 */
@Component
@RequiredArgsConstructor
public class DefendantEnforcementView {

    private static final String ENFORCEMENT_CATEGORY_ID = FeatureCaseFileCategory.ENFORCEMENT.getId();

    private static final Set<String> DEFENDANT_ROLES = Stream.of(
        UserRole.DEFENDANT,
        UserRole.DEFENDANT_SOLICITOR,
        UserRole.GA_DEFENDANT_SOLICITOR
    ).map(UserRole::getRole).collect(Collectors.toUnmodifiableSet());

    private final UserRoleService userRoleService;
    private final CurrentEnforcementService currentEnforcementService;
    private final EnforcementStatusTabView enforcementStatusTabView;

    /**
     * Restricts the enforcement fields and documents when the current user is a defendant on the case.
     * Expects {@code enforcements} to hold one item per enforcement, with the enforcement's id as its id, as
     * {@link EnforcementOrderView} sets it.
     */
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
        // With no enforcement on the claim the fields hold the viewer's own draft, if any, and the event flags.
        // "Enforce the order" is offered to the claimant side then, so its flag is taken away from defendants.
        if (enforcements.isEmpty() && !hasEnforcementDocuments) {
            EnforcementOrder enforcementOrder = pcsCase.getEnforcementOrder();
            if (enforcementOrder.getEventAvailability() != null
                && isDefendant(userRoleService.getCurrentUserCaseRoles(caseReference).roles())) {
                enforcementOrder.setEventAvailability(null);
            }
            return;
        }
        if (!isDefendant(userRoleService.getCurrentUserCaseRoles(caseReference).roles())) {
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

    /**
     * The visible warrants' items on {@code enforcements} and the "Enforcement status" tab rendered from them
     * for a defendant, and nothing else; null when no warrant is visible.
     */
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

    private static boolean isDefendant(Collection<String> roles) {
        return roles != null && roles.stream().anyMatch(DEFENDANT_ROLES::contains);
    }
}
