package uk.gov.hmcts.reform.contract;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslRootValue;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = "s2s-auth.url=http://localhost:5050")
@EnableAutoConfiguration
@ExtendWith(PactConsumerTestExt.class)
@ExtendWith(SpringExtension.class)
@PactTestFor(providerName = "s2s_auth", port = "5050")
@SpringBootTest(classes = ServiceAuthorisationApi.class)
public class ServiceAuthorisationConsumerTest {

    private static final String AUTHORISATION_TOKEN = "Bearer someAuthorisationToken";
    public static final String MICRO_SERVICE_NAME = "pcs_api";
    public static final String MICRO_SERVICE_TOKEN = "microServiceToken";

    @Autowired
    private ServiceAuthorisationApi serviceAuthorisationApi;

    @Pact(provider = "s2s_auth", consumer = "pcs_api")
    public V4Pact executeLease(PactDslWithProvider builder) throws JsonProcessingException {

        return builder
            .given("microservice with valid credentials")
            .uponReceiving("a request for a token")
            .path("/lease")
            .method(HttpMethod.POST.toString())
            .body("{\"microservice\":\"pcs_api\",\"oneTimePassword\":\"784467\"}")
            .willRespondWith()
            .headers(Map.of(HttpHeaders.CONTENT_TYPE, "text/plain"))
            .status(HttpStatus.OK.value())
            .body(PactDslRootValue.stringType(MICRO_SERVICE_TOKEN))
            .toPact(V4Pact.class);
    }

    @Pact(provider = "s2s_auth", consumer = "pcs_api")
    public V4Pact executeDetails(PactDslWithProvider builder) throws JsonProcessingException {

        return builder.given("microservice with valid token")
            .uponReceiving("a request to validate details")
            .path("/details")
            .headers(HttpHeaders.AUTHORIZATION, AUTHORISATION_TOKEN)
            .method(HttpMethod.GET.toString())
            .willRespondWith()
            .headers(Map.of(HttpHeaders.CONTENT_TYPE, "text/plain"))
            .status(HttpStatus.OK.value())
            .body(PactDslRootValue.stringType(MICRO_SERVICE_NAME))
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "executeLease")
    void verifyLease(MockServer mockServer) {
        String mockUrl = mockServer.getUrl();

        Map<String, String> jsonPayload = new HashMap<>();
        jsonPayload.put("microservice", "pcs_api");
        jsonPayload.put("oneTimePassword", "784467");

        String token = given()
            .baseUri(mockUrl)
            .contentType("application/json")
            .body(jsonPayload)
            .when()
            .post("/lease")
            .then()
            .statusCode(200)
            .extract()
            .asString();

        assertThat(token)
            .isEqualTo("microServiceToken");

    }

    @Test
    @PactTestFor(pactMethod = "executeDetails")
    void verifyDetails(MockServer mockServer) {
        System.out.println("Pact mock server is running on port: " + mockServer.getPort());

        String token = serviceAuthorisationApi.getServiceName(AUTHORISATION_TOKEN);
        assertThat(token)
            .isEqualTo("pcs_api");
    }
}
