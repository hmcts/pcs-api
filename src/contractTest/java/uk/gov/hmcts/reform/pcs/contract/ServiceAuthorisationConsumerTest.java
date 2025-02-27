package uk.gov.hmcts.reform.pcs.contract;

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

import static org.assertj.core.api.Assertions.assertThat;

@EnableAutoConfiguration
@TestPropertySource(properties = "idam.s2s-auth.url=http://localhost:5050")
@ExtendWith(PactConsumerTestExt.class)
@ExtendWith(SpringExtension.class)
@PactTestFor(providerName = "s2s_auth", port = "5050")
@SpringBootTest(classes = ServiceAuthorisationApi.class)
public class ServiceAuthorisationConsumerTest {

    private static final String AUTHORISATION_TOKEN = "Bearer someAuthorisationToken";
    public static final String MICRO_SERVICE_NAME = "someMicroServiceName";
    public static final String MICRO_SERVICE_TOKEN = "someMicroServiceToken";

    @Autowired
    private ServiceAuthorisationApi serviceAuthorisationApi;

    @Pact(provider = "s2s_auth", consumer = "pcs_api")
    public V4Pact executeLease(PactDslWithProvider builder) throws JsonProcessingException {

        return builder
            .given("microservice with valid credentials")
            .uponReceiving("a request for a token")
            .path("/lease")
            .method(HttpMethod.POST.toString())
            .body("{\"microservice\":\"" + MICRO_SERVICE_NAME + "\", \"oneTimePassword\":\"784467\"}")
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
    void verifyLease() {

        Map<String, String> jsonPayload = new HashMap<>();
        jsonPayload.put("microservice", MICRO_SERVICE_NAME);
        jsonPayload.put("oneTimePassword", "784467");

        String token = serviceAuthorisationApi.serviceToken(jsonPayload);
        assertThat(token)
            .isEqualTo(MICRO_SERVICE_TOKEN);
    }

    @Test
    @PactTestFor(pactMethod = "executeDetails")
    void verifyDetails() {

        String token = serviceAuthorisationApi.getServiceName(AUTHORISATION_TOKEN);
        assertThat(token)
            .isEqualTo(MICRO_SERVICE_NAME);
    }
}
