package uk.gov.hmcts.reform.pcs.feesandpay.service;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

import static feign.Request.HttpMethod.GET;
import static feign.Request.HttpMethod.POST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@ExtendWith(MockitoExtension.class)
class FeesAndPayServiceTest {

    private interface Tokens {
        String IDAM = "idam-token";
    }

    private interface FeeTypes {
        String CASE_ISSUE = "caseIssued";
    }

    private interface FeesRegister {
        String CHANNEL = "default";
        String EVENT = "issue";
    }

    private interface Defaults {
        String HMCTS_ORG_ID = "BBA3";
        String CALLBACK_URL = "https://cb/case/pay";
        String ALT_CALLBACK_URL = "https://env-gateway/callback";
    }

    private interface FeeSamples {
        String CODE = "FEE0412";
        String DESCRIPTION = "Recovery of Land - County Court";
        Integer VERSION = 4;
        BigDecimal AMOUNT = new BigDecimal("404.00");
    }

    private enum Action {
        PAYMENT("payment");
        private final String value;
        Action(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    @Mock
    private FeesConfiguration feesConfiguration;
    @Mock
    private FeesClient feesClient;
    @Mock
    private IdamService idamService;
    @Mock
    private PaymentsClient paymentsClient;
    @Mock
    private PaymentRequestMapper paymentRequestMapper;

    @InjectMocks
    private FeesAndPayService feesAndPayService;

    private LookUpReferenceData lookup;
    private FeeLookupResponseDto feeResponse;

    @BeforeEach
    void setUp() {
        lookup = new LookUpReferenceData();
        lookup.setChannel(FeesRegister.CHANNEL);
        lookup.setEvent(FeesRegister.EVENT);
        lookup.setAmountOrVolume(FeeSamples.AMOUNT);

        feeResponse = new FeeLookupResponseDto();
        feeResponse.setCode(FeeSamples.CODE);
        feeResponse.setDescription(FeeSamples.DESCRIPTION);
        feeResponse.setVersion(FeeSamples.VERSION);
        feeResponse.setFeeAmount(FeeSamples.AMOUNT);

        setField(feesAndPayService, "callbackUrl", Defaults.CALLBACK_URL);
        setField(feesAndPayService, "hmctsOrgId", Defaults.HMCTS_ORG_ID);
    }

    @Test
    void shouldSuccessfullyGetFee() {
        when(feesConfiguration.getLookup(FeeTypes.CASE_ISSUE)).thenReturn(lookup);
        when(feesClient.lookupFee(
            FeesRegister.CHANNEL,
            FeesRegister.EVENT,
            FeeSamples.AMOUNT
        )).thenReturn(feeResponse);

        FeeLookupResponseDto result = feesAndPayService.getFee(FeeTypes.CASE_ISSUE);

        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo(FeeSamples.CODE);
        assertThat(result.getDescription()).isEqualTo(FeeSamples.DESCRIPTION);
        assertThat(result.getVersion()).isEqualTo(FeeSamples.VERSION);
        assertThat(result.getFeeAmount()).isEqualByComparingTo(FeeSamples.AMOUNT);

        verify(feesClient).lookupFee(
            FeesRegister.CHANNEL,
            FeesRegister.EVENT,
            FeeSamples.AMOUNT
        );
    }

    @Test
    void shouldThrowFeeNotFoundExceptionWhenFeeTypeNotInConfiguration() {
        when(feesConfiguration.getLookup(FeeTypes.CASE_ISSUE)).thenReturn(null);

        assertThatThrownBy(() -> feesAndPayService.getFee(FeeTypes.CASE_ISSUE))
            .isInstanceOf(FeeNotFoundException.class)
            .hasMessageContaining("Fee not found for feeType: " + FeeTypes.CASE_ISSUE);
    }

    @Test
    void shouldThrowFeeNotFoundExceptionWhenFeignCallFails() {
        when(feesConfiguration.getLookup(FeeTypes.CASE_ISSUE)).thenReturn(lookup);

        Request request = Request.create(
            GET, "/fees-register/fees/lookup", new HashMap<>(), null, new RequestTemplate()
        );

        when(feesClient.lookupFee(
            FeesRegister.CHANNEL,
            FeesRegister.EVENT,
            FeeSamples.AMOUNT
        )).thenThrow(new FeignException.NotFound("Fee not found", request, null, null));

        assertThatThrownBy(() -> feesAndPayService.getFee(FeeTypes.CASE_ISSUE))
            .isInstanceOf(FeeNotFoundException.class)
            .hasMessageContaining("Unable to retrieve fee: " + FeeTypes.CASE_ISSUE)
            .hasCauseInstanceOf(FeignException.NotFound.class);
    }

    @Test
    void shouldThrowFeeNotFoundExceptionWhenFeignReturnsServerError() {
        when(feesConfiguration.getLookup(FeeTypes.CASE_ISSUE)).thenReturn(lookup);

        Request request = Request.create(
            GET, "/fees-register/fees/lookup", new HashMap<>(), null, new RequestTemplate()
        );

        when(feesClient.lookupFee(
            FeesRegister.CHANNEL,
            FeesRegister.EVENT,
            FeeSamples.AMOUNT
        )).thenThrow(new FeignException.InternalServerError(
            "Internal server error", request, null, null));

        assertThatThrownBy(() -> feesAndPayService.getFee(FeeTypes.CASE_ISSUE))
            .isInstanceOf(FeeNotFoundException.class)
            .hasMessageContaining("Unable to retrieve fee: " + FeeTypes.CASE_ISSUE)
            .hasCauseInstanceOf(FeignException.InternalServerError.class);
    }

    @Test
    void shouldMapFeeResponseCorrectly() {
        FeeLookupResponseDto custom = new FeeLookupResponseDto();
        custom.setCode("FEE9999");
        custom.setDescription("Custom Fee");
        custom.setVersion(1);
        custom.setFeeAmount(new BigDecimal("100.50"));

        when(feesConfiguration.getLookup(FeeTypes.CASE_ISSUE)).thenReturn(lookup);
        when(feesClient.lookupFee(
            FeesRegister.CHANNEL,
            FeesRegister.EVENT,
            FeeSamples.AMOUNT
        )).thenReturn(custom);

        FeeLookupResponseDto result = feesAndPayService.getFee(FeeTypes.CASE_ISSUE);

        assertThat(result.getCode()).isEqualTo(custom.getCode());
        assertThat(result.getDescription()).isEqualTo(custom.getDescription());
        assertThat(result.getVersion()).isEqualTo(custom.getVersion());
        assertThat(result.getFeeAmount()).isEqualByComparingTo(custom.getFeeAmount());
    }

    @Test
    void createServiceRequest_shouldCallApisAndReturnResponse() {
        FeeLookupResponseDto fee = new FeeLookupResponseDto();
        fee.setCode("X");
        fee.setDescription("desc");
        fee.setVersion(1);
        fee.setFeeAmount(new BigDecimal("10.00"));
        int volume = 2;
        String responsibleParty = "RP";

        FeeDto feeDto = FeeDto.builder().calculatedAmount(new BigDecimal("10.00")).build();
        CasePaymentRequestDto casePaymentRequestDto = CasePaymentRequestDto.builder()
            .action(Action.PAYMENT.value()).responsibleParty("RP").build();

        PaymentServiceResponse response = PaymentServiceResponse.builder()
            .serviceRequestReference("SR-123").build();

        when(paymentRequestMapper.toFeeDto(fee, volume)).thenReturn(feeDto);
        when(paymentRequestMapper.toCasePaymentRequest(Action.PAYMENT.value(), responsibleParty))
            .thenReturn(casePaymentRequestDto);
        when(idamService.getSystemUserAuthorisation()).thenReturn(Tokens.IDAM);

        ArgumentCaptor<CreateServiceRequestDTO> dtoCaptor = ArgumentCaptor.forClass(CreateServiceRequestDTO.class);
        when(paymentsClient.createServiceRequest(anyString(), dtoCaptor.capture())).thenReturn(response);

        String caseReference = "CR";
        String ccdCaseNumber = "CCD";

        PaymentServiceResponse result = feesAndPayService.createServiceRequest(
            caseReference, ccdCaseNumber, fee, volume, responsibleParty);

        assertThat(result).isSameAs(response);

        verify(paymentRequestMapper).toFeeDto(fee, volume);
        verify(paymentRequestMapper).toCasePaymentRequest(Action.PAYMENT.value(), responsibleParty);
        verify(paymentsClient).createServiceRequest(anyString(), any(CreateServiceRequestDTO.class));

        CreateServiceRequestDTO sent = dtoCaptor.getValue();
        assertThat(sent.getCallBackUrl()).isEqualTo(Defaults.CALLBACK_URL);
        assertThat(sent.getCaseReference()).isEqualTo(caseReference);
        assertThat(sent.getCcdCaseNumber()).isEqualTo(ccdCaseNumber);
        assertThat(sent.getCasePaymentRequest()).isEqualTo(casePaymentRequestDto);
        assertThat(sent.getFees()).hasSize(1);
        assertThat(sent.getFees()[0]).isEqualTo(feeDto);
        assertThat(sent.getHmctsOrgId()).isEqualTo(Defaults.HMCTS_ORG_ID);
        assertThat(sent.getCasePaymentRequest().getAction()).isEqualTo(Action.PAYMENT.value());
    }

    @Test
    void createServiceRequest_shouldPassIdamTokenToClient() {
        FeeLookupResponseDto fee = new FeeLookupResponseDto();
        fee.setFeeAmount(new BigDecimal("1.00"));
        when(paymentRequestMapper.toFeeDto(fee, 1)).thenReturn(FeeDto.builder().build());
        when(paymentRequestMapper.toCasePaymentRequest(Action.PAYMENT.value(), "RP"))
            .thenReturn(CasePaymentRequestDto.builder().build());
        when(idamService.getSystemUserAuthorisation()).thenReturn(Tokens.IDAM);
        when(paymentsClient.createServiceRequest(anyString(), any(CreateServiceRequestDTO.class)))
            .thenReturn(PaymentServiceResponse.builder().serviceRequestReference("SR").build());

        feesAndPayService.createServiceRequest(
            "CR", "CCD", fee, 1, "RP");

        verify(paymentsClient).createServiceRequest(eq(Tokens.IDAM), any(CreateServiceRequestDTO.class));
    }

    @Test
    void createServiceRequest_shouldAllowNullCallbackUrl() {
        setField(feesAndPayService, "callbackUrl", null);
        setField(feesAndPayService, "hmctsOrgId", Defaults.HMCTS_ORG_ID);

        FeeLookupResponseDto fee = new FeeLookupResponseDto();
        fee.setFeeAmount(new BigDecimal("1.00"));
        FeeDto feeDto = FeeDto.builder().build();
        CasePaymentRequestDto casePaymentRequestDto = CasePaymentRequestDto.builder().build();

        when(paymentRequestMapper.toFeeDto(fee, 1)).thenReturn(feeDto);
        when(paymentRequestMapper.toCasePaymentRequest(Action.PAYMENT.value(), "RP"))
            .thenReturn(casePaymentRequestDto);
        when(idamService.getSystemUserAuthorisation()).thenReturn(Tokens.IDAM);

        ArgumentCaptor<CreateServiceRequestDTO> dtoCaptor = ArgumentCaptor.forClass(CreateServiceRequestDTO.class);
        when(paymentsClient.createServiceRequest(anyString(), dtoCaptor.capture()))
            .thenReturn(PaymentServiceResponse.builder().serviceRequestReference("SR").build());

        feesAndPayService.createServiceRequest(
            "CR", "CCD", fee, 1, "RP");

        assertThat(dtoCaptor.getValue().getCallBackUrl()).isNull();
        assertThat(dtoCaptor.getValue().getHmctsOrgId()).isEqualTo(Defaults.HMCTS_ORG_ID);
    }

    @Test
    void createServiceRequest_shouldPropagateDifferentResponsibleParty() {
        FeeLookupResponseDto fee = new FeeLookupResponseDto();
        fee.setFeeAmount(new BigDecimal("1.00"));
        String party = "Defendant";
        FeeDto feeDto = FeeDto.builder().build();
        CasePaymentRequestDto casePaymentRequestDto = CasePaymentRequestDto.builder().build();

        when(paymentRequestMapper.toFeeDto(fee, 1)).thenReturn(feeDto);
        when(paymentRequestMapper.toCasePaymentRequest(Action.PAYMENT.value(), party))
            .thenReturn(casePaymentRequestDto);
        when(idamService.getSystemUserAuthorisation()).thenReturn(Tokens.IDAM);

        ArgumentCaptor<CreateServiceRequestDTO> dtoCaptor = ArgumentCaptor.forClass(CreateServiceRequestDTO.class);
        when(paymentsClient.createServiceRequest(anyString(), dtoCaptor.capture()))
            .thenReturn(PaymentServiceResponse.builder().serviceRequestReference("SR").build());

        feesAndPayService.createServiceRequest("CR", "CCD", fee, 1, party);

        assertThat(dtoCaptor.getValue().getCasePaymentRequest()).isEqualTo(casePaymentRequestDto);
    }

    @Test
    void createServiceRequest_shouldAllowEmptyHmctsOrgId() {
        setField(feesAndPayService, "hmctsOrgId", "");

        FeeLookupResponseDto fee = new FeeLookupResponseDto();
        fee.setFeeAmount(new BigDecimal("1.00"));
        FeeDto feeDto = FeeDto.builder().build();
        CasePaymentRequestDto casePaymentRequestDto = CasePaymentRequestDto.builder().build();

        when(paymentRequestMapper.toFeeDto(fee, 1)).thenReturn(feeDto);
        when(paymentRequestMapper.toCasePaymentRequest(Action.PAYMENT.value(), "RP"))
            .thenReturn(casePaymentRequestDto);
        when(idamService.getSystemUserAuthorisation()).thenReturn(Tokens.IDAM);

        ArgumentCaptor<CreateServiceRequestDTO> dtoCaptor = ArgumentCaptor.forClass(CreateServiceRequestDTO.class);
        when(paymentsClient.createServiceRequest(anyString(), dtoCaptor.capture()))
            .thenReturn(PaymentServiceResponse.builder().serviceRequestReference("SR").build());

        feesAndPayService.createServiceRequest("CR", "CCD", fee, 1, "RP");

        assertThat(dtoCaptor.getValue().getHmctsOrgId()).isEmpty();
    }

    @Test
    void createServiceRequest_shouldRethrowFeignExceptionAndLog() {
        FeeLookupResponseDto fee = new FeeLookupResponseDto();
        fee.setFeeAmount(new BigDecimal("1.00"));
        int volume = 1;
        String responsibleParty = "RP";

        FeeDto feeDto = FeeDto.builder().build();
        CasePaymentRequestDto casePaymentRequestDto = CasePaymentRequestDto.builder().build();

        when(paymentRequestMapper.toFeeDto(fee, volume)).thenReturn(feeDto);
        when(paymentRequestMapper.toCasePaymentRequest(Action.PAYMENT.value(), responsibleParty))
            .thenReturn(casePaymentRequestDto);
        when(idamService.getSystemUserAuthorisation()).thenReturn(Tokens.IDAM);

        Request req = Request.create(POST, "/payments", new HashMap<>(), null, new RequestTemplate());
        FeignException ex = new FeignException.BadRequest("bad", req, "err".getBytes(), null);

        when(paymentsClient.createServiceRequest(anyString(), any(CreateServiceRequestDTO.class))).thenThrow(ex);

        String caseReference = "CR";
        String ccdCaseNumber = "CCD";

        assertThatThrownBy(() -> feesAndPayService.createServiceRequest(
            caseReference, ccdCaseNumber, fee, volume, responsibleParty)).isSameAs(ex);
    }

    @Test
    void createServiceRequest_shouldRethrowGenericExceptions() {
        String caseReference = "CR";
        String ccdCaseNumber = "CCD";
        FeeLookupResponseDto fee = new FeeLookupResponseDto();
        fee.setFeeAmount(new BigDecimal("1.00"));
        int volume = 1;
        String responsibleParty = "RP";

        when(paymentRequestMapper.toFeeDto(fee, volume)).thenThrow(new IllegalStateException("mapper failed"));

        assertThatThrownBy(() -> feesAndPayService.createServiceRequest(
            caseReference, ccdCaseNumber, fee, volume, responsibleParty))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("mapper failed");
    }

    @Test
    void createServiceRequest_shouldUseConfiguredCallbackUrl() {
        setField(feesAndPayService, "callbackUrl", Defaults.ALT_CALLBACK_URL);
        setField(feesAndPayService, "hmctsOrgId", Defaults.HMCTS_ORG_ID);

        FeeLookupResponseDto fee = new FeeLookupResponseDto();
        fee.setFeeAmount(new BigDecimal("1.00"));
        int volume = 3;
        String responsibleParty = "Resp";

        FeeDto feeDto = FeeDto.builder().build();
        CasePaymentRequestDto casePaymentRequestDto = CasePaymentRequestDto.builder().build();

        when(paymentRequestMapper.toFeeDto(fee, volume)).thenReturn(feeDto);
        when(paymentRequestMapper.toCasePaymentRequest(Action.PAYMENT.value(), responsibleParty))
            .thenReturn(casePaymentRequestDto);
        when(idamService.getSystemUserAuthorisation()).thenReturn(Tokens.IDAM);

        ArgumentCaptor<CreateServiceRequestDTO> dtoCaptor = ArgumentCaptor.forClass(CreateServiceRequestDTO.class);
        when(paymentsClient.createServiceRequest(anyString(), dtoCaptor.capture()))
            .thenReturn(PaymentServiceResponse.builder().serviceRequestReference("SR").build());

        feesAndPayService.createServiceRequest("CR", "CCD", fee, volume, responsibleParty);

        CreateServiceRequestDTO sent = dtoCaptor.getValue();
        assertThat(sent.getCallBackUrl()).isEqualTo(Defaults.ALT_CALLBACK_URL);
        assertThat(sent.getHmctsOrgId()).isEqualTo(Defaults.HMCTS_ORG_ID);
    }

    @Test
    void createServiceRequest_shouldPassVolumeToMapper() {
        setField(feesAndPayService, "hmctsOrgId", Defaults.HMCTS_ORG_ID);

        FeeLookupResponseDto fee = new FeeLookupResponseDto();
        fee.setFeeAmount(new BigDecimal("11.00"));
        int volume = 3;

        FeeDto feeDto = FeeDto.builder().calculatedAmount(new BigDecimal("33.00")).build();
        when(paymentRequestMapper.toFeeDto(fee, volume)).thenReturn(feeDto);
        when(paymentRequestMapper.toCasePaymentRequest(Action.PAYMENT.value(), "RP"))
            .thenReturn(CasePaymentRequestDto.builder().build());
        when(idamService.getSystemUserAuthorisation()).thenReturn(Tokens.IDAM);
        when(paymentsClient.createServiceRequest(anyString(), any(CreateServiceRequestDTO.class)))
            .thenReturn(PaymentServiceResponse.builder().serviceRequestReference("SR").build());

        feesAndPayService.createServiceRequest(
            "CR", "CCD", fee, volume, "RP");

        verify(paymentRequestMapper).toFeeDto(fee, volume);
    }
}
