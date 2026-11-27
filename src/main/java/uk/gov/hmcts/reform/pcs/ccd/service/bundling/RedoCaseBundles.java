package uk.gov.hmcts.reform.pcs.ccd.service.bundling;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.jobs.CaseJob;
import uk.gov.hmcts.ccd.sdk.jobs.CaseJobOutcome;
import uk.gov.hmcts.ccd.sdk.jobs.CaseMetadata;

import java.util.Set;

/**
 * Submits every case's bundle again, as a case event does, so cases from before bundling, or whose bundle could not
 * be made, get one. A case whose bundle is up to date is skipped, so it gets no event. Runs in an environment when
 * CCD_JOBS_RUN_REDO_CASE_BUNDLES is set there to a run id; each run id runs once, each case in a system event.
 */
@Component
@RequiredArgsConstructor
public class RedoCaseBundles implements CaseJob {

    private final CaseBundleDefinition bundle;

    @Override
    public String name() {
        return "redo-case-bundles";
    }

    @Override
    public Set<String> caseTypes() {
        return Set.of("PCS");
    }

    @Override
    public CaseJobOutcome apply(CaseMetadata metadata) {
        return bundle.submit(metadata.reference()) ? CaseJobOutcome.applied() : CaseJobOutcome.skipped();
    }
}
