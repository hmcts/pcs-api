package uk.gov.hmcts.reform.pcs.functional.steps;

import io.restassured.http.ContentType;
import net.serenitybdd.rest.SerenityRest;
import net.serenitybdd.annotations.Step;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public class ApiSteps {

    private String baseUrl;

    @Step("Set up base URL: {0}")
    public void setupBaseUrl(String url) {
        this.baseUrl = url;
    }

    @Step("Get user with ID {0}")
    public void getUserById(int userId) {
        SerenityRest.given()
            .baseUri(baseUrl)
            .contentType(ContentType.JSON)
            .when()
            .get("/users/{id}", userId)
            .then()
            .statusCode(200)
            .body("id", equalTo(userId))
            .body("name", notNullValue())
            .body("email", notNullValue());
    }
}
