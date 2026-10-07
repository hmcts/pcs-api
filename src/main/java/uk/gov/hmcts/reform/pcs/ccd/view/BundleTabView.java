package uk.gov.hmcts.reform.pcs.ccd.view;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.bundling.api.BundleDocument;
import uk.gov.hmcts.ccd.sdk.bundling.api.BundleSection;
import uk.gov.hmcts.ccd.sdk.bundling.api.MissingDocument;
import uk.gov.hmcts.ccd.sdk.bundling.job.BundleJob;
import uk.gov.hmcts.ccd.sdk.bundling.job.BundleJobReport;
import uk.gov.hmcts.ccd.sdk.bundling.job.BundleJobState;
import uk.gov.hmcts.ccd.sdk.bundling.job.OutboxBundleJobService;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.bundle.BundleTab;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.bundle.MissingBundleDocument;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.bundling.CaseBundleScope;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Fills the Bundle tab: the case bundle, whether it reflects the case as it is now, and which
 * documents could not be included and why. What the bundle contains comes from the report of
 * the document-bundling job that rendered it. Nothing is set while case bundling is switched off
 * or before a bundle has been asked for, which hides the tab.
 */
@Component
@RequiredArgsConstructor
public class BundleTabView {

    static final String PREPARING = "The bundle is being prepared.";
    static final String CREATE_FAILED = "The bundle could not be created. It will be tried again when "
        + "a document is next added to the case.";
    static final String UPDATING = "An updated bundle is being prepared. The bundle below does not yet "
        + "include the most recent changes to the case.";
    static final String UPDATE_FAILED = "The bundle could not be updated with the most recent changes to "
        + "the case. It will be tried again when a document is next added to the case.";
    static final String UP_TO_DATE = "The bundle is up to date.";
    static final String UP_TO_DATE_WITH_MISSING = "The bundle is up to date, but some documents could "
        + "not be included. Each has a page in the bundle explaining why.";

    private static final DateTimeFormatter LAST_UPDATED =
        DateTimeFormatter.ofPattern("d MMMM yyyy 'at' h:mma", Locale.UK).withZone(ZoneId.of("Europe/London"));

    private final ObjectProvider<OutboxBundleJobService> bundleJobs;
    private final UploadTimestampProvider uploadTimestampProvider;

    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        OutboxBundleJobService jobs = bundleJobs.getIfAvailable();
        if (jobs == null) {
            return;
        }
        Optional<DocumentEntity> bundle = pcsCaseEntity.getDocuments().stream()
            .filter(document -> document.getType() == DocumentType.CASE_BUNDLE)
            .findFirst();
        Optional<BundleJob> latest = jobs.findLatest(CaseBundleScope.coalesceKey(pcsCaseEntity.getId()));
        if (bundle.isEmpty() && latest.isEmpty()) {
            return;
        }
        Optional<BundleJobReport> report = bundle.map(DocumentEntity::getBundleJobId).flatMap(jobs::findReport);
        List<ListValue<MissingBundleDocument>> missing = report.map(BundleTabView::missingDocuments)
            .orElse(List.of());

        pcsCase.setBundleTab(BundleTab.builder()
            .status(status(bundle, latest, !missing.isEmpty()))
            .bundle(bundle.map(this::document).orElse(null))
            .lastUpdated(bundle.map(DocumentEntity::getSubmittedDate).map(LAST_UPDATED::format).orElse(null))
            .pages(report.map(BundleJobReport::pageCount).orElse(null))
            // Left unset rather than empty, so the tab shows nothing when nothing is missing.
            .missingDocuments(missing.isEmpty() ? null : missing)
            .build());
    }

    private static String status(Optional<DocumentEntity> bundle, Optional<BundleJob> latest,
                                 boolean hasMissingDocuments) {
        boolean latestIsStored = latest.isPresent()
            && bundle.map(DocumentEntity::getBundleJobId).filter(latest.get().externalId()::equals).isPresent();
        if (latest.isPresent() && !latestIsStored) {
            BundleJobState state = latest.get().state();
            if (state == BundleJobState.QUEUED || state == BundleJobState.IN_PROGRESS) {
                return bundle.isPresent() ? UPDATING : PREPARING;
            }
            if (state == BundleJobState.FAILED) {
                return bundle.isPresent() ? UPDATE_FAILED : CREATE_FAILED;
            }
        }
        if (bundle.isEmpty()) {
            return PREPARING;
        }
        return hasMissingDocuments ? UP_TO_DATE_WITH_MISSING : UP_TO_DATE;
    }

    private Document document(DocumentEntity bundle) {
        return Document.builder()
            .url(bundle.getUrl())
            .binaryUrl(bundle.getBinaryUrl())
            .filename(bundle.getFileName())
            .categoryId(bundle.getCategoryId())
            .uploadTimestamp(uploadTimestampProvider.uploadTimestamp(bundle))
            .build();
    }

    private static List<ListValue<MissingBundleDocument>> missingDocuments(BundleJobReport report) {
        return report.missingDocuments().stream()
            .map(missing -> ListValue.<MissingBundleDocument>builder()
                .id(missing.documentId())
                .value(MissingBundleDocument.builder()
                           .documentName(title(report.request().root(), missing))
                           .folder(folder(report.request().root(), missing.documentId()))
                           .page(missing.startPage())
                           .reason(missing.reason().message())
                           .build())
                .build())
            .toList();
    }

    private static String title(BundleSection root, MissingDocument missing) {
        return root.sections().stream()
            .flatMap(section -> section.documents().stream())
            .filter(document -> document.id().equals(missing.documentId()))
            .map(BundleDocument::title)
            .findFirst()
            .orElse(missing.documentId());
    }

    private static String folder(BundleSection root, String documentId) {
        return root.sections().stream()
            .filter(section -> section.documents().stream().map(BundleDocument::id)
                .anyMatch(Objects.requireNonNull(documentId)::equals))
            .map(BundleSection::title)
            .findFirst()
            .orElse(null);
    }
}
