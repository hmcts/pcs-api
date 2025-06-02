package uk.gov.hmcts.reform.pcs.functional.tests;

import net.serenitybdd.annotations.Steps;
import net.serenitybdd.annotations.Title;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.reform.pcs.functional.config.CourtConstants;
import uk.gov.hmcts.reform.pcs.functional.config.TestConstants;
import uk.gov.hmcts.reform.pcs.functional.steps.ApiSteps;
import uk.gov.hmcts.reform.pcs.functional.testutils.DbQueryUtil;
import uk.gov.hmcts.reform.pcs.functional.testutils.RDLocation;


import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Tag("Functional")
@ExtendWith(SerenityJUnit5Extension.class)
class CourtsNameIntegrationTests {

    @Steps
    ApiSteps apiSteps;

    @BeforeEach
    void beforeEach() {
        apiSteps.setUp();
    }

    @Autowired
    private DbQueryUtil dbQueryUtil;

    @Title("Court Name Retrieval")
    @Test

    void shouldReturnExpectedCourtNameForPostcode() {

        // Step: when we query the DB
        String epimId = dbQueryUtil.getRegionByPostcode(CourtConstants.POSTCODE_VALID.replaceAll("\\s+", ""));

        // Step: then epimid should not be null
        assertNotNull(epimId);
        System.out.println("Postcode: " + CourtConstants.POSTCODE_VALID + ", ePimId: " + epimId);

        //Step: RD location service is called with epimid from DB
        List<Map<String, Object>> preparedResponse = RDLocation.getCourtName(Integer.parseInt(epimId));
        apiSteps.setUp();
        apiSteps.requestIsPreparedWithAppropriateValues();
        apiSteps.theRequestContainsValidServiceToken(TestConstants.PCS_API);
        apiSteps.theRequestContainsValidIdamToken();
        apiSteps.theRequestContainsTheQueryParameter("postcode", CourtConstants.POSTCODE_VALID);
        apiSteps.callIsSubmittedToTheEndpoint("Courts", "GET");
        apiSteps.checkStatusCode(200);
        apiSteps.theResponseBodyMatchesTheExpectedList(preparedResponse);
    }
}
