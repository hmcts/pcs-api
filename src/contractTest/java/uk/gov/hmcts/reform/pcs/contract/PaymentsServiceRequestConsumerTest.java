package uk.gov.hmcts.reform.pcs.contract;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpHeaders;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactBuilder;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import static io.restassured.RestAssured.given;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "payment_accounts", port = "8080")
public class PaymentsServiceRequestConsumerTest {

    private static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";
    private static final String SERVICE_TOKEN = "Bearer serviceToken";

    
    @Pact(provider = "payment_accounts", consumer = "pcs_api")
    public V4Pact createServiceRequestPact(PactBuilder builder) {

        PactDslJsonBody requestBody = (PactDslJsonBody) new PactDslJsonBody()
            .stringValue("call_back_url", "http://callback.url")
            .stringValue("case_reference", "CASE123")
            .stringValue("ccd_case_number", "3873323117506524")
            .stringValue("hmcts_org_id", "ORG001")
            .object("case_payment_request")
                .stringValue("action", "submit")
                .stringValue("responsible_party", "claimant")
            .closeObject()
            .minArrayLike("fees", 1)
                .numberValue("calculated_amount", 0)
                .stringValue("code", "FEE001")
                .stringValue("version", "1")
                .numberValue("volume", 1)
            .closeArray();

        
        PactDslJsonBody responseBody = new PactDslJsonBody()
            .stringType("service_request_reference");

        return builder
            .usingLegacyDsl()
            .given("A Service Request Can be Created for a valid Payload")
            .uponReceiving("A Service Request Can be Created for a valid Payload")
            .path("/service-request")
            .method("POST")
            .headers(Map.of(
                HttpHeaders.ACCEPT, ContentType.JSON.toString(),
                HttpHeaders.CONTENT_TYPE, ContentType.JSON.toString(),
                SERVICE_AUTHORIZATION, SERVICE_TOKEN
            ))
            .body(requestBody)
            .willRespondWith()
            .status(201)
            .headers(Map.of(HttpHeaders.CONTENT_TYPE, "application/json"))
            .body(responseBody)
            .toPact(V4Pact.class);
    }

    
    @Test
    @PactTestFor(pactMethod = "createServiceRequestPact")
    void shouldReturnServiceRequestReference(MockServer mockServer) {

        
        PactDslJsonBody requestBody = (PactDslJsonBody) new PactDslJsonBody()
            .stringValue("call_back_url", "http://callback.url")
            .stringValue("case_reference", "CASE123")
            .stringValue("ccd_case_number", "3873323117506524")
            .stringValue("hmcts_org_id", "ORG001")
            .object("case_payment_request")
                .stringValue("action", "submit")
                .stringValue("responsible_party", "claimant")
            .closeObject()
            .minArrayLike("fees", 1)
                .numberValue("calculated_amount", 0)
                .stringValue("code", "FEE001")
                .stringValue("version", "1")
                .numberValue("volume", 1)
            .closeArray();

        
        Response response = given()
            .relaxedHTTPSValidation()
            .header("Accept", ContentType.JSON)
            .header("Content-Type", ContentType.JSON)
            .header(SERVICE_AUTHORIZATION, SERVICE_TOKEN)
            .body(requestBody.toString())
            .when()
            .post(mockServer.getUrl() + "/service-request")
            .then()
            .statusCode(201)
            .extract()
            .response();

        assertThat(response.jsonPath().getString("service_request_reference")).isNotEmpty();
    }
}
