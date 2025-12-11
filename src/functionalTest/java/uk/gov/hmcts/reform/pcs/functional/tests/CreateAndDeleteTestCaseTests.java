package uk.gov.hmcts.reform.pcs.functional.tests;

import net.serenitybdd.annotations.Steps;
import net.serenitybdd.annotations.Title;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.gov.hmcts.reform.pcs.functional.steps.ApiSteps;
import uk.gov.hmcts.reform.pcs.functional.steps.BaseApi;

@Tag("Functional")
@ExtendWith(SerenityJUnit5Extension.class)
class CreateAndDeleteTestCaseTests extends BaseApi {

    @Steps
    ApiSteps apiSteps;

    @Title("Create a test case and then delete it via testing-support endpoint")
    @Test
    void shouldCreateAndDeleteTestCase() throws Exception {
        // Create test case (party IDs and case reference are auto-generated)
        Long caseReference = apiSteps.createTestCase();

        // Delete the test case
        apiSteps.deleteTestCase(caseReference);
    }
}
