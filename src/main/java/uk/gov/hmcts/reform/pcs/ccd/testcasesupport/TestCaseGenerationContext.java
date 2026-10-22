package uk.gov.hmcts.reform.pcs.ccd.testcasesupport;

import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

/**
 * Core test case support operations available to a {@link TestCaseGenerator}.
 */
public interface TestCaseGenerationContext {

    /**
     * Load and parse the test-case-generation fixture with the given label.
     */
    PCSCase loadTestPcsCase(String label);

    /**
     * Create a case and submit a claim for it from the fixture with the given label.
     */
    void makeAClaimTestCreation(String label, Long caseReference);

}
