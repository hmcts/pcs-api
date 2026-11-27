package uk.gov.hmcts.reform.pcs.ccd.service.bundling;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.jobs.CaseJob;
import uk.gov.hmcts.ccd.sdk.jobs.CaseJobOutcome;
import uk.gov.hmcts.ccd.sdk.jobs.CaseMetadata;

import java.util.Set;

/**
 * Submits every case's bundle again, as a case event would, so cases from before bundling, or whose bundle could not
 * be made, get one. A case whose bundle is unchanged is left as it is. Runs in an environment when
 * CASE_BUNDLE_REDO_RUN is set there to a run id; each run id runs once.
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
