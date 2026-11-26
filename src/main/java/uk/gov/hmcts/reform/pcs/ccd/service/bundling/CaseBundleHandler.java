package uk.gov.hmcts.reform.pcs.ccd.service.bundling;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.doctoolkit.DocFailure;
import uk.gov.hmcts.ccd.sdk.doctoolkit.DocJob;
import uk.gov.hmcts.ccd.sdk.doctoolkit.DocJobHandler;
import uk.gov.hmcts.ccd.sdk.doctoolkit.UploadedDoc;
import uk.gov.hmcts.reform.pcs.ccd.entity.bundle.CaseBundleVersionEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.CaseBundleVersionRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;

import java.util.List;
import java.util.UUID;

/**
 * Keeps each version of a case's bundle the doc toolkit renders. It runs in one transaction with the toolkit's own
 * bookkeeping, and only for a render that is still current.
 */
@Component
@RequiredArgsConstructor
public class CaseBundleHandler implements DocJobHandler {

    private final PcsCaseRepository cases;
    private final CaseBundleVersionRepository versions;

    @Override
    public void completed(DocJob job, UploadedDoc uploaded) {
        String url = uploaded.document().getUrl();
        UUID caseId = cases.findByCaseReference(job.caseReference()).orElseThrow().getId();
        versions.save(CaseBundleVersionEntity.builder()
            .caseId(caseId)
            .revision(job.revision())
            .documentId(UUID.fromString(url.substring(url.lastIndexOf('/') + 1)))
            .url(url)
            .binaryUrl(uploaded.document().getBinaryUrl())
            .fileName(uploaded.document().getFilename())
            .pageCount(uploaded.pageCount())
            .missingDocuments(missing(uploaded.failures()))
            .build());
    }

    /** As the Bundle tab lists them: the folder is the section path below the bundle's own title. */
    static List<CaseBundleVersionEntity.MissingDocument> missing(List<DocFailure> failures) {
        return failures.stream().map(failure -> new CaseBundleVersionEntity.MissingDocument(
            failure.id().orElse(null),
            failure.title(),
            String.join(" / ", failure.sectionPath().stream().skip(1).toList()),
            failure.page().isPresent() ? failure.page().getAsInt() : null,
            failure.reason().message())).toList();
    }
}
