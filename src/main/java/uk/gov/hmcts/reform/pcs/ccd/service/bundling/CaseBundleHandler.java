package uk.gov.hmcts.reform.pcs.ccd.service.bundling;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.doctoolkit.DocJob;
import uk.gov.hmcts.ccd.sdk.doctoolkit.DocJobHandler;
import uk.gov.hmcts.ccd.sdk.doctoolkit.UploadedDoc;
import uk.gov.hmcts.reform.pcs.ccd.entity.bundle.CaseBundleEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.CaseBundleRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;

import java.util.UUID;

/**
 * Keeps each version of a case's bundle the doc toolkit renders. It runs in one transaction with the toolkit's own
 * bookkeeping, and only for a render that is still current, so versions arrive in order.
 */
@Component
@RequiredArgsConstructor
public class CaseBundleHandler implements DocJobHandler {

    private final PcsCaseRepository cases;
    private final CaseBundleRepository versions;

    @Override
    public void completed(DocJob job, UploadedDoc uploaded) {
        UUID caseId = cases.findByCaseReference(job.caseReference()).orElseThrow().getId();
        versions.save(CaseBundleEntity.builder()
            .caseId(caseId)
            .caseRevision(job.caseRevision())
            .url(uploaded.document().getUrl())
            .fileName(uploaded.document().getFilename())
            .build());
    }
}
