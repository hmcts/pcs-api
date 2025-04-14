package uk.gov.hmcts.reform.pcs.functional.steps;

import net.serenitybdd.rest.SerenityRest;
import net.serenitybdd.annotations.Step;
import static org.assertj.core.api.Assertions.assertThat;

public class ApiSteps {

    private String baseUrl;

    @Step("Set up base URL: {0}")
    public void setupBaseUrl(String url) {
        this.baseUrl = url;
    }

    @Step("Check Health")
    public void getHealth() {
        String status = SerenityRest.given()
            .baseUri(baseUrl)
            .when()
            .get("/health")
            .then()
            .statusCode(200)
            .extract()
            .path("status");

        assertThat(status).isEqualTo("UP");
    }
}
