package uk.gov.hmcts.reform.pcs.contract;

import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.cloud.openfeign.FeignClientsConfiguration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.pcs.feesandpay.api.FeesRegisterApi;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeResponse;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ImportAutoConfiguration({
    FeignAutoConfiguration.class,
    FeignClientsConfiguration.class,
    HttpMessageConvertersAutoConfiguration.class
})
@EnableFeignClients(clients = FeesRegisterApi.class)
@TestPropertySource(properties = "fees-register.api.url=http://localhost:8484")
@ExtendWith({PactConsumerTestExt.class, SpringExtension.class})
@PactTestFor(providerName = "feeRegister_lookUp", port = "8484")
class FeeRegistrationLookupConsumerTest {

    @Autowired
    private FeesRegisterApi feesRegisterApi;

    private static final String SERVICE_AUTH_TOKEN = "Bearer serviceToken";

    private static final String POSSESSION_SERVICE = "possession claim";
    private static final String CIVIL_JURISDICTION = "civil";
    private static final String COUNTY_COURT = "county court";
    private static final String DEFAULT_CHANNEL = "default";
    private static final String ISSUE_EVENT = "issue";
    private static final String ALL_APPLICANT_TYPE = "all";
    private static final String AMOUNT_OR_VOLUME = "1";
    private static final String POSSESSION_KEYWORD = "PossessionCC";

    @Pact(provider = "feeRegister_lookUp", consumer = "pcs_api")
    public V4Pact createFeeLookupPact(PactDslWithProvider builder) {

        PactDslJsonBody responseBody = new PactDslJsonBody()
            .stringType("code", "FEE0412")
            .stringType("description", "Recovery of Land - County Court")
            .integerType("version", 4)
            .decimalType("fee_amount", 404.00);

        return builder
            .given("Fees exist for Probate")
            .uponReceiving("a request for Possession fees")
            .path("/fees-register/fees/lookup")
            .method("GET")
            .headers(Map.of(
                "ServiceAuthorization", SERVICE_AUTH_TOKEN
            ))
            .query("service=possession claim"
                       + "&jurisdiction1=" + CIVIL_JURISDICTION
                       + "&jurisdiction2=" + COUNTY_COURT
                       + "&channel=" + DEFAULT_CHANNEL
                       + "&event=" + ISSUE_EVENT
                       + "&applicantType=" + ALL_APPLICANT_TYPE
                       + "&amountOrVolume=" + AMOUNT_OR_VOLUME
                       + "&keyword=" + POSSESSION_KEYWORD)
            .willRespondWith()
            .status(HttpStatus.OK.value())
            .headers(Map.of(HttpHeaders.CONTENT_TYPE, "application/json"))
            .body(responseBody)
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "createFeeLookupPact")
    void shouldSuccessfullyLookupFee() {
        FeeResponse response = feesRegisterApi.lookupFee(
            SERVICE_AUTH_TOKEN,
            POSSESSION_SERVICE,
            CIVIL_JURISDICTION,
            COUNTY_COURT,
            DEFAULT_CHANNEL,
            ISSUE_EVENT,
            ALL_APPLICANT_TYPE,
            AMOUNT_OR_VOLUME,
            POSSESSION_KEYWORD
        );

        assertThat(response.getCode()).isEqualTo("FEE0412");
        assertThat(response.getDescription()).isEqualTo("Recovery of Land - County Court");
        assertThat(response.getVersion()).isEqualTo("4");
        assertThat(response.getFeeAmount()).isEqualByComparingTo(new BigDecimal("404.00"));
    }
}
