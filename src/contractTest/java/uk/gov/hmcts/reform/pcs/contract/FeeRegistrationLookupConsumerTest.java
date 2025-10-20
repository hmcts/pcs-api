package uk.gov.hmcts.reform.pcs.contract;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Feign;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.fees.client.FeesApi;
import uk.gov.hmcts.reform.fees.client.model.FeeLookupResponseDto;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "feeRegister_lookUp")
class FeeRegistrationLookupConsumerTest {

    private FeesApi feesApi;

    private static final String POSSESSION_SERVICE = "possession claim";
    private static final String CIVIL_JURISDICTION = "civil";
    private static final String COUNTY_COURT = "county court";
    private static final String DEFAULT_CHANNEL = "default";
    private static final String ISSUE_EVENT = "issue";
    private static final BigDecimal AMOUNT = new BigDecimal("1");

    @BeforeEach
    void setUp(MockServer mockServer) {
        ObjectMapper objectMapper = new ObjectMapper();

        feesApi = Feign.builder()
            .contract(new SpringMvcContract())
            .encoder(new JacksonEncoder(objectMapper))
            .decoder(new JacksonDecoder(objectMapper))
            .target(FeesApi.class, mockServer.getUrl());
    }

    @Pact(provider = "feeRegister_lookUp", consumer = "pcs_api")
    public V4Pact createFeeLookupPact(PactDslWithProvider builder) {

        PactDslJsonBody responseBody = new PactDslJsonBody()
            .stringType("code", "FEE0412")
            .stringType("description", "Recovery of Land - County Court")
            .integerType("version", 4)
            .decimalType("fee_amount", 404.00);

        return builder
            .given("Fees exist for Possession claims")
            .uponReceiving("a request for Possession fees")
            .path("/fees-register/fees/lookup")
            .method("GET")
            .matchQuery("service", POSSESSION_SERVICE)
            .matchQuery("jurisdiction1", CIVIL_JURISDICTION)
            .matchQuery("jurisdiction2", COUNTY_COURT)
            .matchQuery("channel", DEFAULT_CHANNEL)
            .matchQuery("event", ISSUE_EVENT)
            .matchQuery("amount_or_volume", AMOUNT.toString())
            .willRespondWith()
            .status(HttpStatus.OK.value())
            .headers(Map.of(HttpHeaders.CONTENT_TYPE, "application/json"))
            .body(responseBody)
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "createFeeLookupPact")
    void shouldSuccessfullyLookupFee() {
        FeeLookupResponseDto response = feesApi.lookupFee(
            POSSESSION_SERVICE,
            CIVIL_JURISDICTION,
            COUNTY_COURT,
            DEFAULT_CHANNEL,
            ISSUE_EVENT,
            AMOUNT
        );

        assertThat(response.getCode()).isEqualTo("FEE0412");
        assertThat(response.getDescription()).isEqualTo("Recovery of Land - County Court");
        assertThat(response.getVersion()).isEqualTo(4);
        assertThat(response.getFeeAmount()).isEqualByComparingTo(new BigDecimal("404.00"));
    }
}
