package uk.gov.hmcts.reform.pcs.ccd.service.bundling;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.bundling.api.BundleDocument;
import uk.gov.hmcts.ccd.sdk.bundling.api.BundlePresentation;
import uk.gov.hmcts.ccd.sdk.bundling.api.BundleRequest;
import uk.gov.hmcts.ccd.sdk.bundling.api.BundleSection;
import uk.gov.hmcts.ccd.sdk.bundling.api.DocumentReference;
import uk.gov.hmcts.ccd.sdk.bundling.api.EmptySectionPolicy;
import uk.gov.hmcts.ccd.sdk.bundling.api.MediaPlaceholder;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Turns a case's documents into a bundle request: one section per case file category in display
 * order, each document chronological within its folder, and a placeholder page for an empty
 * folder so a reader can see it was empty rather than left out. Presentation is the SDK's court
 * default (contents page, folder cover sheets, "N of M" page numbers, no watermark).
 */
@Component
public class CaseBundleRequestFactory {

    static final String BUNDLE_TITLE = "Case bundle";
    private static final ZoneId UK = ZoneId.of("Europe/London");

    public BundleRequest build(UUID externalId, PcsCaseEntity pcsCase) {
        Map<CaseFileCategory, List<DocumentEntity>> byFolder = pcsCase.getDocuments().stream()
            .filter(CaseBundleScope::isVisibleToInternalReaders)
            .filter(document -> !isUnlinkableMedia(document))
            .collect(Collectors.groupingBy(CaseBundleRequestFactory::folder));

        BundleSection.Builder root = BundleSection.builder(BUNDLE_TITLE);
        Arrays.stream(CaseFileCategory.values())
            .sorted(Comparator.comparingInt(CaseFileCategory::getDisplayOrder))
            .forEach(category -> root.section(BundleSection.builder(category.getLabel())
                .documents(byFolder.getOrDefault(category, List.of()).stream()
                    .sorted(CHRONOLOGICAL)
                    .map(CaseBundleRequestFactory::toBundleDocument)
                    .toList())
                .emptySectionPolicy(EmptySectionPolicy.INCLUDE_PLACEHOLDER)
                .build()));

        return BundleRequest.builder()
            .externalId(externalId)
            .title(BUNDLE_TITLE)
            .fileName(pcsCase.getCaseReference() + "-case-bundle.pdf")
            .root(root.build())
            .presentation(BundlePresentation.courtDefault())
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
        Comparator.comparing(CaseBundleRequestFactory::date, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(DocumentEntity::getSubmittedDate, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(document -> document.getId().toString());

    private static BundleDocument toBundleDocument(DocumentEntity document) {
        BundleDocument.Builder builder = BundleDocument.builder()
            .id(document.getId().toString())
            .title(title(document))
            .reference(new DocumentReference(CaseBundleScope.CDAM_PROVIDER, document.getDocumentId().toString()));
        LocalDate date = date(document);
        if (date != null) {
            builder.date(date);
        }
        // Audio and video are never fetched: the bundle gets a page linking to the recording.
        String contentType = document.getContentType();
        if (contentType != null && (contentType.startsWith("audio/") || contentType.startsWith("video/"))) {
            builder.media(MediaPlaceholder.builder()
                .mediaType(contentType)
                .accessUrl(document.getBinaryUrl())
                .build());
        }
        return builder.build();
    }

    // A recording is represented by a page linking to it, which needs a link.
    private static boolean isUnlinkableMedia(DocumentEntity document) {
        String contentType = document.getContentType();
        return contentType != null && (contentType.startsWith("audio/") || contentType.startsWith("video/"))
            && StringUtils.isBlank(document.getBinaryUrl());
    }

    private static String title(DocumentEntity document) {
        if (StringUtils.isNotBlank(document.getFileName())) {
            return document.getFileName();
        }
        return document.getType() != null ? document.getType().getLabel() : "Document";
    }
}
