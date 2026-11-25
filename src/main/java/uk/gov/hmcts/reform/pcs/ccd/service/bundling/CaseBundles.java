package uk.gov.hmcts.reform.pcs.ccd.service.bundling;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.doctoolkit.CaseDocStatus;
import uk.gov.hmcts.ccd.sdk.doctoolkit.DocJobs;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.bundle.CaseBundleVersionEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.CaseBundleVersionRepository;

import java.util.List;
import java.util.Optional;

/** A case's bundle as it stands: the versions made so far, and whether a newer one is on its way. */
@Service
@RequiredArgsConstructor
public class CaseBundles {

    private final ObjectProvider<DocJobs> jobs;
    private final CaseBundleVersionRepository versions;

    /** Whether bundles are made at all (ccd.doc-toolkit.jobs.enabled). */
    public boolean enabled() {
        return jobs.getIfAvailable() != null;
    }

    /** The case's bundle versions, latest first. */
    public List<CaseBundleVersionEntity> versions(PcsCaseEntity pcsCase) {
        return versions.findByCaseIdOrderByRevisionDescCreatedAtDesc(pcsCase.getId());
    }

    /** The latest version, if one has been made. */
    public Optional<CaseBundleVersionEntity> current(PcsCaseEntity pcsCase) {
        return versions(pcsCase).stream().findFirst();
    }

    /** Where the latest request is: queued, rendering, done or failed. */
    public Optional<CaseDocStatus> status(PcsCaseEntity pcsCase) {
        DocJobs docJobs = jobs.getIfAvailable();
        return docJobs == null ? Optional.empty()
            : docJobs.status(CaseBundleDefinition.NAME, pcsCase.getCaseReference());
    }
}
