package uk.gov.hmcts.reform.pcs.functional.steps;

import net.serenitybdd.rest.SerenityRest;
import net.serenitybdd.annotations.Step;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGeneratorFactory;

import static org.hamcrest.Matchers.equalTo;

@SpringBootTest
public class ApiSteps {

    private String baseUrl;

    @Autowired
    private ServiceAuthorisationApi serviceAuthorisationApi;

    @Value("${idam.s2s-auth.totp_secret}")
    private String totpSecret;

    private AuthTokenGenerator s2sAuthTokenGenerator;

    @Step("Generate service token")
    public String createServiceToken() {
        s2sAuthTokenGenerator = AuthTokenGeneratorFactory.createDefaultGenerator(
            totpSecret,
            "civil_service",
            serviceAuthorisationApi
        );

        String token = s2sAuthTokenGenerator.generate();

        System.out.println("Generated Service Token: " + token);

        return token;
    }

    @Step("Set up base URL: {0}")
    public void setupBaseUrl(String url) {
        this.baseUrl = url;
    }

    @Step("Check Health")
    public void getHealth() {
        SerenityRest.given()
            .baseUri(baseUrl)
            .when()
            .get("/health");
    }

    @Step("Check status code: {0}")
    public void checkStatusCode(int statusCode) {
        SerenityRest.then().statusCode(statusCode);
    }

    @Step("Check status is: {0}")
    public void checkStatus(String status) {
        SerenityRest.then().body("status", equalTo(status));
    }
}
