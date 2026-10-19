package uk.gov.hmcts.reform.pcs.ccd.service.bundling;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.bundling.api.BundleDocument;
import uk.gov.hmcts.ccd.sdk.bundling.api.BundleRequest;
import uk.gov.hmcts.ccd.sdk.bundling.api.BundleSection;
import uk.gov.hmcts.ccd.sdk.bundling.api.DocumentReference;
import uk.gov.hmcts.ccd.sdk.bundling.api.EmptySectionPolicy;
import uk.gov.hmcts.ccd.sdk.bundling.api.MediaPlaceholder;
import uk.gov.hmcts.ccd.sdk.bundling.api.MissingDocumentPolicy;
import uk.gov.hmcts.ccd.sdk.bundling.managed.SelectedCaseBundle;
import uk.gov.hmcts.reform.pcs.ccd.CaseType;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppState;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimState;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The judges' case bundle. PCS selects the sources; the SDK owns the bundle lifecycle. One section
 * per case file category in display order, each document chronological within its folder, and a
 * placeholder page for an empty folder so a reader can see it was empty rather than left out.
 * Presentation is the SDK's court default (contents page, folder cover sheets, "N of M" page numbers).
 */
@Component
@RequiredArgsConstructor
public class CaseBundleDefinition implements uk.gov.hmcts.ccd.sdk.bundling.managed.CaseBundleDefinition {

    public static final String NAME = "case-bundle";
    /** Case file view folder for generated bundles; not a CaseFileCategory, as those drive the upload journeys. */
    public static final String FOLDER_ID = "bundles";
    public static final String FOLDER_LABEL = "Bundles";
    /** The document-bundling provider name for documents held in CDAM. */
    static final String CDAM_PROVIDER = "cdam";
    static final String BUNDLE_TITLE = "Case bundle";
    private static final ZoneId UK = ZoneId.of("Europe/London");

    private final PcsCaseRepository cases;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public SelectedCaseBundle select(String caseKey) {
        var pcsCase = cases.findById(UUID.fromString(caseKey)).orElseThrow();
        return new SelectedCaseBundle(pcsCase.getCaseReference(), CaseType.getCaseType(),
            CaseType.getJurisdictionId(), build(pcsCase));
    }

    static BundleRequest build(PcsCaseEntity pcsCase) {
        Map<CaseFileCategory, List<DocumentEntity>> byFolder = pcsCase.getDocuments().stream()
            .filter(CaseBundleDefinition::isVisibleToInternalReaders)
            .filter(document -> !isUnlinkableMedia(document))
            .collect(Collectors.groupingBy(CaseBundleDefinition::folder));

        BundleSection.Builder root = BundleSection.builder(BUNDLE_TITLE);
        Arrays.stream(CaseFileCategory.values())
            .sorted(Comparator.comparingInt(CaseFileCategory::getDisplayOrder))
            .forEach(category -> root.section(BundleSection.builder(category.getLabel())
                .documents(byFolder.getOrDefault(category, List.of()).stream()
                    .sorted(CHRONOLOGICAL)
                    .map(CaseBundleDefinition::toBundleDocument)
                    .toList())
                .emptySectionPolicy(EmptySectionPolicy.INCLUDE_PLACEHOLDER)
                .build()));

        return BundleRequest.builder()
            .title(BUNDLE_TITLE)
            .fileName(pcsCase.getCaseReference() + "-case-bundle.pdf")
            .root(root.build())
            // A document that cannot be included gets a page saying so, rather than no bundle.
            .missingDocuments(MissingDocumentPolicy.PLACEHOLDER)
            .build();
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

    private static BundleDocument toBundleDocument(DocumentEntity document) {
        BundleDocument.Builder builder = BundleDocument.builder()
            .id(document.getId().toString())
            .title(title(document))
            .reference(new DocumentReference(CDAM_PROVIDER, document.getDocumentId().toString()))
            .date(date(document));
        // Audio and video are never fetched: the bundle gets a page linking to the recording.
        if (isRecording(document)) {
            builder.media(MediaPlaceholder.builder()
                .mediaType(document.getContentType())
                .accessUrl(document.getBinaryUrl())
                .build());
        }
        return builder.build();
    }

    private static boolean isRecording(DocumentEntity document) {
        String contentType = document.getContentType();
        return contentType != null && (contentType.startsWith("audio/") || contentType.startsWith("video/"));
    }

    // A recording is represented by a page linking to it, which needs a link.
    private static boolean isUnlinkableMedia(DocumentEntity document) {
        return isRecording(document) && StringUtils.isBlank(document.getBinaryUrl());
    }

    // What an internal user sees in case file view: keep in step with DocumentsView.
    private static boolean isVisibleToInternalReaders(DocumentEntity document) {
        if (document.getDocumentId() == null || document.getType() == DocumentType.DEFENDANT_ACCESS_CODE) {
            return false;
        }
        GenAppEntity genApp = document.getGeneralApplication();
        if (genApp != null && genApp.getWithoutNotice() != VerticalYesNo.YES
            && genApp.getState() != GenAppState.GEN_APP_ISSUED) {
            return false;
        }
        CounterClaimEntity counterClaim = document.getCounterClaim();
        return counterClaim == null || counterClaim.getStatus() == CounterClaimState.COUNTER_CLAIM_ISSUED;
    }

    private static String title(DocumentEntity document) {
        if (StringUtils.isNotBlank(document.getFileName())) {
            return document.getFileName();
        }
        return document.getType() != null ? document.getType().getLabel() : "Document";
    }
}
