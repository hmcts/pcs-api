package uk.gov.hmcts.reform.pcs.feesandpay.service;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.pcs.feesandpay.api.FeesRegisterApi;
import uk.gov.hmcts.reform.pcs.feesandpay.config.FeesConfiguration;
import uk.gov.hmcts.reform.pcs.feesandpay.config.FeesConfiguration.LookUpReferenceData;
import uk.gov.hmcts.reform.pcs.feesandpay.entity.Fee;
import uk.gov.hmcts.reform.pcs.feesandpay.exception.FeeNotFoundException;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeResponse;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo("FEE0412");
        assertThat(result.getDescription()).isEqualTo("Recovery of Land - County Court");
        assertThat(result.getVersion()).isEqualTo("4");
        assertThat(result.getCalculatedAmount()).isEqualByComparingTo(new BigDecimal("404.00"));

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

        assertThatThrownBy(() -> feesAndPayService.getFee(FEE_TYPE))
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

        assertThatThrownBy(() -> feesAndPayService.getFee(FEE_TYPE))
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

        assertThatThrownBy(() -> feesAndPayService.getFee(FEE_TYPE))
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

        assertThat(result.getCode()).isEqualTo(customFeeResponse.getCode());
        assertThat(result.getDescription()).isEqualTo(customFeeResponse.getDescription());
        assertThat(result.getVersion()).isEqualTo(customFeeResponse.getVersion());
        assertThat(result.getCalculatedAmount()).isEqualByComparingTo(customFeeResponse.getFeeAmount());
    }
}
