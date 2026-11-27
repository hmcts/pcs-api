package uk.gov.hmcts.reform.pcs.ccd.service.bundling;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.CaseEventContext;
import uk.gov.hmcts.ccd.sdk.CaseEventHook;
import uk.gov.hmcts.ccd.sdk.doctoolkit.DocJobs;
import uk.gov.hmcts.ccd.sdk.doctoolkit.DocSource;
import uk.gov.hmcts.ccd.sdk.doctoolkit.MissingDocuments;
import uk.gov.hmcts.ccd.sdk.doctoolkit.PageNumbers;
import uk.gov.hmcts.ccd.sdk.doctoolkit.PdfPart;
import uk.gov.hmcts.ccd.sdk.doctoolkit.PdfRequest;
import uk.gov.hmcts.ccd.sdk.doctoolkit.PdfSection;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.DocumentRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.view.DocumentsView;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The judges' case bundle, submitted to the SDK's doc toolkit after every case event; the toolkit renders it only
 * when it changed, so PCS never works out which changes affect the bundle. One section per case file category in
 * display order, each document chronological within its folder, and a placeholder page for an empty folder so a
 * reader can see it was empty rather than left out. An application's documents follow the rest of their folder, under
 * a cover sheet giving its reference.
 * Presentation is the court default (title page, contents page, folder cover sheets, "N of M" page numbers).
 */
@Component
@RequiredArgsConstructor
public class CaseBundleDefinition implements CaseEventHook {

    public static final String NAME = "case-bundle";
    /** Case file view folder for generated bundles; not a CaseFileCategory, as those drive the upload journeys. */
    public static final String FOLDER_ID = "bundles";
    public static final String FOLDER_LABEL = "Bundles";
    static final String BUNDLE_TITLE = "Case bundle";
    private static final ZoneId UK = ZoneId.of("Europe/London");

    private final PcsCaseRepository cases;
    private final DocumentRepository documents;
    private final ObjectProvider<DocJobs> jobs;

    /** Runs in the event's transaction, so the bundle is submitted only if the event commits. */
    @Override
    public void afterCaseEvent(CaseEventContext event) {
        submit(event.caseReference());
    }

    /**
     * Submits the case's bundle as it now stands, in the caller's transaction with the case locked; false when bundles
     * are off or there is no such case.
     */
    boolean submit(long caseReference) {
        DocJobs docJobs = jobs.getIfAvailable();
        if (docJobs == null) {
            return false;
        }
        // The documents are queried rather than read from the case's list: an event can save or delete a document
        // without touching a list it has already loaded.
        return cases.findByCaseReference(caseReference).map(pcsCase -> {
            docJobs.submit(caseReference, NAME, build(pcsCase, documents.findByPcsCase_Id(pcsCase.getId())));
            return true;
        }).orElse(false);
    }

    static PdfRequest build(PcsCaseEntity pcsCase, List<DocumentEntity> documents) {
        Map<CaseFileCategory, List<DocumentEntity>> byFolder = documents.stream()
            .filter(CaseBundleDefinition::isInBundle)
            .collect(Collectors.groupingBy(CaseBundleDefinition::folder));

        PdfRequest.Builder bundle = PdfRequest.builder(BUNDLE_TITLE)
            .fileName(pcsCase.getCaseReference() + "-case-bundle.pdf")
            .titlePage()
            .tableOfContents()
            .sectionCoverSheets()
            .pageNumbers(PageNumbers.BOTTOM_CENTRE_N_OF_M)
            .confidentialMarking()
            // A document that cannot be included gets a page saying so, rather than no bundle.
            .missingDocuments(MissingDocuments.PLACEHOLDER)
            .category(FOLDER_ID);
        Arrays.stream(CaseFileCategory.values())
            .sorted(Comparator.comparingInt(CaseFileCategory::getDisplayOrder))
            .forEach(category -> bundle.section(folderSection(category, byFolder.getOrDefault(category, List.of()))));
        return bundle.build();
    }

    /**
     * A folder's documents, those of an application in a section of their own after the rest, so the bundle gives
     * each application a cover sheet with its reference (GA1, GA2...), in the order the applications were made.
     */
    private static PdfSection folderSection(CaseFileCategory category, List<DocumentEntity> documents) {
        Map<Boolean, List<DocumentEntity>> ofApplication = documents.stream()
            .collect(Collectors.partitioningBy(document -> document.getGeneralApplication() != null));
        PdfSection.Builder folder = PdfSection.builder(category.getLabel())
            .documents(bundleDocuments(ofApplication.get(false)))
            .placeholderWhenEmpty();
        // Grouped in date order (an entity is equal only to itself), so applications without a rank keep that order.
        ofApplication.get(true).stream()
            .sorted(CHRONOLOGICAL)
            .collect(Collectors.groupingBy(DocumentEntity::getGeneralApplication, LinkedHashMap::new,
                                           Collectors.toList()))
            .entrySet().stream()
            .sorted(Map.Entry.comparingByKey(BY_RANK))
            .forEach(application -> folder.section(PdfSection.builder(applicationTitle(application.getKey()))
                .documents(bundleDocuments(application.getValue()))
                .build()));
        return folder.build();
    }

    private static List<PdfPart> bundleDocuments(List<DocumentEntity> documents) {
        return documents.stream().sorted(CHRONOLOGICAL).map(CaseBundleDefinition::toBundleDocument).toList();
    }

    private static final Comparator<GenAppEntity> BY_RANK =
        Comparator.comparing(GenAppEntity::getRank, Comparator.nullsLast(Comparator.naturalOrder()));

    /** As the judges' pages refer to it: "GA1: Suspend enforcement". */
    static String applicationTitle(GenAppEntity application) {
        String reference = application.getRank() == null ? "Application" : "GA" + application.getRank();
        return application.getType() == null ? reference : reference + ": " + application.getType().getShortName();
    }

    static CaseFileCategory folder(DocumentEntity document) {
        return Arrays.stream(CaseFileCategory.values())
            .filter(category -> category.getId().equals(document.getCategoryId()))
            .findFirst()
            .orElse(CaseFileCategory.UNCATEGORISED_DOCUMENTS);
    }

    static LocalDate date(DocumentEntity document) {
        if (document.getIssueDate() != null) {
            return document.getIssueDate();
        }
        return document.getSubmittedDate() == null ? null : LocalDate.ofInstant(document.getSubmittedDate(), UK);
    }

    private static final Comparator<DocumentEntity> CHRONOLOGICAL =
        Comparator.comparing(CaseBundleDefinition::date, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(DocumentEntity::getSubmittedDate, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(document -> document.getId().toString());

    // A recording is fetched like any other document and, being unsupported, becomes a missing-document page.
    // The bundle does not link to it: the stored URL is dm-store's internal one, which a judge cannot open.
    // The source carries no name, so CDAM's own file name decides the type when its content type does not.
    private static PdfPart toBundleDocument(DocumentEntity document) {
        return PdfPart.builder(DocSource.cdam(document.getDocumentId()))
            .title(title(document))
            .date(date(document))
            .build();
    }

    static boolean isInBundle(DocumentEntity document) {
        return document.getDocumentId() != null && DocumentsView.isVisibleToInternalReaders(document);
    }

    private static String title(DocumentEntity document) {
        if (StringUtils.isNotBlank(document.getFileName())) {
            return document.getFileName();
        }
        return document.getType() != null ? document.getType().getLabel() : "Document";
    }
}
