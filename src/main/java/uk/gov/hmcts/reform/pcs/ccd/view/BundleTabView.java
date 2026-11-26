package uk.gov.hmcts.reform.pcs.ccd.view;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialReadAccess;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.bundle.BundleTab;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.bundle.BundleVersion;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.bundle.MissingBundleDocument;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.bundle.CaseBundleVersionEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.UserRoleService;
import uk.gov.hmcts.reform.pcs.ccd.service.bundling.CaseBundleDefinition;
import uk.gov.hmcts.reform.pcs.ccd.service.bundling.CaseBundles;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Judges alone receive a bundle reference; CCD field permissions also protect direct downloads. */
@Component
@RequiredArgsConstructor
public class BundleTabView {

    static final String LATEST = "This is the latest bundle for the case.";
    static final String LATEST_WITH_MISSING = "This is the latest bundle for the case. Some documents could not be "
        + "included; each has a page in the bundle explaining why.";

    private static final DateTimeFormatter LAST_UPDATED =
        DateTimeFormatter.ofPattern("d MMMM yyyy 'at' h:mma", Locale.UK).withZone(ZoneId.of("Europe/London"));

    private final CaseBundles bundles;
    private final UserRoleService roles;
    private final FeatureToggleService features;

    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity entity) {
        if (!bundles.enabled()) {
            return;
        }
        // The judge roles are all organisational, so no case-level lookup is needed.
        var user = roles.getCurrentUserOrganisationalRoles();
        if (!JudicialReadAccess.isJudge(user.roles())
            || !features.isEnabledFor(FeatureFlag.CASE_BUNDLE_VIEW, user.userId())) {
            return;
        }
        List<CaseBundleVersionEntity> versions = bundles.versions(entity);
        if (versions.isEmpty()) {
            return;
        }
        CaseBundleVersionEntity current = versions.getFirst();
        List<ListValue<MissingBundleDocument>> missing = missingDocuments(current);
        pcsCase.setBundleTab(BundleTab.builder()
            .status(missing.isEmpty() ? LATEST : LATEST_WITH_MISSING)
            .documentId(current.getDocumentId())
            .bundle(document(current))
            .lastUpdated(lastUpdated(current.getCreatedAt()))
            .pages(current.getPageCount())
            .missingDocuments(missing.isEmpty() ? null : missing)
            .previousVersions(previous(versions))
            .build());
    }

    static Document document(CaseBundleVersionEntity version) {
        return Document.builder()
            .url(version.getUrl())
            .binaryUrl(version.getBinaryUrl())
            .filename(version.getFileName())
            .categoryId(CaseBundleDefinition.FOLDER_ID)
            .build();
    }

    static String lastUpdated(Instant at) {
        return LAST_UPDATED.format(at);
    }

    /** Earlier versions stay in the case data, so CDAM keeps serving a version a judge already has open. */
    private static List<ListValue<BundleVersion>> previous(List<CaseBundleVersionEntity> versions) {
        if (versions.size() < 2) {
            return null;
        }
        List<ListValue<BundleVersion>> previous = new ArrayList<>();
        for (CaseBundleVersionEntity version : versions.subList(1, versions.size())) {
            previous.add(ListValue.<BundleVersion>builder().id(version.getId().toString())
                .value(new BundleVersion(document(version), lastUpdated(version.getCreatedAt()),
                                         version.getPageCount()))
                .build());
        }
        return previous;
    }

    private static List<ListValue<MissingBundleDocument>> missingDocuments(CaseBundleVersionEntity version) {
        List<ListValue<MissingBundleDocument>> missing = new ArrayList<>();
        for (CaseBundleVersionEntity.MissingDocument document : version.getMissingDocuments()) {
            missing.add(ListValue.<MissingBundleDocument>builder()
                .id(document.id() != null ? document.id() : String.valueOf(missing.size() + 1))
                .value(new MissingBundleDocument(document.title(), document.folder(), document.page(),
                                                 document.reason()))
                .build());
        }
        return missing;
    }
}
