package uk.gov.hmcts.reform.pcs.enforcement.view;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.FeatureCaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.service.UserRoleService;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Keeps the Enforcement folder of Case File View from defendants and their legal representatives, so that
 * enforcement is not put at risk. Internal users, judiciary and claimants see every enforcement document.
 *
 * <p>CCD categories cannot be restricted by role, so the folder itself is still shown, empty, to defendants.
 * Its documents are taken out of {@code allDocuments}, the only case field that carries them.
 */
@Component
@RequiredArgsConstructor
public class EnforcementDocumentVisibility {

    private static final String ENFORCEMENT_CATEGORY_ID = FeatureCaseFileCategory.ENFORCEMENT.getId();

    private static final Set<String> DEFENDANT_ROLES = Stream.of(
        UserRole.DEFENDANT,
        UserRole.DEFENDANT_SOLICITOR,
        UserRole.GA_DEFENDANT_SOLICITOR
    ).map(UserRole::getRole).collect(Collectors.toUnmodifiableSet());

    private final UserRoleService userRoleService;

    /**
     * Removes the enforcement documents from the case file when the current user is a defendant on the case.
     *
     * <p>TODO HDPI-7346: once the bailiff marks the notice of eviction as served, defendants should see that
     * notice, and only that notice: never the warrant or writ (HDPI-9566) or the risk assessment (HDPI-7350),
     * and nothing again once a warrant of restitution is applied for (HDPI-7539). That needs a notice-served
     * flag on the enforcement and a way to tell the notice of eviction apart, such as its document type.
     */
    public void hideFromDefendants(PCSCase pcsCase, long caseReference) {
        List<ListValue<Document>> allDocuments = pcsCase.getAllDocuments();
        if (allDocuments == null || allDocuments.stream().noneMatch(EnforcementDocumentVisibility::isEnforcement)) {
            return;
        }

        if (isDefendant(userRoleService.getCurrentUserCaseRoles(caseReference).roles())) {
            pcsCase.setAllDocuments(allDocuments.stream()
                                        .filter(document -> !isEnforcement(document))
                                        .toList());
        }
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
