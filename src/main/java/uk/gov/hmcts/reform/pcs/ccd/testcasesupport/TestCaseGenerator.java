package uk.gov.hmcts.reform.pcs.ccd.testcasesupport;

import uk.gov.hmcts.reform.pcs.ccd.domain.State;

/**
 * Generates a test case from a test-case-generation fixture. Implementations are picked up by the
 * test support case creation event, which delegates to the first generator that handles the
 * selected fixture label.
 */
public interface TestCaseGenerator {

    /**
     * Whether this generator handles the fixture with the given label.
     */
    boolean handles(String label);

    /**
     * Generate the test case for the fixture with the given label.
     *
     * @return the state the generated case should be left in
     */
    State generate(String label, long caseReference, TestCaseGenerationContext context);

}
