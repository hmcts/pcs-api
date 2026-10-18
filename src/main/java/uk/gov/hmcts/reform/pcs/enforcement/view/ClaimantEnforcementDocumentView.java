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
 * Keeps the internal-only enforcement documents (the warrants and the notice of eviction, see
 * {@link EnforcementOutput#isVisibleToClaimant()}) from the claimant side. Internal users and judiciary see every
 * enforcement document; what defendants see is {@link DefendantEnforcementView}'s job.
 *
 * <p>The documents are taken out of {@code allDocuments}, the only case field that carries them.
 */
@Component
@RequiredArgsConstructor
public class ClaimantEnforcementDocumentView {

    private static final String ENFORCEMENT_CATEGORY_ID = FeatureCaseFileCategory.ENFORCEMENT.getId();

    private static final Set<String> CLAIMANT_ROLES = Stream.of(
        UserRole.CREATOR,
        UserRole.CLAIMANT_SOLICITOR,
        UserRole.GA_CLAIMANT_SOLICITOR,
        UserRole.CLAIMANT
    ).map(UserRole::getRole).collect(Collectors.toUnmodifiableSet());

    private final UserRoleService userRoleService;

    /**
     * Removes the internal-only enforcement documents from the case file when the current user is on the
     * claimant side of the case.
     */
    public void restrictForClaimants(PcsCaseEntity pcsCaseEntity, PCSCase pcsCase, long caseReference) {
        if (pcsCase == null) {
            return;
        }
        List<ListValue<Document>> allDocuments = pcsCase.getAllDocuments();
        if (allDocuments == null || allDocuments.stream().noneMatch(ClaimantEnforcementDocumentView::isEnforcement)) {
            return;
        }
        Set<String> internalOnly = internalOnlyDocumentIds(pcsCaseEntity);
        if (internalOnly.isEmpty()) {
            return;
        }
        if (!isClaimant(userRoleService.getCurrentUserCaseRoles(caseReference).roles())) {
            return;
        }
        pcsCase.setAllDocuments(allDocuments.stream()
                                    .filter(document -> !(isEnforcement(document)
                                        && internalOnly.contains(document.getId())))
                                    .toList());
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

    private static boolean isClaimant(Collection<String> roles) {
        return roles != null && roles.stream().anyMatch(CLAIMANT_ROLES::contains);
    }
}
