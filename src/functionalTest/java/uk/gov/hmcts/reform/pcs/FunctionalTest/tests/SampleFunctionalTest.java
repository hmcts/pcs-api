package uk.gov.hmcts.reform.pcs.FunctionalTest.tests;

import net.serenitybdd.junit5.SerenityJUnit5Extension;
import net.serenitybdd.annotations.Steps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.gov.hmcts.reform.pcs.FunctionalTest.steps.ApiSteps;

@ExtendWith(SerenityJUnit5Extension.class)
class SampleFunctionalTest {

    private static final String BASE_URL = "https://jsonplaceholder.typicode.com";

    @Steps
    ApiSteps apiSteps;

    @BeforeEach
    void setUp() {
        apiSteps.setupBaseUrl(BASE_URL);
    }

    @Test
    void testGetUser() {
        apiSteps.getUserById(1);
    }

    @Test
    @Tag("Functional")
    void testGetUser2() {
        apiSteps.getUserById(1);
    }
}
