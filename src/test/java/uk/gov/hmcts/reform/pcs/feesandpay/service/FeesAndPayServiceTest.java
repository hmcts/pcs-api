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
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.payments.client.PaymentsClient;
import uk.gov.hmcts.reform.payments.client.models.CasePaymentRequestDto;
import uk.gov.hmcts.reform.payments.client.models.FeeDto;
import uk.gov.hmcts.reform.payments.request.CreateServiceRequestDTO;
import uk.gov.hmcts.reform.payments.response.PaymentServiceResponse;
import uk.gov.hmcts.reform.pcs.feesandpay.api.FeesRegisterApi;
import uk.gov.hmcts.reform.pcs.feesandpay.config.FeesConfiguration;
import uk.gov.hmcts.reform.pcs.feesandpay.config.FeesConfiguration.LookUpReferenceData;
import uk.gov.hmcts.reform.pcs.feesandpay.entity.Fee;
import uk.gov.hmcts.reform.pcs.feesandpay.exception.FeeNotFoundException;
import uk.gov.hmcts.reform.pcs.feesandpay.mapper.PaymentRequestMapper;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeResponse;
import uk.gov.hmcts.reform.pcs.idam.IdamService;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

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
        String SERVICE = "Bearer test-token";
        String IDAM = "idam-token";
    }

    private interface FeeTypes {
        String CASE_ISSUE = "caseIssueFee";
    }

    private interface FeesRegister {
        String SERVICE = "possession claim";
        String JURISDICTION_1 = "civil";
        String JURISDICTION_2 = "county court";
        String CHANNEL = "default";
        String EVENT = "issue";
        String APPLICANT_TYPE = "all";
        String AMOUNT_OR_VOLUME = "1";
        String KEYWORD = "PossessionCC";
    }

    private interface Defaults {
        String HMCTS_ORG_ID = "BBA3";
        String CALLBACK_URL = "https://cb/case/pay";
        String ALT_CALLBACK_URL = "https://env-gateway/callback";
    }

    private interface FeeSamples {
        String CODE = "FEE0412";
        String DESCRIPTION = "Recovery of Land - County Court";
        String VERSION = "4";
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
    private AuthTokenGenerator authTokenGenerator;
    @Mock
    private FeesConfiguration feesConfiguration;
    @Mock
    private FeesRegisterApi feesRegisterApi;
    @Mock
    private IdamService idamService;
    @Mock
    private PaymentsClient paymentsClient;
    @Mock
    private PaymentRequestMapper paymentRequestMapper;

    @InjectMocks
    private FeesAndPayService feesAndPayService;

    private LookUpReferenceData lookup;
    private FeeResponse feeResponse;

    @BeforeEach
    void setUp() {
        lookup = new LookUpReferenceData();
        lookup.setService(FeesRegister.SERVICE);
        lookup.setJurisdiction1(FeesRegister.JURISDICTION_1);
        lookup.setJurisdiction2(FeesRegister.JURISDICTION_2);
        lookup.setChannel(FeesRegister.CHANNEL);
        lookup.setEvent(FeesRegister.EVENT);
        lookup.setApplicantType(FeesRegister.APPLICANT_TYPE);
        lookup.setAmountOrVolume(FeesRegister.AMOUNT_OR_VOLUME);
        lookup.setKeyword(FeesRegister.KEYWORD);

        feeResponse = FeeResponse.builder()
            .code(FeeSamples.CODE)
            .description(FeeSamples.DESCRIPTION)
            .version(FeeSamples.VERSION)
            .feeAmount(FeeSamples.AMOUNT)
            .build();

        setField(feesAndPayService, "callbackUrl", Defaults.CALLBACK_URL);
        setField(feesAndPayService, "hmctsOrgId", Defaults.HMCTS_ORG_ID);
    }

    @Test
    void shouldSuccessfullyGetFee() {
        Map<String, LookUpReferenceData> feesMap = new HashMap<>();
        feesMap.put(FeeTypes.CASE_ISSUE, lookup);

        when(feesConfiguration.getFees()).thenReturn(feesMap);
        when(authTokenGenerator.generate()).thenReturn(Tokens.SERVICE);
        when(feesRegisterApi.lookupFee(
            anyString(), anyString(), anyString(), anyString(), anyString(),
            anyString(), anyString(), anyString(), anyString()
        )).thenReturn(feeResponse);

        Fee result = feesAndPayService.getFee(FeeTypes.CASE_ISSUE);

        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo(FeeSamples.CODE);
        assertThat(result.getDescription()).isEqualTo(FeeSamples.DESCRIPTION);
        assertThat(result.getVersion()).isEqualTo(FeeSamples.VERSION);
        assertThat(result.getCalculatedAmount()).isEqualByComparingTo(FeeSamples.AMOUNT);

        verify(authTokenGenerator).generate();
        verify(feesRegisterApi).lookupFee(
            Tokens.SERVICE,
            FeesRegister.SERVICE,
            FeesRegister.JURISDICTION_1,
            FeesRegister.JURISDICTION_2,
            FeesRegister.CHANNEL,
            FeesRegister.EVENT,
            FeesRegister.APPLICANT_TYPE,
            FeesRegister.AMOUNT_OR_VOLUME,
            FeesRegister.KEYWORD
        );
    }

    @Test
    void shouldThrowFeeNotFoundExceptionWhenFeeTypeNotInConfiguration() {
        Map<String, LookUpReferenceData> emptyFeesMap = new HashMap<>();
        when(feesConfiguration.getFees()).thenReturn(emptyFeesMap);

        assertThatThrownBy(() -> feesAndPayService.getFee(FeeTypes.CASE_ISSUE))
            .isInstanceOf(FeeNotFoundException.class)
            .hasMessageContaining("Fee not found for feeType: " + FeeTypes.CASE_ISSUE);
    }

    @Test
    void shouldThrowFeeNotFoundExceptionWhenFeignCallFails() {
        Map<String, LookUpReferenceData> feesMap = new HashMap<>();
        feesMap.put(FeeTypes.CASE_ISSUE, lookup);

        when(feesConfiguration.getFees()).thenReturn(feesMap);
        when(authTokenGenerator.generate()).thenReturn(Tokens.SERVICE);

        Request request = Request.create(
            GET, "/fees-register/fees/lookup", new HashMap<>(), null, new RequestTemplate()
        );

        when(feesRegisterApi.lookupFee(
            anyString(), anyString(), anyString(), anyString(), anyString(),
            anyString(), anyString(), anyString(), anyString()
        )).thenThrow(new FeignException.NotFound("Fee not found", request, null, null));

        assertThatThrownBy(() -> feesAndPayService.getFee(FeeTypes.CASE_ISSUE))
            .isInstanceOf(FeeNotFoundException.class)
            .hasMessageContaining("Unable to retrieve fee: " + FeeTypes.CASE_ISSUE)
            .hasCauseInstanceOf(FeignException.NotFound.class);
    }

    @Test
    void shouldThrowFeeNotFoundExceptionWhenFeignReturnsServerError() {
        Map<String, LookUpReferenceData> feesMap = new HashMap<>();
        feesMap.put(FeeTypes.CASE_ISSUE, lookup);

        when(feesConfiguration.getFees()).thenReturn(feesMap);
        when(authTokenGenerator.generate()).thenReturn(Tokens.SERVICE);

        Request request = Request.create(
            GET, "/fees-register/fees/lookup", new HashMap<>(), null, new RequestTemplate()
        );

        when(feesRegisterApi.lookupFee(
            anyString(), anyString(), anyString(), anyString(), anyString(),
            anyString(), anyString(), anyString(), anyString()
        )).thenThrow(new FeignException.InternalServerError(
            "Internal server error", request, null, null));

        assertThatThrownBy(() -> feesAndPayService.getFee(FeeTypes.CASE_ISSUE))
            .isInstanceOf(FeeNotFoundException.class)
            .hasMessageContaining("Unable to retrieve fee: " + FeeTypes.CASE_ISSUE)
            .hasCauseInstanceOf(FeignException.InternalServerError.class);
    }

    @Test
    void shouldMapFeeResponseToFeeCorrectly() {
        Map<String, LookUpReferenceData> feesMap = new HashMap<>();
        feesMap.put(FeeTypes.CASE_ISSUE, lookup);

        FeeResponse custom = FeeResponse.builder()
            .code("FEE9999")
            .description("Custom Fee")
            .version("1")
            .feeAmount(new BigDecimal("100.50"))
            .build();

        when(feesConfiguration.getFees()).thenReturn(feesMap);
        when(authTokenGenerator.generate()).thenReturn(Tokens.SERVICE);
        when(feesRegisterApi.lookupFee(
            anyString(), anyString(), anyString(), anyString(), anyString(),
            anyString(), anyString(), anyString(), anyString()
        )).thenReturn(custom);

        Fee result = feesAndPayService.getFee(FeeTypes.CASE_ISSUE);

        assertThat(result.getCode()).isEqualTo(custom.getCode());
        assertThat(result.getDescription()).isEqualTo(custom.getDescription());
        assertThat(result.getVersion()).isEqualTo(custom.getVersion());
        assertThat(result.getCalculatedAmount()).isEqualByComparingTo(custom.getFeeAmount());
    }

    @Test
    void createServiceRequest_shouldCallApisAndReturnResponse() {
        String caseReference = "CR";
        String ccdCaseNumber = "CCD";
        Fee fee = Fee.builder()
            .code("X").description("desc").version("1")
            .calculatedAmount(new BigDecimal("10.00")).build();
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
        Fee fee = Fee.builder().calculatedAmount(new BigDecimal("1.00")).build();
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

        Fee fee = Fee.builder().calculatedAmount(new BigDecimal("1.00")).build();
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
        Fee fee = Fee.builder().calculatedAmount(new BigDecimal("1.00")).build();
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

        Fee fee = Fee.builder().calculatedAmount(new BigDecimal("1.00")).build();
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
        String caseReference = "CR";
        String ccdCaseNumber = "CCD";
        Fee fee = Fee.builder().calculatedAmount(new BigDecimal("1.00")).build();
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

        assertThatThrownBy(() -> feesAndPayService.createServiceRequest(
            caseReference, ccdCaseNumber, fee, volume, responsibleParty)).isSameAs(ex);
    }

    @Test
    void createServiceRequest_shouldRethrowGenericExceptions() {
        String caseReference = "CR";
        String ccdCaseNumber = "CCD";
        Fee fee = Fee.builder().calculatedAmount(new BigDecimal("1.00")).build();
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

        Fee fee = Fee.builder().calculatedAmount(new BigDecimal("1.00")).build();
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

        Fee fee = Fee.builder().calculatedAmount(new BigDecimal("11.00")).build();
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
