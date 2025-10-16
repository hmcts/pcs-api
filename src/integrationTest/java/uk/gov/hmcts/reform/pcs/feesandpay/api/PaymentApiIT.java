package uk.gov.hmcts.reform.pcs.feesandpay.api;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import feign.FeignException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.reform.pcs.Application;
import uk.gov.hmcts.reform.pcs.feesandpay.dto.CasePaymentRequestDto;
import uk.gov.hmcts.reform.pcs.feesandpay.dto.FeeDto;
import uk.gov.hmcts.reform.pcs.feesandpay.model.ServiceRequestBody;
import uk.gov.hmcts.reform.pcs.feesandpay.model.ServiceRequestResponse;

import java.math.BigDecimal;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@AutoConfigureWireMock(port = 0)
@TestPropertySource(properties = {
    "payment.url=http://localhost:${wiremock.server.port}",
    "spring.flyway.enabled=false",
    "spring.jms.servicebus.enabled=false"
})
@SpringBootTest(classes = Application.class)
class PaymentApiIT {

    private final PaymentApi paymentApi;

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    PaymentApiIT(PaymentApi paymentApi) {
        this.paymentApi = paymentApi;
    }

    private static final String AUTH_TOKEN = "Bearer user-auth-token";
    private static final String SERVICE_AUTH_TOKEN = "Bearer service-auth-token";
    private static final String SERVICE_REQUEST_ENDPOINT = "/service-request";
    private static final String CCD_CASE_NUMBER = "1234567890123456";
    private static final String HMCTS_ORG_ID = "AAA3";
    private static final String CASE_REFERENCE = "REF-2024-001";
    private static final String CALLBACK_URL = "https://example.com/callback";
    private static final String FEE_CODE = "FEE0412";
    private static final String FEE_VERSION = "4";
    private static final BigDecimal FEE_AMOUNT = new BigDecimal("404.00");
    private static final Integer FEE_VOLUME = 1;
    private static final String RESPONSIBLE_PARTY = "Test Claimant";
    private static final String ACTION = "case-issue";

    @Test
    void shouldSuccessfullyCreateServiceRequest() {
        String serviceRequestReference = "2024-1234567890";
        stubSuccessfulServiceRequestResponse(serviceRequestReference);

        ServiceRequestBody requestBody = buildServiceRequestBody();
        ServiceRequestResponse response = paymentApi.createServiceRequest(
            AUTH_TOKEN,
            SERVICE_AUTH_TOKEN,
            requestBody
        );

        assertThat(response).isNotNull();
        assertThat(response.getServiceRequestReference()).isEqualTo(serviceRequestReference);
        verifyServiceRequestWithHeaders();
    }

    @Test
    void shouldIncludeAllRequiredFieldsInRequest() {
        stubSuccessfulServiceRequestResponse("2024-1234567890");

        ServiceRequestBody requestBody = buildServiceRequestBody();
        paymentApi.createServiceRequest(AUTH_TOKEN, SERVICE_AUTH_TOKEN, requestBody);

        verify(
            postRequestedFor(urlPathEqualTo(SERVICE_REQUEST_ENDPOINT)).withRequestBody(equalToJson("""
                {
                    "ccd_case_number": "%s",
                    "hmcts_org_id": "%s",
                    "case_reference": "%s",
                    "callback_url": "%s",
                    "case_payment_request": {
                        "action": "%s",
                        "responsible_party": "%s"
                    },
                    "fees": [
                        {
                            "code": "%s",
                            "version": "%s",
                            "calculated_amount": %.2f,
                            "volume": %d
                        }
                    ]
                }
                """.formatted(
                    CCD_CASE_NUMBER,
                       HMCTS_ORG_ID,
                       CASE_REFERENCE,
                       CALLBACK_URL,
                       ACTION,
                       RESPONSIBLE_PARTY,
                       FEE_CODE,
                       FEE_VERSION,
                       FEE_AMOUNT,
                       FEE_VOLUME
                   ), true, true)));
    }

    @Test
    void shouldHandleMultipleFees() {
        String serviceRequestReference = "2024-9876543210";
        stubSuccessfulServiceRequestResponse(serviceRequestReference);

        FeeDto fee1 = FeeDto.builder()
            .code("FEE0001")
            .version("1")
            .calculatedAmount(new BigDecimal("100.00"))
            .volume(1)
            .build();

        FeeDto fee2 = FeeDto.builder()
            .code("FEE0002")
            .version("2")
            .calculatedAmount(new BigDecimal("200.00"))
            .volume(2)
            .build();

        ServiceRequestBody requestBody = ServiceRequestBody.builder()
            .ccdCaseNumber(CCD_CASE_NUMBER)
            .caseReference(CASE_REFERENCE)
            .hmctsOrgId(HMCTS_ORG_ID)
            .callbackUrl(CALLBACK_URL)
            .casePaymentRequest(buildCasePaymentRequest())
            .fees(new FeeDto[]{fee1, fee2})
            .build();

        ServiceRequestResponse response = paymentApi.createServiceRequest(
            AUTH_TOKEN,
            SERVICE_AUTH_TOKEN,
            requestBody
        );

        assertThat(response).isNotNull();
        assertThat(response.getServiceRequestReference()).isEqualTo(serviceRequestReference);
    }

    @Test
    void shouldIncludeOptionalFeeFields() {
        stubSuccessfulServiceRequestResponse("2024-OPTIONAL-FIELDS");

        FeeDto fee = FeeDto.builder()
            .code(FEE_CODE)
            .version(FEE_VERSION)
            .calculatedAmount(FEE_AMOUNT)
            .volume(FEE_VOLUME)
            .memoLine("Test memo line")
            .ccdCaseNumber(CCD_CASE_NUMBER)
            .reference("TEST-REF-001")
            .build();

        ServiceRequestBody requestBody = ServiceRequestBody.builder()
            .ccdCaseNumber(CCD_CASE_NUMBER)
            .caseReference(CASE_REFERENCE)
            .callbackUrl(CALLBACK_URL)
            .casePaymentRequest(buildCasePaymentRequest())
            .fees(new FeeDto[]{fee})
            .build();

        ServiceRequestResponse response = paymentApi.createServiceRequest(
            AUTH_TOKEN,
            SERVICE_AUTH_TOKEN,
            requestBody
        );

        assertThat(response).isNotNull();
        verify(postRequestedFor(urlPathEqualTo(SERVICE_REQUEST_ENDPOINT))
                   .withRequestBody(equalToJson("""
                {
                    "ccd_case_number": "%s",
                    "case_reference": "%s",
                    "callback_url": "%s",
                    "hmcts_org_id": "AAA3",
                    "case_payment_request": {
                        "action": "%s",
                        "responsible_party": "%s"
                    },
                    "fees": [
                        {
                            "code": "%s",
                            "version": "%s",
                            "calculated_amount": %.2f,
                            "volume": %d,
                            "memoLine": "Test memo line",
                            "ccdCaseNumber": "%s",
                            "reference": "TEST-REF-001"
                        }
                    ]
                }
                """.formatted(
                       CCD_CASE_NUMBER,
                       CASE_REFERENCE,
                       CALLBACK_URL,
                       ACTION,
                       RESPONSIBLE_PARTY,
                       FEE_CODE,
                       FEE_VERSION,
                       FEE_AMOUNT,
                       FEE_VOLUME,
                       CCD_CASE_NUMBER
                   ), true, true)));
    }

    @Test
    void shouldUseDefaultHmctsOrgId() {
        stubSuccessfulServiceRequestResponse("2024-DEFAULT-ORG");

        ServiceRequestBody requestBody = ServiceRequestBody.builder()
            .ccdCaseNumber(CCD_CASE_NUMBER)
            .caseReference(CASE_REFERENCE)
            .callbackUrl(CALLBACK_URL)
            .casePaymentRequest(buildCasePaymentRequest())
            .fees(new FeeDto[]{buildFeeDto()})
            .build();

        paymentApi.createServiceRequest(AUTH_TOKEN, SERVICE_AUTH_TOKEN, requestBody);

        verify(postRequestedFor(urlPathEqualTo(SERVICE_REQUEST_ENDPOINT))
                   .withRequestBody(equalToJson("""
                {
                    "hmcts_org_id": "AAA3"
                }
                """, false, true)));
    }

    @Test
    void shouldHandleUnauthorizedRequest() {
        stubFor(post(urlPathEqualTo(SERVICE_REQUEST_ENDPOINT))
                    .withHeader("Authorization", equalTo("Bearer invalid-token"))
                    .willReturn(aResponse()
                                    .withStatus(401)
                                    .withHeader("Content-Type", "application/json")
                                    .withBody("""
                    {
                        "message": "Unauthorized"
                    }
                    """)));

        ServiceRequestBody requestBody = buildServiceRequestBody();

        assertThatThrownBy(() -> paymentApi.createServiceRequest(
            "Bearer invalid-token",
            SERVICE_AUTH_TOKEN,
            requestBody
        )).isInstanceOf(FeignException.class);
    }

    @Test
    void shouldHandleServiceAuthorizationFailure() {
        stubFor(post(urlPathEqualTo(SERVICE_REQUEST_ENDPOINT))
                    .withHeader("serviceAuthorization", equalTo("Bearer invalid-service-token"))
                    .willReturn(aResponse()
                                    .withStatus(403)
                                    .withHeader("Content-Type", "application/json")
                                    .withBody("""
                    {
                        "message": "Service authorization failed"
                    }
                    """)));

        ServiceRequestBody requestBody = buildServiceRequestBody();

        assertThatThrownBy(() -> paymentApi.createServiceRequest(
            AUTH_TOKEN,
            "Bearer invalid-service-token",
            requestBody
        )).isInstanceOf(FeignException.class);
    }

    @Test
    void shouldHandleBadRequest() {
        stubFor(post(urlPathEqualTo(SERVICE_REQUEST_ENDPOINT))
                    .willReturn(aResponse()
                                    .withStatus(400)
                                    .withHeader("Content-Type", "application/json")
                                    .withBody("""
                    {
                        "message": "Invalid request body",
                        "errors": ["ccd_case_number is required"]
                    }
                    """)));

        ServiceRequestBody requestBody = ServiceRequestBody.builder()
            .caseReference(CASE_REFERENCE)
            .build();

        assertThatThrownBy(() -> paymentApi.createServiceRequest(
            AUTH_TOKEN,
            SERVICE_AUTH_TOKEN,
            requestBody
        )).isInstanceOf(FeignException.class);
    }

    @Test
    void shouldHandleServerError() {
        stubFor(post(urlPathEqualTo(SERVICE_REQUEST_ENDPOINT))
                    .willReturn(aResponse()
                                    .withStatus(500)
                                    .withHeader("Content-Type", "application/json")
                                    .withBody("""
                    {
                        "message": "Internal server error"
                    }
                    """)));

        ServiceRequestBody requestBody = buildServiceRequestBody();

        assertThatThrownBy(() -> paymentApi.createServiceRequest(
            AUTH_TOKEN,
            SERVICE_AUTH_TOKEN,
            requestBody
        )).isInstanceOf(FeignException.class);
    }

    @Test
    void shouldHandleServiceUnavailable() {
        stubFor(post(urlPathEqualTo(SERVICE_REQUEST_ENDPOINT))
                    .willReturn(aResponse()
                                    .withStatus(503)
                                    .withHeader("Content-Type", "application/json")
                                    .withBody("""
                    {
                        "message": "Service temporarily unavailable"
                    }
                    """)));

        ServiceRequestBody requestBody = buildServiceRequestBody();

        assertThatThrownBy(() -> paymentApi.createServiceRequest(
            AUTH_TOKEN,
            SERVICE_AUTH_TOKEN,
            requestBody
        )).isInstanceOf(FeignException.class);
    }

    @Test
    void shouldHandleRequestWithoutCallback() {
        String serviceRequestReference = "2024-NO-CALLBACK";
        stubSuccessfulServiceRequestResponse(serviceRequestReference);

        ServiceRequestBody requestBody = ServiceRequestBody.builder()
            .ccdCaseNumber(CCD_CASE_NUMBER)
            .caseReference(CASE_REFERENCE)
            .hmctsOrgId(HMCTS_ORG_ID)
            .callbackUrl(null)
            .casePaymentRequest(buildCasePaymentRequest())
            .fees(new FeeDto[]{buildFeeDto()})
            .build();

        ServiceRequestResponse response = paymentApi.createServiceRequest(
            AUTH_TOKEN,
            SERVICE_AUTH_TOKEN,
            requestBody
        );

        assertThat(response).isNotNull();
        assertThat(response.getServiceRequestReference()).isEqualTo(serviceRequestReference);
    }

    @Test
    void shouldVerifyCorrectContentType() {
        stubSuccessfulServiceRequestResponse("2024-CONTENT-TYPE");

        ServiceRequestBody requestBody = buildServiceRequestBody();
        paymentApi.createServiceRequest(AUTH_TOKEN, SERVICE_AUTH_TOKEN, requestBody);

        verify(postRequestedFor(urlPathEqualTo(SERVICE_REQUEST_ENDPOINT))
                   .withHeader("Content-Type", equalTo("application/json")));
    }

    @Test
    void shouldHandleResponseWithAdditionalFields() {
        stubFor(buildBaseServiceRequestStub()
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withHeader("Content-Type", "application/json")
                                    .withBody("""
                    {
                        "service_request_reference": "2024-EXTRA-FIELDS",
                        "status": "Created",
                        "date_created": "2024-01-15T10:30:00Z"
                    }
                    """)));

        ServiceRequestBody requestBody = buildServiceRequestBody();
        ServiceRequestResponse response = paymentApi.createServiceRequest(
            AUTH_TOKEN,
            SERVICE_AUTH_TOKEN,
            requestBody
        );

        assertThat(response).isNotNull();
        assertThat(response.getServiceRequestReference()).isEqualTo("2024-EXTRA-FIELDS");
    }

    @Test
    void shouldHandleEmptyFeesArray() {
        String serviceRequestReference = "2024-NO-FEES";
        stubSuccessfulServiceRequestResponse(serviceRequestReference);

        ServiceRequestBody requestBody = ServiceRequestBody.builder()
            .ccdCaseNumber(CCD_CASE_NUMBER)
            .caseReference(CASE_REFERENCE)
            .callbackUrl(CALLBACK_URL)
            .casePaymentRequest(buildCasePaymentRequest())
            .fees(new FeeDto[]{})
            .build();

        ServiceRequestResponse response = paymentApi.createServiceRequest(
            AUTH_TOKEN,
            SERVICE_AUTH_TOKEN,
            requestBody
        );

        assertThat(response).isNotNull();
        assertThat(response.getServiceRequestReference()).isEqualTo(serviceRequestReference);
    }

    private ServiceRequestBody buildServiceRequestBody() {
        return ServiceRequestBody.builder()
            .ccdCaseNumber(CCD_CASE_NUMBER)
            .caseReference(CASE_REFERENCE)
            .hmctsOrgId(HMCTS_ORG_ID)
            .callbackUrl(CALLBACK_URL)
            .casePaymentRequest(buildCasePaymentRequest())
            .fees(new FeeDto[]{buildFeeDto()})
            .build();
    }

    private CasePaymentRequestDto buildCasePaymentRequest() {
        return CasePaymentRequestDto.builder()
            .action(ACTION)
            .responsibleParty(RESPONSIBLE_PARTY)
            .build();
    }

    private FeeDto buildFeeDto() {
        return FeeDto.builder()
            .code(FEE_CODE)
            .version(FEE_VERSION)
            .calculatedAmount(FEE_AMOUNT)
            .volume(FEE_VOLUME)
            .build();
    }

    private void stubSuccessfulServiceRequestResponse(String serviceRequestReference) {
        stubFor(buildBaseServiceRequestStub()
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withHeader("Content-Type", "application/json")
                                    .withBody(String.format("""
                    {
                        "service_request_reference": "%s"
                    }
                    """, serviceRequestReference))));
    }

    private MappingBuilder buildBaseServiceRequestStub() {
        return post(urlPathEqualTo(SERVICE_REQUEST_ENDPOINT))
            .withHeader("Authorization", equalTo(AUTH_TOKEN))
            .withHeader("serviceAuthorization", equalTo(SERVICE_AUTH_TOKEN))
            .withHeader("Content-Type", equalTo("application/json"));
    }

    private void verifyServiceRequestWithHeaders() {
        verify(postRequestedFor(urlPathEqualTo(SERVICE_REQUEST_ENDPOINT))
                   .withHeader("Authorization", equalTo(AUTH_TOKEN))
                   .withHeader("serviceAuthorization", equalTo(SERVICE_AUTH_TOKEN))
                   .withHeader("Content-Type", equalTo("application/json")));
    }
}
