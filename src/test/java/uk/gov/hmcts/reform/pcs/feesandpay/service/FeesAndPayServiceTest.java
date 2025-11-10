package uk.gov.hmcts.reform.pcs.feesandpay.service;

import feign.FeignException.InternalServerError;
import feign.FeignException.NotFound;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fees.client.FeesClient;
import uk.gov.hmcts.reform.fees.client.model.FeeLookupResponseDto;
import uk.gov.hmcts.reform.payments.client.PaymentsClient;
import uk.gov.hmcts.reform.payments.client.models.CasePaymentRequestDto;
import uk.gov.hmcts.reform.payments.client.models.FeeDto;
import uk.gov.hmcts.reform.payments.request.CreateServiceRequestDTO;
import uk.gov.hmcts.reform.payments.response.PaymentServiceResponse;
import uk.gov.hmcts.reform.pcs.feesandpay.config.FeesConfiguration;
import uk.gov.hmcts.reform.pcs.feesandpay.config.FeesConfiguration.LookUpReferenceData;
import uk.gov.hmcts.reform.pcs.feesandpay.exception.FeeNotFoundException;
import uk.gov.hmcts.reform.pcs.feesandpay.mapper.PaymentRequestMapper;
import uk.gov.hmcts.reform.pcs.idam.IdamService;

import java.math.BigDecimal;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeesAndPayServiceTest {

    @Mock
    private FeesConfiguration feesConfiguration;

    @Mock
    private FeesClient feesClient;

    @Mock
    private PaymentsClient paymentsClient;

    @Mock
    private PaymentRequestMapper paymentRequestMapper;

    @Mock
    private IdamService idamService;

    @InjectMocks
    private FeesAndPayService feesAndPayService;

    @Captor
    private ArgumentCaptor<CreateServiceRequestDTO> createServiceRequestCaptor;

    private static final String FEE_TYPE = "caseIssueFee";

    private LookUpReferenceData lookUpReferenceData;
    private FeeLookupResponseDto feeLookupResponseDto;

    @BeforeEach
    void setUp() throws Exception {
        var callbackUrlField = FeesAndPayService.class.getDeclaredField("callbackUrl");
        callbackUrlField.setAccessible(true);
        callbackUrlField.set(feesAndPayService, "https://callback");

        var hmctsOrgIdField = FeesAndPayService.class.getDeclaredField("hmctsOrgId");
        hmctsOrgIdField.setAccessible(true);
        hmctsOrgIdField.set(feesAndPayService, "TEST_ORG");

        lookUpReferenceData = new LookUpReferenceData();
        lookUpReferenceData.setChannel("default");
        lookUpReferenceData.setEvent("issue");
        lookUpReferenceData.setApplicantType("all");
        lookUpReferenceData.setAmountOrVolume(new BigDecimal("1"));
        lookUpReferenceData.setKeyword("PossessionCC");

        feeLookupResponseDto = FeeLookupResponseDto.builder()
            .code("FEE0412")
            .description("Recovery of Land - County Court")
            .version(4)
            .feeAmount(BigDecimal.valueOf(404.00))
            .build();
    }

    @Test
    void shouldSuccessfullyGetFee() {
        when(feesConfiguration.getLookup(FEE_TYPE)).thenReturn(lookUpReferenceData);
        when(feesClient.lookupFee(
            "default",
            "issue",
            new BigDecimal("1"),
            "PossessionCC"
        )).thenReturn(feeLookupResponseDto);

        FeeLookupResponseDto result = feesAndPayService.getFee(FEE_TYPE);

        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo("FEE0412");
        assertThat(result.getDescription()).isEqualTo("Recovery of Land - County Court");
        assertThat(result.getVersion()).isEqualTo(4);
        assertThat(result.getFeeAmount()).isEqualTo(BigDecimal.valueOf(404.00));

        verify(feesClient)
            .lookupFee("default", "issue", new BigDecimal("1"), "PossessionCC");
    }

    @Test
    void shouldThrowFeeNotFoundExceptionWhenFeeTypeNotInConfiguration() {
        when(feesConfiguration.getLookup(FEE_TYPE)).thenReturn(null);

        assertThatThrownBy(() -> feesAndPayService.getFee(FEE_TYPE))
            .isInstanceOf(FeeNotFoundException.class)
            .hasMessageContaining("Fee not found for feeType: " + FEE_TYPE);
    }

    @Test
    void shouldThrowFeeNotFoundExceptionWhenFeignCallFails() {
        when(feesConfiguration.getLookup(FEE_TYPE)).thenReturn(lookUpReferenceData);

        Request request = Request.create(
            Request.HttpMethod.GET,
            "/fees/lookup",
            new HashMap<>(),
            null,
            new RequestTemplate()
        );

        when(feesClient.lookupFee(anyString(), anyString(), any(BigDecimal.class), anyString()))
            .thenThrow(new NotFound("Fee not found", request, null, null));

        assertThatThrownBy(() -> feesAndPayService.getFee(FEE_TYPE))
            .isInstanceOf(FeeNotFoundException.class)
            .hasMessageContaining("Unable to retrieve fee: " + FEE_TYPE)
            .hasCauseInstanceOf(NotFound.class);
    }

    @Test
    void shouldThrowFeeNotFoundExceptionWhenFeignReturnsServerError() {
        when(feesConfiguration.getLookup(FEE_TYPE)).thenReturn(lookUpReferenceData);

        Request request = Request.create(
            Request.HttpMethod.GET,
            "/fees/lookup",
            new HashMap<>(),
            null,
            new RequestTemplate()
        );

        when(feesClient.lookupFee(anyString(), anyString(), any(BigDecimal.class), anyString()))
            .thenThrow(new InternalServerError(
                "Internal server error", request, null, null));

        assertThatThrownBy(() -> feesAndPayService.getFee(FEE_TYPE))
            .isInstanceOf(FeeNotFoundException.class)
            .hasMessageContaining("Unable to retrieve fee: " + FEE_TYPE)
            .hasCauseInstanceOf(InternalServerError.class);
    }

    @Test
    void shouldCreateServiceRequestSuccessfully() {
        String caseReference = "BUS-123";
        String ccdCaseNumber = "1111-2222-3333-4444";
        int volume = 2;
        String responsibleParty = "Applicant";
        String systemToken = "Bearer sys-token";

        FeeDto mappedFee = FeeDto.builder()
            .calculatedAmount(new BigDecimal("808.00"))
            .code("FEE0412")
            .version("4")
            .volume(volume)
            .build();

        CasePaymentRequestDto casePaymentRequestDto = CasePaymentRequestDto.builder()
            .action("payment")
            .responsibleParty(responsibleParty)
            .build();

        PaymentServiceResponse paymentResponse = PaymentServiceResponse.builder()
            .serviceRequestReference("SR-123")
            .build();

        when(paymentRequestMapper.toFeeDto(feeLookupResponseDto, volume)).thenReturn(mappedFee);
        when(paymentRequestMapper.toCasePaymentRequest(responsibleParty))
            .thenReturn(casePaymentRequestDto);
        when(idamService.getSystemUserAuthorisation()).thenReturn(systemToken);
        when(paymentsClient.createServiceRequest(eq(systemToken), any(CreateServiceRequestDTO.class)))
            .thenReturn(paymentResponse);

        PaymentServiceResponse result = feesAndPayService.createServiceRequest(
            caseReference,
            ccdCaseNumber,
            feeLookupResponseDto,
            volume,
            responsibleParty
        );

        assertThat(result).isNotNull();
        assertThat(result.getServiceRequestReference()).isEqualTo("SR-123");

        verify(paymentsClient).createServiceRequest(eq(systemToken), createServiceRequestCaptor.capture());
        CreateServiceRequestDTO sent = createServiceRequestCaptor.getValue();
        assertThat(sent.getCallBackUrl()).isEqualTo("https://callback");
        assertThat(sent.getHmctsOrgId()).isEqualTo("TEST_ORG");
        assertThat(sent.getCaseReference()).isEqualTo(caseReference);
        assertThat(sent.getCcdCaseNumber()).isEqualTo(ccdCaseNumber);
        assertThat(sent.getFees()).isNotNull();
        assertThat(sent.getFees()).hasSize(1);
        assertThat(sent.getFees()[0]).isEqualTo(mappedFee);
        assertThat(sent.getCasePaymentRequest()).isEqualTo(casePaymentRequestDto);
    }
}
