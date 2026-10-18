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
import uk.gov.hmcts.reform.pcs.enforcement.document.EnforcementOutput;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Keeps the Enforcement folder of Case File View from defendants and their legal representatives, so that
 * enforcement is not put at risk, and keeps the internal-only enforcement documents (the warrants and the notice
 * of eviction, see {@link EnforcementOutput#isVisibleToClaimant()}) from the claimant side. Internal users and
 * judiciary see every enforcement document.
 *
 * <p>CCD categories cannot be restricted by role, so the folder itself is still shown, empty, to defendants.
 * Its documents are taken out of {@code allDocuments}, the only case field that carries them.
 */
@Component
@RequiredArgsConstructor
public class EnforcementDocumentVisibility {

    private static final String ENFORCEMENT_CATEGORY_ID = FeatureCaseFileCategory.ENFORCEMENT.getId();

    private static final Set<String> DEFENDANT_ROLES = roles(
        UserRole.DEFENDANT,
        UserRole.DEFENDANT_SOLICITOR,
        UserRole.GA_DEFENDANT_SOLICITOR
    );

    private static final Set<String> CLAIMANT_ROLES = roles(
        UserRole.CREATOR,
        UserRole.CLAIMANT_SOLICITOR,
        UserRole.GA_CLAIMANT_SOLICITOR,
        UserRole.CLAIMANT
    );

    private final UserRoleService userRoleService;

    /**
     * Removes the enforcement documents the current user may not see from the case file: all of them for a
     * defendant on the case, the internal-only ones for the claimant side.
     *
     * <p>TODO HDPI-7346: once the bailiff marks the notice of eviction as served, defendants should see that
     * notice, and only that notice: never the warrant or writ (HDPI-9566) or the risk assessment (HDPI-7350),
     * and nothing again once a warrant of restitution is applied for (HDPI-7539). The notice of eviction can now
     * be told apart by its document type, {@code NOTICE_OF_EVICTION}.
     */
    public void restrict(PcsCaseEntity pcsCaseEntity, PCSCase pcsCase, long caseReference) {
        List<ListValue<Document>> allDocuments = pcsCase.getAllDocuments();
        if (allDocuments == null || allDocuments.stream().noneMatch(EnforcementDocumentVisibility::isEnforcement)) {
            return;
        }

        Collection<String> roles = userRoleService.getCurrentUserCaseRoles(caseReference).roles();
        if (hasAny(roles, DEFENDANT_ROLES)) {
            pcsCase.setAllDocuments(allDocuments.stream()
                                        .filter(document -> !isEnforcement(document))
                                        .toList());
        } else if (hasAny(roles, CLAIMANT_ROLES)) {
            Set<String> internalOnly = internalOnlyDocumentIds(pcsCaseEntity);
            if (!internalOnly.isEmpty()) {
                pcsCase.setAllDocuments(allDocuments.stream()
                                            .filter(document -> !(isEnforcement(document)
                                                && internalOnly.contains(document.getId())))
                                            .toList());
            }
        }
    }

    private static Set<String> internalOnlyDocumentIds(PcsCaseEntity pcsCaseEntity) {
        if (pcsCaseEntity == null || pcsCaseEntity.getDocuments() == null) {
            return Set.of();
        }
        return pcsCaseEntity.getDocuments().stream()
            .filter(document -> ENFORCEMENT_CATEGORY_ID.equals(document.getCategoryId()))
            .filter(document -> EnforcementOutput.isInternalOnly(document.getType()))
            .filter(document -> document.getId() != null)
            .map(document -> document.getId().toString())
            .collect(Collectors.toUnmodifiableSet());
    }

    private static boolean isEnforcement(ListValue<Document> document) {
        return document != null
            && document.getValue() != null
            && ENFORCEMENT_CATEGORY_ID.equals(document.getValue().getCategoryId());
    }

    private static boolean hasAny(Collection<String> roles, Set<String> wanted) {
        return roles != null && roles.stream().anyMatch(wanted::contains);
    }

    private static Set<String> roles(UserRole... userRoles) {
        return Stream.of(userRoles).map(UserRole::getRole).collect(Collectors.toUnmodifiableSet());
    }
}
