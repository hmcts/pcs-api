package uk.gov.hmcts.reform.pcs.ccd.view;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.bundling.managed.CaseBundles;
import uk.gov.hmcts.ccd.sdk.bundling.managed.CaseBundleView;
import uk.gov.hmcts.ccd.sdk.bundling.api.BundleDocument;
import uk.gov.hmcts.ccd.sdk.bundling.job.BundleJobReport;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudgeBundleAccess;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.bundle.BundleTab;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.bundle.MissingBundleDocument;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.UserRoleService;
import uk.gov.hmcts.reform.pcs.ccd.service.bundling.CaseBundleDefinition;
import uk.gov.hmcts.reform.pcs.ccd.service.bundling.CaseBundleScope;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** Judges alone receive a bundle reference; CCD field permissions also protect direct downloads. */
@Component
@RequiredArgsConstructor
public class BundleTabView {

    static final String PREPARING = "The bundle is being prepared.";
    static final String CREATE_FAILED = "The bundle could not be created. It will be tried again when "
        + "the case documents next change.";
    static final String UPDATING = "An updated bundle is being prepared. The bundle below does not yet "
        + "include the most recent changes to the case.";
    static final String UPDATE_FAILED = "The bundle could not be updated with the most recent changes to "
        + "the case. It will be tried again when the case documents next change.";
    static final String UP_TO_DATE = "The bundle is up to date.";
    static final String UP_TO_DATE_WITH_MISSING = "The bundle is up to date, but some documents could "
        + "not be included. Each has a page in the bundle explaining why.";

    private static final DateTimeFormatter LAST_UPDATED =
        DateTimeFormatter.ofPattern("d MMMM yyyy 'at' h:mma", Locale.UK).withZone(ZoneId.of("Europe/London"));

    private final ObjectProvider<CaseBundles> bundles;
    private final UserRoleService roles;

    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity entity) {
        CaseBundles managed = bundles.getIfAvailable();
        if (managed == null || !JudgeBundleAccess.isJudge(roles.getCurrentUserCaseRoles(
            entity.getCaseReference()).roles())) {
            return;
        }
        CaseBundleView view = managed.view(CaseBundleDefinition.NAME, entity.getId().toString());
        if (view.lifecycle() == CaseBundleView.Lifecycle.NONE) {
            return;
        }
        var publication = view.current();
        List<ListValue<MissingBundleDocument>> missing = publication
            .map(current -> missingDocuments(current.report())).orElse(List.of());
        String status = switch (view.lifecycle()) {
            case PREPARING, NONE -> PREPARING;
            case UPDATING -> UPDATING;
            case FAILED -> publication.isPresent() ? UPDATE_FAILED : CREATE_FAILED;
            case UP_TO_DATE -> missing.isEmpty() ? UP_TO_DATE : UP_TO_DATE_WITH_MISSING;
        };
        pcsCase.setBundleTab(BundleTab.builder().status(status)
            .bundle(publication.map(current -> {
                var document = current.document();
                document.setCategoryId(CaseBundleScope.BUNDLES_CATEGORY_ID);
                return document;
            }).orElse(null))
            .lastUpdated(publication.map(current -> LAST_UPDATED.format(current.publishedAt())).orElse(null))
            .pages(publication.map(current -> current.report().pageCount()).orElse(null))
            .missingDocuments(missing.isEmpty() ? null : missing).build());
    }

    private static List<ListValue<MissingBundleDocument>> missingDocuments(BundleJobReport report) {
        return report.missingDocuments().stream().map(missing -> {
            var section = report.request().root().sections().stream()
                .filter(folder -> folder.documents().stream()
                    .anyMatch(document -> document.id().equals(missing.documentId())))
                .findFirst();
            String title = section.stream().flatMap(folder -> folder.documents().stream())
                .filter(document -> document.id().equals(missing.documentId()))
                .map(BundleDocument::title).findFirst().orElse(missing.documentId());
            return ListValue.<MissingBundleDocument>builder().id(missing.documentId())
                .value(new MissingBundleDocument(title, section.map(folder -> folder.title()).orElse(null),
                    missing.startPage(), missing.reason().message())).build();
        }).toList();
    }
}
