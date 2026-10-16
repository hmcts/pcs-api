package uk.gov.hmcts.reform.pcs.enforcement.testcasesupport;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.testcasesupport.TestCaseGenerationContext;
import uk.gov.hmcts.reform.pcs.ccd.testcasesupport.TestCaseGenerator;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.event.EnforceTheOrder;

import static uk.gov.hmcts.reform.pcs.ccd.domain.State.CASE_ISSUED;

/**
 * Generates enforcement test cases from the Create-Enforcement-* test-case-generation fixtures: creates
 * a basic claim case, then submits the enforcement order from the selected fixture against it.
 */
@Component
@RequiredArgsConstructor
public class EnforcementTestCaseGenerator implements TestCaseGenerator {

    public static final String ENFORCEMENT_CASE_GENERATOR = "Create Enforcement";
    static final String BASE_CLAIM_FIXTURE = "Create-Case-Make-A-Claim-Basic-Case";

    private final EnforceTheOrder enforceTheOrder;

    @Override
    public boolean handles(String label) {
        return label.startsWith(ENFORCEMENT_CASE_GENERATOR);
    }

    @Override
    public State generate(String label, long caseReference, TestCaseGenerationContext context) {
        context.makeAClaimTestCreation(BASE_CLAIM_FIXTURE, caseReference);
        enforceTheOrder.submit(new EventPayload<>(caseReference, context.loadTestPcsCase(label), null));
        return CASE_ISSUED;
    }

}
