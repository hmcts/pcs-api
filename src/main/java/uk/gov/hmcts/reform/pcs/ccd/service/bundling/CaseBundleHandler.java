package uk.gov.hmcts.reform.pcs.ccd.service.bundling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * bookkeeping, which rolls it back if the version was superseded while it was being rendered.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CaseBundleHandler implements DocJobHandler {

    private final PcsCaseRepository cases;
    private final CaseBundleVersionRepository versions;

    @Override
    public void completed(DocJob job, UploadedDoc uploaded) {
        requireBundle(job);
        String url = uploaded.document().getUrl();
        UUID documentId = UUID.fromString(url.substring(url.lastIndexOf('/') + 1));
        // Called again with the same document if an earlier call failed.
        if (versions.existsByDocumentId(documentId)) {
            return;
        }
        UUID caseId = cases.findByCaseReference(job.caseReference()).orElseThrow().getId();
        versions.save(CaseBundleVersionEntity.builder()
            .caseId(caseId)
            .revision(job.revision().orElseThrow())
            .documentId(documentId)
            .url(url)
            .binaryUrl(uploaded.document().getBinaryUrl())
            .fileName(uploaded.document().getFilename())
            .pageCount(uploaded.pageCount())
            .missingDocuments(missing(uploaded.failures()))
            .build());
    }

    @Override
    public void failed(DocJob job) {
        requireBundle(job);
        // The Bundle tab reads the failure from the toolkit's status; the last good version stays.
        log.error("The case bundle for case {} at revision {} could not be made: {}", job.caseReference(),
                  job.revision().orElse(0), job.failure().map(DocJob.Failure::message).orElse(""));
    }

    private static void requireBundle(DocJob job) {
        if (!job.key().filter(CaseBundleDefinition.NAME::equals).isPresent()) {
            throw new IllegalStateException("PCS queues no doc job other than the case bundle, but was given "
                                                + job.key().orElse("a one-off job"));
        }
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
