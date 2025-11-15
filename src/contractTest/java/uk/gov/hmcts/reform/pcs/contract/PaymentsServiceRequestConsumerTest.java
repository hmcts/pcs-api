package uk.gov.hmcts.reform.pcs.contract;

import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.cloud.openfeign.FeignClientsConfiguration;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.payments.client.PaymentsApi;
import uk.gov.hmcts.reform.payments.client.models.CasePaymentRequestDto;
import uk.gov.hmcts.reform.payments.client.models.FeeDto;
import uk.gov.hmcts.reform.payments.request.CreateServiceRequestDTO;
import uk.gov.hmcts.reform.payments.response.PaymentServiceResponse;
import java.math.BigDecimal;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.TestConstructor.AutowireMode.ALL;

@ImportAutoConfiguration({
    FeignAutoConfiguration.class,
    FeignClientsConfiguration.class,
    HttpMessageConvertersAutoConfiguration.class
})
@EnableFeignClients(clients = PaymentsApi.class)
@TestPropertySource(properties = "payments.api.url=http://localhost:8080")
@ExtendWith({PactConsumerTestExt.class, SpringExtension.class})
@PactTestFor(providerName = "payment_accounts", port = "8080")
@RequiredArgsConstructor
@TestConstructor(autowireMode = ALL)
public class PaymentsServiceRequestConsumerTest {
    private final PaymentsApi paymentsApi;

    private static final String SERVICE_AUTH_TOKEN = "Bearer serviceToken";
    private static final String AUTHORIZATION_TOKEN = "Bearer userToken";
    private static final String CALL_BACK_URL = "http://callback.url";
    private static final String CASE_REFERENCE = "CASE123";
    private static final String CCD_CASE_NUMBER = "3873323117506524";
    private static final String HMCTS_ORG_ID = "ORG001";
    private static final String ACTION = "Submit";
    private static final String RESPONSIBLE_PARTY = "Claimant";
    private static final BigDecimal CALCULATED_AMOUNT = new BigDecimal("404.00");
    private static final String CODE = "FEE001";
    private static final String VERSION = "1";
    private static final Number VOLUME = 1;

    private static final CasePaymentRequestDto casePaymentRequest = new CasePaymentRequestDto(
        ACTION,
        RESPONSIBLE_PARTY
    );

    //Building FeeDto to populate fields otherwise will return Null.
    private static final FeeDto[] fees = new FeeDto[]{
        FeeDto.builder()
            .calculatedAmount(new BigDecimal("404.00"))
            .ccdCaseNumber("")
            .code("FEE001")
            .description("")
            .id(100)
            .jurisdiction1("")
            .jurisdiction2("")
            .memoLine("")
            .naturalAccountCode("")
            .netAmount(BigDecimal.valueOf(100))
            .reference("")
            .version("1")
            .volume(1)
            .build()
    };

    @Pact(provider = "payment_accounts", consumer = "pcs_api")
    public V4Pact createServiceRequestPact(PactDslWithProvider builder) {
        //Building Request body for Pact test:
        PactDslJsonBody requestBody = (PactDslJsonBody) new PactDslJsonBody()
            .stringValue("call_back_url", CALL_BACK_URL)
            .stringValue("case_reference", CASE_REFERENCE)
            .stringValue("ccd_case_number", CCD_CASE_NUMBER)
            .stringValue("hmcts_org_id", HMCTS_ORG_ID)
            .object("case_payment_request")
            .stringValue("action", ACTION)
            .stringValue("responsible_party", RESPONSIBLE_PARTY)
            .closeObject()
            .minArrayLike("fees", 1)
            .numberValue("calculated_amount", CALCULATED_AMOUNT)
            .stringValue("code", CODE)
            .stringValue("version", VERSION)
            .numberValue("volume", VOLUME)
            // The below fields are not needed for request, only for FeesDto (Fees Array) type expectations:
            .stringType("ccd_case_number", "")
            .stringType("description", "")
            .numberType("id")
            .stringType("jurisdiction1", "")
            .stringType("jurisdiction2", "")
            .stringType("memo_line", "")
            .stringType("natural_account_code", "")
            .stringType("reference", "")
            .numberType("net_amount")
            .closeArray();
        //Building Response body for Pact test:
        PactDslJsonBody responseBody = new PactDslJsonBody()
            .stringType("service_request_reference");

        return builder
            .given("A Service Request Can be Created for a valid Payload")
            .uponReceiving("A Service Request Can be Created for a valid Payload")
            .path("/service-request")
            .method("POST")
            .headers(Map.of(
                HttpHeaders.CONTENT_TYPE, "application/json",
                "ServiceAuthorization", SERVICE_AUTH_TOKEN
            ))
            .body(requestBody.toString())
            .willRespondWith()
            .status(201)
            .headers(Map.of(HttpHeaders.CONTENT_TYPE, "application/json"))
            .body(responseBody)
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "createServiceRequestPact")
    void shouldReturnServiceRequestReference() {
        //Initialising CreateServiceRequestDTO to fulfill type expectations for createServiceRequest method.
        CreateServiceRequestDTO createServiceRequestDTO = new CreateServiceRequestDTO(
            CALL_BACK_URL, casePaymentRequest,
            CASE_REFERENCE, CCD_CASE_NUMBER,
            fees, HMCTS_ORG_ID
        );
        PaymentServiceResponse paymentServiceResponse = paymentsApi.createServiceRequest(
            AUTHORIZATION_TOKEN,
            SERVICE_AUTH_TOKEN,
            createServiceRequestDTO
        );
        assertThat(paymentServiceResponse.getServiceRequestReference()).isNotNull();
    }
}
