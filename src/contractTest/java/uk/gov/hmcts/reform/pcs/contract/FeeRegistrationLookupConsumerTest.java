package uk.gov.hmcts.reform.pcs.contract;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactBuilder;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpHeaders;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "feeRegister_lookUp", port = "8080")
public class FeeRegistrationLookupConsumerTest {

    private static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";
    private static final String SERVICE_TOKEN = "Bearer serviceToken";

    @Pact(provider = "feeRegister_lookUp", consumer = "pcs_api")
    public V4Pact createFeeLookupPact(PactBuilder builder) {

        PactDslJsonBody responseBody = new PactDslJsonBody()
            .stringType("code")
            .stringType("description")
            .numberType("version")
            .decimalType("fee_amount");

        return builder
            .usingLegacyDsl()
            .given("Fees exist for Probate")//change to other state
            .uponReceiving("a request for fees lookup")
            .path("/fees-register/fees/lookup")
            .method("GET")
            .headers(Map.of(
                HttpHeaders.ACCEPT, ContentType.JSON.toString(),
                SERVICE_AUTHORIZATION, SERVICE_TOKEN
            ))
            .query("service=possession claim&jurisdiction1=civil&jurisdiction2=county court"
                       + "&channel=default&applicant_type=all&event=issue"
                       + "&amount_or_volume=1&keyword=PossessionCC")
            .willRespondWith()
            .status(200)
            .headers(Map.of(HttpHeaders.CONTENT_TYPE, "application/json"))
            .body(responseBody)
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "createFeeLookupPact")
    void shouldReturnFeeLookupSuccessfully(MockServer mockServer) {

        Response response = given()
            .relaxedHTTPSValidation()
            .header("Accept", ContentType.JSON)
            .header(SERVICE_AUTHORIZATION, SERVICE_TOKEN)
            .queryParam("service", "possession claim")
            .queryParam("jurisdiction1", "civil")
            .queryParam("jurisdiction2", "county court")
            .queryParam("channel", "default")
            .queryParam("applicant_type", "all")
            .queryParam("event", "issue")
            .queryParam("amount_or_volume", "1")
            .queryParam("keyword", "PossessionCC")
            .when()
            .get(mockServer.getUrl() + "/fees-register/fees/lookup")
            .then()
            .statusCode(200)
            .extract()
            .response();

        assertThat(response.jsonPath().getString("code")).isNotEmpty();
        assertThat(response.jsonPath().getString("description")).isNotEmpty();
        assertThat(response.jsonPath().getInt("version")).isGreaterThanOrEqualTo(0);
        assertThat(response.jsonPath().getDouble("fee_amount")).isGreaterThanOrEqualTo(0.0);
    }
}
