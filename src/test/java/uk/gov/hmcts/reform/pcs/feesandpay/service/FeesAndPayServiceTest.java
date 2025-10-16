package uk.gov.hmcts.reform.pcs.feesandpay.service;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.pcs.feesandpay.api.FeesRegisterApi;
import uk.gov.hmcts.reform.pcs.feesandpay.api.PaymentApi;
import uk.gov.hmcts.reform.pcs.feesandpay.config.FeesConfiguration;
import uk.gov.hmcts.reform.pcs.feesandpay.config.FeesConfiguration.LookUpReferenceData;
import uk.gov.hmcts.reform.pcs.feesandpay.dto.CasePaymentRequestDto;
import uk.gov.hmcts.reform.pcs.feesandpay.dto.FeeDto;
import uk.gov.hmcts.reform.pcs.feesandpay.entity.Fee;
import uk.gov.hmcts.reform.pcs.feesandpay.exception.FeeNotFoundException;
import uk.gov.hmcts.reform.pcs.feesandpay.mapper.PaymentRequestMapper;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeResponse;
import uk.gov.hmcts.reform.pcs.feesandpay.model.ServiceRequestBody;
import uk.gov.hmcts.reform.pcs.feesandpay.model.ServiceRequestResponse;
import uk.gov.hmcts.reform.pcs.idam.IdamService;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeesAndPayServiceTest {

    @Mock
    private AuthTokenGenerator authTokenGenerator;

    @Mock
    private FeesConfiguration feesConfiguration;

    @Mock
    private FeesRegisterApi feesRegisterApi;

    @Mock
    private IdamService idamService;

    @Mock
    private PaymentApi paymentApi;

    @Mock
    private PaymentRequestMapper paymentRequestMapper;

    @InjectMocks
    private FeesAndPayService feesAndPayService;

    private static final String SERVICE_AUTH_TOKEN = "Bearer test-token";
    private static final String FEE_TYPE = "caseIssueFee";

    private LookUpReferenceData lookUpReferenceData;
    private FeeResponse feeResponse;

    @BeforeEach
    void setUp() {
        lookUpReferenceData = new LookUpReferenceData();
        lookUpReferenceData.setService("possession claim");
        lookUpReferenceData.setJurisdiction1("civil");
        lookUpReferenceData.setJurisdiction2("county court");
        lookUpReferenceData.setChannel("default");
        lookUpReferenceData.setEvent("issue");
        lookUpReferenceData.setApplicantType("all");
        lookUpReferenceData.setAmountOrVolume("1");
        lookUpReferenceData.setKeyword("PossessionCC");

        feeResponse = FeeResponse.builder()
            .code("FEE0412")
            .description("Recovery of Land - County Court")
            .version("4")
            .feeAmount(new BigDecimal("404.00"))
            .build();

        ReflectionTestUtils.setField(feesAndPayService, "callbackUrl", "https://cb/case/pay");
    }

    @Test
    void shouldSuccessfullyGetFee() {
        Map<String, LookUpReferenceData> feesMap = new HashMap<>();
        feesMap.put(FEE_TYPE, lookUpReferenceData);

        when(feesConfiguration.getFees()).thenReturn(feesMap);
        when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTH_TOKEN);
        when(feesRegisterApi.lookupFee(
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString()
        )).thenReturn(feeResponse);

        Fee result = feesAndPayService.getFee(FEE_TYPE);

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getCode()).isEqualTo("FEE0412");
        Assertions.assertThat(result.getDescription()).isEqualTo("Recovery of Land - County Court");
        Assertions.assertThat(result.getVersion()).isEqualTo("4");
        Assertions.assertThat(result.getCalculatedAmount()).isEqualByComparingTo(new BigDecimal("404.00"));

        verify(authTokenGenerator).generate();
        verify(feesRegisterApi).lookupFee(
            SERVICE_AUTH_TOKEN,
            "possession claim",
            "civil",
            "county court",
            "default",
            "issue",
            "all",
            "1",
            "PossessionCC"
        );
    }

    @Test
    void shouldThrowFeeNotFoundExceptionWhenFeeTypeNotInConfiguration() {
        Map<String, LookUpReferenceData> emptyFeesMap = new HashMap<>();
        when(feesConfiguration.getFees()).thenReturn(emptyFeesMap);

        Assertions.assertThatThrownBy(() -> feesAndPayService.getFee(FEE_TYPE))
            .isInstanceOf(FeeNotFoundException.class)
            .hasMessageContaining("Fee not found for feeType: " + FEE_TYPE);
    }

    @Test
    void shouldThrowFeeNotFoundExceptionWhenFeignCallFails() {
        Map<String, LookUpReferenceData> feesMap = new HashMap<>();
        feesMap.put(FEE_TYPE, lookUpReferenceData);

        when(feesConfiguration.getFees()).thenReturn(feesMap);
        when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTH_TOKEN);

        Request request = Request.create(
            Request.HttpMethod.GET,
            "/fees-register/fees/lookup",
            new HashMap<>(),
            null,
            new RequestTemplate()
        );

        when(feesRegisterApi.lookupFee(
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString()
        )).thenThrow(new FeignException.NotFound("Fee not found", request, null, null));

        Assertions.assertThatThrownBy(() -> feesAndPayService.getFee(FEE_TYPE))
            .isInstanceOf(FeeNotFoundException.class)
            .hasMessageContaining("Unable to retrieve fee: " + FEE_TYPE)
            .hasCauseInstanceOf(FeignException.NotFound.class);
    }

    @Test
    void shouldThrowFeeNotFoundExceptionWhenFeignReturnsServerError() {
        Map<String, LookUpReferenceData> feesMap = new HashMap<>();
        feesMap.put(FEE_TYPE, lookUpReferenceData);

        when(feesConfiguration.getFees()).thenReturn(feesMap);
        when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTH_TOKEN);

        Request request = Request.create(
            Request.HttpMethod.GET,
            "/fees-register/fees/lookup",
            new HashMap<>(),
            null,
            new RequestTemplate()
        );

        when(feesRegisterApi.lookupFee(
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString()
        )).thenThrow(new FeignException.InternalServerError(
            "Internal server error",
            request,
            null,
            null
        ));

        Assertions.assertThatThrownBy(() -> feesAndPayService.getFee(FEE_TYPE))
            .isInstanceOf(FeeNotFoundException.class)
            .hasMessageContaining("Unable to retrieve fee: " + FEE_TYPE)
            .hasCauseInstanceOf(FeignException.InternalServerError.class);
    }

    @Test
    void shouldMapFeeResponseToFeeCorrectly() {
        Map<String, LookUpReferenceData> feesMap = new HashMap<>();
        feesMap.put(FEE_TYPE, lookUpReferenceData);

        FeeResponse customFeeResponse = FeeResponse.builder()
            .code("FEE9999")
            .description("Custom Fee")
            .version("1")
            .feeAmount(new BigDecimal("100.50"))
            .build();

        when(feesConfiguration.getFees()).thenReturn(feesMap);
        when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTH_TOKEN);
        when(feesRegisterApi.lookupFee(
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString()
        )).thenReturn(customFeeResponse);

        Fee result = feesAndPayService.getFee(FEE_TYPE);

        Assertions.assertThat(result.getCode()).isEqualTo(customFeeResponse.getCode());
        Assertions.assertThat(result.getDescription()).isEqualTo(customFeeResponse.getDescription());
        Assertions.assertThat(result.getVersion()).isEqualTo(customFeeResponse.getVersion());
        Assertions.assertThat(result.getCalculatedAmount()).isEqualByComparingTo(customFeeResponse.getFeeAmount());
    }

    @Test
    void createServiceRequest_shouldCallApisAndReturnResponse() {
        String caseReference = "CR";
        String ccdCaseNumber = "CCD";
        Fee fee = Fee.builder().code("X").description("desc").version("1").calculatedAmount(
            new BigDecimal("10.00")).build();
        int volume = 2;
        String responsibleParty = "RP";
        String s2s = "s2s-token";
        String idam = "idam-token";

        FeeDto feeDto = FeeDto.builder().calculatedAmount(new BigDecimal("10.00")).build();
        CasePaymentRequestDto cprd = CasePaymentRequestDto.builder().action("payment").responsibleParty("RP").build();
        ServiceRequestBody requestBody = ServiceRequestBody.builder()
            .callbackUrl("https://cb/case/pay")
            .caseReference(caseReference)
            .ccdCaseNumber(ccdCaseNumber)
            .fees(new FeeDto[]{feeDto})
            .casePaymentRequest(cprd)
            .build();
        ServiceRequestResponse response = ServiceRequestResponse.builder().serviceRequestReference("SR-123").build();

        when(paymentRequestMapper.toFeeDto(fee, volume)).thenReturn(feeDto);
        when(paymentRequestMapper.toCasePaymentRequest("payment", responsibleParty)).thenReturn(cprd);
        when(paymentRequestMapper.toServiceRequestBody(
            "https://cb/case/pay", caseReference, ccdCaseNumber, new FeeDto[]{feeDto}, cprd))
            .thenReturn(requestBody);
        when(idamService.getSystemUserAuthorisation()).thenReturn(idam);
        when(authTokenGenerator.generate()).thenReturn(s2s);
        when(paymentApi.createServiceRequest(idam, s2s, requestBody)).thenReturn(response);

        ServiceRequestResponse result = feesAndPayService.createServiceRequest(
            caseReference, ccdCaseNumber, fee, volume, responsibleParty);

        Assertions.assertThat(result).isSameAs(response);
        verify(paymentRequestMapper).toFeeDto(fee, volume);
        verify(paymentRequestMapper).toCasePaymentRequest("payment", responsibleParty);
        verify(paymentRequestMapper).toServiceRequestBody(
            "https://cb/case/pay", caseReference, ccdCaseNumber, new FeeDto[]{feeDto}, cprd);
        verify(paymentApi).createServiceRequest(idam, s2s, requestBody);
    }

    @Test
    void createServiceRequest_shouldRethrowFeignExceptionAndLog() {
        String caseReference = "CR";
        String ccdCaseNumber = "CCD";
        Fee fee = Fee.builder().calculatedAmount(new BigDecimal("1.00")).build();
        int volume = 1;
        String responsibleParty = "RP";

        FeeDto feeDto = FeeDto.builder().build();
        CasePaymentRequestDto cprd = CasePaymentRequestDto.builder().build();
        ServiceRequestBody body = ServiceRequestBody.builder().build();

        when(paymentRequestMapper.toFeeDto(fee, volume)).thenReturn(feeDto);
        when(paymentRequestMapper.toCasePaymentRequest("payment", responsibleParty)).thenReturn(cprd);
        when(paymentRequestMapper.toServiceRequestBody(
            "https://cb/case/pay", caseReference, ccdCaseNumber, new FeeDto[]{feeDto}, cprd))
            .thenReturn(body);
        when(idamService.getSystemUserAuthorisation()).thenReturn("idam");
        when(authTokenGenerator.generate()).thenReturn("s2s");

        Request req = Request.create(
            Request.HttpMethod.POST, "/payments", new HashMap<>(), null, new RequestTemplate()
        );
        FeignException ex = new FeignException.BadRequest("bad", req, "err".getBytes(), null);
        when(paymentApi.createServiceRequest("idam", "s2s", body)).thenThrow(ex);

        Assertions.assertThatThrownBy(() -> feesAndPayService.createServiceRequest(
            caseReference, ccdCaseNumber, fee, volume, responsibleParty))
            .isSameAs(ex);
    }

    @Test
    void createServiceRequest_shouldRethrowGenericExceptions() {
        String caseReference = "CR";
        String ccdCaseNumber = "CCD";
        Fee fee = Fee.builder().calculatedAmount(new BigDecimal("1.00")).build();
        int volume = 1;
        String responsibleParty = "RP";

        when(paymentRequestMapper.toFeeDto(fee, volume)).thenThrow(new IllegalStateException("mapper failed"));

        Assertions.assertThatThrownBy(() -> feesAndPayService.createServiceRequest(
            caseReference, ccdCaseNumber, fee, volume, responsibleParty))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("mapper failed");
    }

    @Test
    void createServiceRequest_shouldUseConfiguredCallbackUrl() {
        String cb = "https://env-gateway/callback";
        ReflectionTestUtils.setField(feesAndPayService, "callbackUrl", cb);

        String caseReference = "CR";
        String ccdCaseNumber = "CCD";
        Fee fee = Fee.builder().calculatedAmount(new BigDecimal("1.00")).build();
        int volume = 3;
        String responsibleParty = "Resp";

        FeeDto feeDto = FeeDto.builder().build();
        CasePaymentRequestDto cprd = CasePaymentRequestDto.builder().build();
        ServiceRequestBody body = ServiceRequestBody.builder().build();

        when(paymentRequestMapper.toFeeDto(fee, volume)).thenReturn(feeDto);
        when(paymentRequestMapper.toCasePaymentRequest("payment", responsibleParty)).thenReturn(cprd);
        when(paymentRequestMapper.toServiceRequestBody(
            cb, caseReference, ccdCaseNumber, new FeeDto[]{feeDto}, cprd)).thenReturn(body);
        when(idamService.getSystemUserAuthorisation()).thenReturn("idam");
        when(authTokenGenerator.generate()).thenReturn("s2s");
        when(paymentApi.createServiceRequest("idam", "s2s", body))
            .thenReturn(ServiceRequestResponse.builder().serviceRequestReference("SR").build());

        feesAndPayService.createServiceRequest(caseReference, ccdCaseNumber, fee, volume, responsibleParty);

        verify(paymentRequestMapper).toServiceRequestBody(cb, caseReference, ccdCaseNumber, new FeeDto[]{feeDto}, cprd);
    }

    @Test
    void createServiceRequest_shouldPassVolumeToMapper() {
        Fee fee = Fee.builder().calculatedAmount(new BigDecimal("11.00")).build();
        int volume = 3;

        FeeDto feeDto = FeeDto.builder().calculatedAmount(new BigDecimal("33.00")).build();
        when(paymentRequestMapper.toFeeDto(fee, volume)).thenReturn(feeDto);
        when(paymentRequestMapper.toCasePaymentRequest("payment", "RP"))
            .thenReturn(CasePaymentRequestDto.builder().build());
        when(paymentRequestMapper.toServiceRequestBody(anyString(), anyString(), anyString(), any(), any()))
            .thenReturn(ServiceRequestBody.builder().build());
        when(idamService.getSystemUserAuthorisation()).thenReturn("idam");
        when(authTokenGenerator.generate()).thenReturn("s2s");
        when(paymentApi.createServiceRequest(anyString(), anyString(), any()))
            .thenReturn(ServiceRequestResponse.builder().serviceRequestReference("SR").build());

        feesAndPayService.createServiceRequest(
            "CR", "CCD", fee, volume, "RP");

        verify(paymentRequestMapper).toFeeDto(fee, volume);
    }
}
