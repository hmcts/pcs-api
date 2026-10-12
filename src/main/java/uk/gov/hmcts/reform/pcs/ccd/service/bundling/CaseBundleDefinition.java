package uk.gov.hmcts.reform.pcs.ccd.service.bundling;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.bundling.managed.SelectedCaseBundle;
import uk.gov.hmcts.reform.pcs.ccd.CaseType;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;

import java.util.UUID;

/** PCS selects the sources; the SDK owns the entire bundle lifecycle. */
@Component
@RequiredArgsConstructor
public class CaseBundleDefinition implements uk.gov.hmcts.ccd.sdk.bundling.managed.CaseBundleDefinition {
    public static final String NAME = "case-bundle";
    private final PcsCaseRepository cases;
    private final CaseBundleRequestFactory requests;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public SelectedCaseBundle select(String caseKey, UUID jobId) {
        var pcsCase = cases.findById(UUID.fromString(caseKey)).orElseThrow();
        return new SelectedCaseBundle(pcsCase.getCaseReference(), CaseType.getCaseType(),
            CaseType.getJurisdictionId(), requests.build(jobId, pcsCase));
    }
}
