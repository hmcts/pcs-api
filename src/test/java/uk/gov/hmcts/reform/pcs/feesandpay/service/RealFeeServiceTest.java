package uk.gov.hmcts.reform.pcs.feesandpay.service;

import feign.FeignException.InternalServerError;
import feign.FeignException.NotFound;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fees.client.model.FeeLookupResponseDto;
import uk.gov.hmcts.reform.pcs.feesandpay.config.FeesConfiguration;
import uk.gov.hmcts.reform.pcs.feesandpay.config.FeesConfiguration.LookUpReferenceData;
import uk.gov.hmcts.reform.pcs.feesandpay.config.PCSFeesClient;
import uk.gov.hmcts.reform.pcs.feesandpay.exception.FeeNotFoundException;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeTypes;

import java.math.BigDecimal;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RealFeeServiceTest {

    @Mock
    private FeesConfiguration feesConfiguration;

    @Mock
    private PCSFeesClient pcsFeesClient;

    private RealFeeService underTest;

    private static final FeeTypes FEE_TYPE = FeeTypes.CASE_ISSUE_FEE;

    private LookUpReferenceData lookUpReferenceData;
    private FeeLookupResponseDto feeLookupResponseDto;

    @BeforeEach
    void setUp() {
        underTest = new RealFeeService(feesConfiguration, pcsFeesClient);

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
    void shouldSuccessfullyGetFeeDetails() {
        when(feesConfiguration.getLookup(FEE_TYPE.getCode())).thenReturn(lookUpReferenceData);
        when(pcsFeesClient.lookupFee(FEE_TYPE.getCode(),
            "default",
            "issue",
            new BigDecimal("1"),
            "PossessionCC"
        )).thenReturn(feeLookupResponseDto);

        FeeDetails feeDetails = underTest.getFee(FEE_TYPE);

        assertThat(feeDetails).isNotNull();
        assertThat(feeDetails.getCode()).isEqualTo("FEE0412");
        assertThat(feeDetails.getDescription()).isEqualTo("Recovery of Land - County Court");
        assertThat(feeDetails.getVersion()).isEqualTo(4);
        assertThat(feeDetails.getFeeAmount()).isEqualTo(BigDecimal.valueOf(404.00));

        verify(pcsFeesClient)
            .lookupFee(FEE_TYPE.getCode(), "default", "issue",
                       new BigDecimal("1"), "PossessionCC");
    }

    @Test
    void shouldThrowFeeNotFoundExceptionWhenFeeTypeNotInConfiguration() {
        when(feesConfiguration.getLookup(FEE_TYPE.getCode())).thenReturn(null);

        assertThatThrownBy(() -> underTest.getFee(FEE_TYPE.getCode()))
            .isInstanceOf(FeeNotFoundException.class)
            .hasMessageContaining("Fee not found for feeType: " + FEE_TYPE.getCode());
    }

    @Test
    void shouldThrowFeeNotFoundExceptionWhenFeignCallFails() {
        when(feesConfiguration.getLookup(FEE_TYPE.getCode())).thenReturn(lookUpReferenceData);

        Request request = Request.create(
            Request.HttpMethod.GET,
            "/fees/lookup",
            new HashMap<>(),
            null,
            new RequestTemplate()
        );

        when(pcsFeesClient.lookupFee(anyString(), anyString(),
                                     anyString(), any(BigDecimal.class), anyString()))
            .thenThrow(new NotFound("Fee not found", request, null, null));

        assertThatThrownBy(() -> underTest.getFee(FEE_TYPE.getCode()))
            .isInstanceOf(FeeNotFoundException.class)
            .hasMessageContaining("Unable to retrieve fee: " + FEE_TYPE.getCode())
            .hasCauseInstanceOf(NotFound.class);
    }

    @Test
    void shouldThrowFeeNotFoundExceptionWhenFeignReturnsServerError() {
        when(feesConfiguration.getLookup(FEE_TYPE.getCode())).thenReturn(lookUpReferenceData);

        Request request = Request.create(
            Request.HttpMethod.GET,
            "/fees/lookup",
            new HashMap<>(),
            null,
            new RequestTemplate()
        );

        when(pcsFeesClient.lookupFee(anyString(), anyString(), anyString(), any(BigDecimal.class), anyString()))
            .thenThrow(new InternalServerError(
                "Internal server error", request, null, null));

        assertThatThrownBy(() -> underTest.getFee(FEE_TYPE.getCode()))
            .isInstanceOf(FeeNotFoundException.class)
            .hasMessageContaining("Unable to retrieve fee: " + FEE_TYPE.getCode())
            .hasCauseInstanceOf(InternalServerError.class);
    }

}
