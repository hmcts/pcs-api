package uk.gov.hmcts.reform.pcs.contract;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.cloud.openfeign.FeignClientsConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.fees.client.FeesApi;
import uk.gov.hmcts.reform.fees.client.FeesClient;
import uk.gov.hmcts.reform.fees.client.model.FeeLookupResponseDto;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ImportAutoConfiguration({
    FeignAutoConfiguration.class,
    FeignClientsConfiguration.class,
    HttpMessageConvertersAutoConfiguration.class
})
@ComponentScan(basePackages = "uk.gov.hmcts.reform.fees.client")
@TestPropertySource(properties = {
    "fees.api.url=http://localhost:8080",
    "fees.api.service=possession claim",
    "fees.api.jurisdiction1=civil",
    "fees.api.jurisdiction2=county court"
})
@ExtendWith({PactConsumerTestExt.class, SpringExtension.class})
@PactTestFor(providerName = "feeRegister_lookUp")
class FeeRegistrationLookupConsumerTest {

    private final FeesClient feesClient;

    private static final String POSSESSION_SERVICE = "possession claim";
    private static final String CIVIL_JURISDICTION = "civil";
    private static final String COUNTY_COURT = "county court";
    private static final String DEFAULT_CHANNEL = "default";
    private static final String ISSUE_EVENT = "issue";
    private static final BigDecimal AMOUNT = new BigDecimal("1");

    @Autowired
    FeeRegistrationLookupConsumerTest(FeesClient feesClient) {
        this.feesClient = feesClient;
    }

    @BeforeEach
    void setUp(MockServer mockServer) {
        FeesApi feesApi = (FeesApi) ReflectionTestUtils.getField(feesClient, "feesApi");
        ReflectionTestUtils.setField(feesApi, "url", mockServer.getUrl());
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
            .query("service=" + POSSESSION_SERVICE
                       + "&jurisdiction1=" + CIVIL_JURISDICTION
                       + "&jurisdiction2=" + COUNTY_COURT
                       + "&channel=" + DEFAULT_CHANNEL
                       + "&event=" + ISSUE_EVENT
                       + "&amount_or_volume=" + AMOUNT)
            .willRespondWith()
            .status(HttpStatus.OK.value())
            .headers(Map.of(HttpHeaders.CONTENT_TYPE, "application/json"))
            .body(responseBody)
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "createFeeLookupPact")
    void shouldSuccessfullyLookupFee(MockServer mockServer) {
        FeeLookupResponseDto response = feesClient.lookupFee(
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
