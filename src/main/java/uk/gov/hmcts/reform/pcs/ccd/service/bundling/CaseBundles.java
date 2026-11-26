package uk.gov.hmcts.reform.pcs.ccd.service.bundling;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.doctoolkit.DocJobs;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.bundle.CaseBundleEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.CaseBundleRepository;

import java.util.List;
import java.util.Optional;

/** A case's bundle versions, as the doc toolkit handed them over. */
@Service
@RequiredArgsConstructor
public class CaseBundles {

    private final ObjectProvider<DocJobs> jobs;
    private final CaseBundleRepository versions;

    /** Whether bundles are made at all (ccd.doc-toolkit.jobs.enabled). */
    public boolean enabled() {
        return jobs.getIfAvailable() != null;
    }

    /** The case's bundle versions, latest first. */
    public List<CaseBundleEntity> versions(PcsCaseEntity pcsCase) {
        return versions.findByCaseIdOrderByCaseRevisionDesc(pcsCase.getId());
    }

    /** The latest version, if one has been made. */
    public Optional<CaseBundleEntity> current(PcsCaseEntity pcsCase) {
        return versions(pcsCase).stream().findFirst();
    }
}
