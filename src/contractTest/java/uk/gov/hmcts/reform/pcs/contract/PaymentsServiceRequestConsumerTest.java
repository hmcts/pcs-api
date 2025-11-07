package uk.gov.hmcts.reform.pcs.contract;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.cloud.openfeign.FeignClientsConfiguration;
import org.springframework.http.HttpHeaders;

import au.com.dius.pact.consumer.dsl.PactBuilder;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;

import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.payments.client.PaymentsApi;
import uk.gov.hmcts.reform.payments.client.models.CasePaymentRequestDto;
import uk.gov.hmcts.reform.payments.client.models.FeeDto;
import uk.gov.hmcts.reform.payments.request.CreateServiceRequestDTO;
import uk.gov.hmcts.reform.payments.response.PaymentServiceResponse;


@ImportAutoConfiguration({
    FeignAutoConfiguration.class,
    FeignClientsConfiguration.class,
    HttpMessageConvertersAutoConfiguration.class
})


@EnableFeignClients(clients = PaymentsApi.class)
@TestPropertySource(properties = "payments.api.url=http://localhost:8080")
@ExtendWith({PactConsumerTestExt.class, SpringExtension.class})

@PactTestFor(providerName = "payment_accounts", port = "8080")
public class PaymentsServiceRequestConsumerTest {

    private static final String SERVICE_AUTHORISATION = "Bearer serviceToken";
    private static final String AUTHORISATION = "Bearer userToken";
    private static final String CALL_BACK_URL = "http://callback.url";
    private static final String CASE_REFERENCE = "CASE123";
    private static final String CCD_CASE_NUMBER = "3873323117506524";
    private static final String HMCTS_ORG_ID = "ORG001";
    private static final String ACTION = "Submit";
    private static final String RESPONSIBLE_PARTY = "Claimant";

    private static final CasePaymentRequestDto casePaymentRequest = new CasePaymentRequestDto(ACTION,RESPONSIBLE_PARTY);
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


    @Autowired
    private PaymentsApi paymentsApi;

    @Pact(provider = "payment_accounts", consumer = "pcs_api")
    public V4Pact createServiceRequestPact(PactBuilder builder) {

        PactDslJsonBody requestBody = (PactDslJsonBody) new PactDslJsonBody()
            .stringValue("call_back_url", "http://callback.url")
            .stringValue("case_reference", "CASE123")
            .stringValue("ccd_case_number", "3873323117506524")
            .stringValue("hmcts_org_id", "ORG001")
            .object("case_payment_request")
            .stringValue("action", "Submit")
            .stringValue("responsible_party", "Claimant")
            .closeObject()
            .minArrayLike("fees", 1)
            .numberValue("calculated_amount", 404.00)
            .stringValue("code", "FEE001")
            .stringValue("version", "1")
            .numberValue("volume", 1)
            // Fields not needed for request, only for FeesDto type expectations:
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

        PactDslJsonBody responseBody = new PactDslJsonBody()
            .stringType("service_request_reference");

        return builder
            .usingLegacyDsl()
            .given("A Service Request Can be Created for a valid Payload")
            .uponReceiving("A Service Request Can be Created for a valid Payload")
            .path("/service-request")
            .method("POST")
            .headers(Map.of(
                HttpHeaders.CONTENT_TYPE, "application/json",
                "ServiceAuthorization", "Bearer serviceToken"
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

        CreateServiceRequestDTO createServiceRequestDTO = new CreateServiceRequestDTO(CALL_BACK_URL,casePaymentRequest,
                                                                                      CASE_REFERENCE,CCD_CASE_NUMBER,
                                                                                      fees,HMCTS_ORG_ID);

        PaymentServiceResponse requestBody = paymentsApi.createServiceRequest(
            AUTHORISATION,
            SERVICE_AUTHORISATION,
            createServiceRequestDTO);

        assertThat(requestBody.getServiceRequestReference()).isNotNull();
    }
}

