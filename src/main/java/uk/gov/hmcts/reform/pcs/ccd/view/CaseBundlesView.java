package uk.gov.hmcts.reform.pcs.ccd.view;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialReadAccess;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.CaseBundleRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.UserRoleService;
import uk.gov.hmcts.reform.pcs.ccd.service.bundling.CaseBundleDefinition;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

/** Judges alone receive the case's bundles; CCD field permissions also protect direct downloads. */
@Component
@RequiredArgsConstructor
public class CaseBundlesView {

    private final CaseBundleRepository bundles;
    private final UserRoleService roles;
    private final FeatureToggleService features;

    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity entity) {
        // The judge roles are all organisational, so no case-level lookup is needed.
        var user = roles.getCurrentUserOrganisationalRoles();
        if (!JudicialReadAccess.isJudge(user.roles())
            || !features.isEnabledFor(FeatureFlag.CASE_BUNDLE_VIEW, user.userId())) {
            return;
        }
        var versions = bundles.findByCaseIdOrderByCaseRevisionDesc(entity.getId()).stream()
            .map(version -> ListValue.<Document>builder().id(version.getId().toString())
                .value(Document.builder()
                           .url(version.getUrl())
                           .binaryUrl(version.getBinaryUrl())
                           .filename(version.getFileName())
                           .categoryId(CaseBundleDefinition.FOLDER_ID)
                           .build())
                .build())
            .toList();
        if (!versions.isEmpty()) {
            pcsCase.setCaseBundles(versions);
        }
    }
}
