package uk.gov.hmcts.reform.pcs.ccd.view;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.BundleReadAccess;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialReadAccess;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.bundle.CaseBundleEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.CaseBundleRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.UserRoleService;
import uk.gov.hmcts.reform.pcs.ccd.service.bundling.CaseBundleDefinition;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Projects the bundle documents for authorised Case File View users. */
@Component
@RequiredArgsConstructor
public class CaseBundlesView {

    private final CaseBundleRepository bundles;
    private final UserRoleService roles;
    private final FeatureToggleService features;

    /**
     * How long a version stays readable once a newer one replaces it, so a judge already reading or linking it is not
     * cut off, while pages removed from the case stop being served a day later.
     */
    static final Duration REPLACED_VERSION_READABLE_FOR = Duration.ofHours(24);
    /** As DocumentNameService dates a document's name, with the time, as a case can have several bundles a day. */
    private static final DateTimeFormatter MADE_AT = DateTimeFormatter.ofPattern("ddMMyyyy HHmm")
        .withZone(ZoneId.of("Europe/London"));

    public boolean shownInMakeOrder() {
        var user = roles.getCurrentUserOrganisationalRoles();
        return JudicialReadAccess.isJudge(user.roles())
            && features.isEnabledFor(FeatureFlag.CASE_BUNDLE_VIEW, user.userId());
    }

    /** The newest version, and any replaced within the last day, newest first. */
    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity entity) {
        var user = roles.getCurrentUserOrganisationalRoles();
        if (!BundleReadAccess.canRead(user.roles())
            || !features.isEnabledFor(FeatureFlag.CASE_BUNDLE_VIEW, user.userId())) {
            return;
        }
        Instant replacedSince = Instant.now().minus(REPLACED_VERSION_READABLE_FOR);
        List<CaseBundleEntity> versions = bundles.findByCaseIdOrderByCaseRevisionDesc(entity.getId());
        List<ListValue<Document>> readable = new ArrayList<>();
        for (int i = 0; i < versions.size(); i++) {
            // A version is replaced when the next newer one was made.
            if (i == 0 || versions.get(i - 1).getCreatedAt().isAfter(replacedSince)) {
                readable.add(listed(versions.get(i), i > 0));
            }
        }
        if (!readable.isEmpty()) {
            pcsCase.setCaseBundles(readable);
        }
    }

    /** Named for Case File View: when it was made, and whether a newer one has replaced it. */
    private static ListValue<Document> listed(CaseBundleEntity version, boolean replaced) {
        String name = "Case bundle " + MADE_AT.format(version.getCreatedAt())
            + (replaced ? " (replaced)" : "") + ".pdf";
        return ListValue.<Document>builder().id(version.getId().toString())
            .value(Document.builder()
                       .url(version.getUrl())
                       .binaryUrl(version.getBinaryUrl())
                       .filename(name)
                       .categoryId(CaseBundleDefinition.FOLDER_ID)
                       .build())
            .build();
    }
}
