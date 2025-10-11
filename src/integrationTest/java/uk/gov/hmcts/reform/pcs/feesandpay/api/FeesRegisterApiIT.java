package uk.gov.hmcts.reform.pcs.feesandpay.api;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import feign.FeignException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.reform.pcs.Application;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeResponse;

import java.math.BigDecimal;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@AutoConfigureWireMock(port = 0)
@TestPropertySource(properties = {
    "fees-register.api.url=http://localhost:${wiremock.server.port}",
    "spring.flyway.enabled=false",
    "spring.jms.servicebus.enabled=false"
})
@SpringBootTest(classes = Application.class)
class FeesRegisterApiIT {

    private final FeesRegisterApi feesRegisterApi;

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    FeesRegisterApiIT(FeesRegisterApi feesRegisterApi) {
        this.feesRegisterApi = feesRegisterApi;
    }

    private static final String SERVICE_AUTH_TOKEN = "Bearer test-token";
    private static final String LOOKUP_ENDPOINT = "/fees-register/fees/lookup";
    private static final String POSSESSION_SERVICE = "possession claim";
    private static final String CIVIL_JURISDICTION = "civil";
    private static final String COUNTY_COURT = "county court";
    private static final String DEFAULT_CHANNEL = "default";
    private static final String ISSUE_EVENT = "issue";
    private static final String ALL_APPLICANT_TYPE = "all";
    private static final String AMOUNT_OR_VOLUME = "1";
    private static final String POSSESSION_KEYWORD = "PossessionCC";

    @Test
    void shouldSuccessfullyLookupFee() {
        stubSuccessfulFeeResponse("FEE0412",
                                    "Recovery of Land - County Court", "4", 404.00);

        FeeResponse response = lookupPossessionFee();

        assertFeeResponse(response, new BigDecimal("404.00"));
        verifyFullFeeRequest();
    }

    @Test
    void shouldSuccessfullyLookupFeeWithKeyword() {
        stubSuccessfulFeeResponse("FEE0412",
                                    "Recovery of Land - County Court", "4", 404.00);

        FeeResponse response = lookupPossessionFee();

        assertFeeResponse(response, new BigDecimal("404.00"));
        verify(getRequestedFor(urlPathEqualTo(LOOKUP_ENDPOINT))
                    .withQueryParam("keyword", equalTo(POSSESSION_KEYWORD)));
    }

    @Test
    void shouldHandleFeeNotFound() {
        stubFor(buildBaseFeeRequest("unknown", "unknown service")
                    .willReturn(aResponse()
                                    .withStatus(404)
                                    .withHeader("Content-Type", "application/json")
                                    .withBody("""
                                        {
                                            "message": "Fee not found"
                                        }
                                        """)));

        assertThatThrownBy(() -> feesRegisterApi.lookupFee(
            SERVICE_AUTH_TOKEN,
            "unknown service",
            CIVIL_JURISDICTION,
            COUNTY_COURT,
            DEFAULT_CHANNEL,
            "unknown",
            ALL_APPLICANT_TYPE,
            AMOUNT_OR_VOLUME,
            null
        )).isInstanceOf(FeignException.class);
    }

    @Test
    void shouldHandleUnauthorizedRequest() {
        stubFor(get(urlPathEqualTo(LOOKUP_ENDPOINT))
                    .withHeader("ServiceAuthorization", equalTo("invalid-token"))
                    .willReturn(aResponse()
                                    .withStatus(401)
                                    .withHeader("Content-Type", "application/json")
                                    .withBody("""
                                        {
                                            "message": "Unauthorized"
                                        }
                                        """)));

        assertThatThrownBy(() -> feesRegisterApi.lookupFee(
            "invalid-token",
            POSSESSION_SERVICE,
            CIVIL_JURISDICTION,
            COUNTY_COURT,
            DEFAULT_CHANNEL,
            ISSUE_EVENT,
            ALL_APPLICANT_TYPE,
            AMOUNT_OR_VOLUME,
            POSSESSION_KEYWORD
        )).isInstanceOf(Exception.class);
    }

    @Test
    void shouldHandleServerError() {
        stubFor(get(urlPathEqualTo(LOOKUP_ENDPOINT))
                    .willReturn(aResponse()
                                    .withStatus(500)
                                    .withHeader("Content-Type", "application/json")
                                    .withBody("""
                                        {
                                            "message": "Internal server error"
                                        }
                                        """)));

        assertThatThrownBy(this::lookupPossessionFee).isInstanceOf(Exception.class);
    }

    @Test
    void shouldHandleZeroFeeAmount() {
        stubSuccessfulFeeResponse("FEE0000", "Fee waived - Help with Fees", "1", 0.00);

        FeeResponse response = lookupPossessionFee();

        assertThat(response).isNotNull();
        assertThat(response.getCode()).isEqualTo("FEE0000");
        assertThat(response.getFeeAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldHandleLargeFeeAmount() {
        stubSuccessfulFeeResponse("FEE9999",
                                    "High value possession claim fee", "1", 10000.00);

        FeeResponse response = lookupPossessionFee();

        assertThat(response).isNotNull();
        assertThat(response.getCode()).isEqualTo("FEE9999");
        assertThat(response.getFeeAmount()).isEqualByComparingTo(new BigDecimal("10000.00"));
    }

    private FeeResponse lookupPossessionFee() {
        return feesRegisterApi.lookupFee(
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
    }

    private void assertFeeResponse(FeeResponse response, BigDecimal amount) {
        assertThat(response).isNotNull();
        assertThat(response.getCode()).isEqualTo("FEE0412");
        assertThat(response.getDescription()).isEqualTo("Recovery of Land - County Court");
        assertThat(response.getVersion()).isEqualTo("4");
        assertThat(response.getFeeAmount()).isEqualByComparingTo(amount);
    }

    private void verifyFullFeeRequest() {
        verify(getRequestedFor(urlPathEqualTo(LOOKUP_ENDPOINT))
                .withQueryParam("service", equalTo(POSSESSION_SERVICE))
                .withQueryParam("jurisdiction1", equalTo(CIVIL_JURISDICTION))
                .withQueryParam("jurisdiction2", equalTo(COUNTY_COURT))
                .withQueryParam("channel", equalTo(DEFAULT_CHANNEL))
                .withQueryParam("event", equalTo(ISSUE_EVENT))
                .withQueryParam("applicantType", equalTo(ALL_APPLICANT_TYPE))
                .withQueryParam("amountOrVolume", equalTo(AMOUNT_OR_VOLUME))
                .withQueryParam("keyword", equalTo(POSSESSION_KEYWORD))
                .withHeader("ServiceAuthorization", equalTo(SERVICE_AUTH_TOKEN)));
    }

    private void stubSuccessfulFeeResponse(String code, String description, String version, double amount) {
        stubFor(buildBaseFeeRequest(ISSUE_EVENT, POSSESSION_SERVICE)
                    .withQueryParam("keyword", equalTo(POSSESSION_KEYWORD))
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withHeader("Content-Type", "application/json")
                                    .withBody(String.format("""
                                        {
                                            "code": "%s",
                                            "description": "%s",
                                            "version": "%s",
                                            "fee_amount": %.2f
                                        }
                                        """, code, description, version, amount))));
    }

    private MappingBuilder buildBaseFeeRequest(String event, String service) {
        return get(urlPathEqualTo(LOOKUP_ENDPOINT))
            .withQueryParam("service", equalTo(service))
            .withQueryParam("jurisdiction1", equalTo(CIVIL_JURISDICTION))
            .withQueryParam("jurisdiction2", equalTo(COUNTY_COURT))
            .withQueryParam("channel", equalTo(DEFAULT_CHANNEL))
            .withQueryParam("event", equalTo(event))
            .withQueryParam("applicantType", equalTo(ALL_APPLICANT_TYPE))
            .withQueryParam("amountOrVolume", equalTo(AMOUNT_OR_VOLUME))
            .withHeader("ServiceAuthorization", equalTo(SERVICE_AUTH_TOKEN));
    }
}
