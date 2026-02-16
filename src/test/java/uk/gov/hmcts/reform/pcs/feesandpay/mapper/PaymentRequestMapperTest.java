package uk.gov.hmcts.reform.pcs.feesandpay.mapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.payments.client.models.CasePaymentRequestDto;
import uk.gov.hmcts.reform.payments.client.models.FeeDto;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;

import java.math.BigDecimal;

import static java.math.BigDecimal.ZERO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class PaymentRequestMapperTest {

    @InjectMocks
    private PaymentRequestMapper paymentRequestMapper;

    private static final String FEE_CODE = "FEE0412";
    private static final Integer FEE_VERSION = 4;
    private static final BigDecimal FEE_AMOUNT = new BigDecimal("404.00");
    private static final int VOLUME = 1;
    private static final String RESPONSIBLE_PARTY = "Test Claimant";

    @Test
    void shouldMapFeeLookupResponseDtoToFeeDto() {
        FeeDetails feeDetails = FeeDetails.builder()
            .code(FEE_CODE)
            .version(FEE_VERSION)
            .feeAmount(FEE_AMOUNT)
            .build();

        FeeDto result = paymentRequestMapper.toFeeDto(feeDetails, VOLUME);

        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo(FEE_CODE);
        assertThat(result.getVersion()).isEqualTo(String.valueOf(FEE_VERSION));
        assertThat(result.getCalculatedAmount()).isEqualByComparingTo(FEE_AMOUNT);
        assertThat(result.getVolume()).isEqualTo(VOLUME);
    }

    @Test
    void shouldMapFeeLookupResponseDtoToFeeDtoWithDifferentVolume() {
        FeeDetails feeDetails = FeeDetails.builder()
            .code(FEE_CODE)
            .version(FEE_VERSION)
            .feeAmount(FEE_AMOUNT)
            .build();
        int customVolume = 5;

        FeeDto result = paymentRequestMapper.toFeeDto(feeDetails, customVolume);

        assertThat(result).isNotNull();
        assertThat(result.getVolume()).isEqualTo(customVolume);
    }

    @Test
    void shouldMapFeeLookupResponseDtoToFeeDtoWithZeroAmount() {
        FeeDetails feeDetails = FeeDetails.builder()
            .code("FEE0000")
            .version(1)
            .feeAmount(ZERO)
            .build();

        FeeDto result = paymentRequestMapper.toFeeDto(feeDetails, VOLUME);

        assertThat(result).isNotNull();
        assertThat(result.getCalculatedAmount()).isEqualByComparingTo(ZERO);
    }

    @Test
    void shouldMapFeeLookupResponseDtoToFeeDtoWithLargeAmount() {
        BigDecimal largeAmount = new BigDecimal("99999.99");
        FeeDetails feeDetails = FeeDetails.builder()
            .code("FEE9999")
            .version(10)
            .feeAmount(largeAmount)
            .build();

        FeeDto result = paymentRequestMapper.toFeeDto(feeDetails, VOLUME);

        assertThat(result).isNotNull();
        assertThat(result.getCalculatedAmount()).isEqualByComparingTo(largeAmount);
    }

    @Test
    void shouldNotIncludeOptionalFieldsInFeeDto() {
        FeeDetails feeDetails = FeeDetails.builder()
            .code(FEE_CODE)
            .version(FEE_VERSION)
            .feeAmount(FEE_AMOUNT)
            .build();

        FeeDto result = paymentRequestMapper.toFeeDto(feeDetails, VOLUME);

        assertThat(result.getMemoLine()).isNull();
        assertThat(result.getCcdCaseNumber()).isNull();
        assertThat(result.getReference()).isNull();
    }

    @Test
    void shouldMapToCasePaymentRequest() {
        CasePaymentRequestDto result = paymentRequestMapper.toCasePaymentRequest(RESPONSIBLE_PARTY);

        assertThat(result).isNotNull();
        assertThat(result.getAction()).isEqualTo("payment");
        assertThat(result.getResponsibleParty()).isEqualTo(RESPONSIBLE_PARTY);
    }

    @Test
    void shouldMapToCasePaymentRequestWithDifferentResponsibleParty() {
        String customParty = "Another Claimant";

        CasePaymentRequestDto result = paymentRequestMapper.toCasePaymentRequest(customParty);

        assertThat(result).isNotNull();
        assertThat(result.getResponsibleParty()).isEqualTo(customParty);
        assertThat(result.getAction()).isEqualTo("payment");
    }

    @Test
    void shouldPreserveDecimalPrecisionWhenMappingFee() {
        BigDecimal preciseAmount = new BigDecimal("404.567");
        FeeDetails feeDetails = FeeDetails.builder()
            .code(FEE_CODE)
            .version(FEE_VERSION)
            .feeAmount(preciseAmount)
            .build();

        FeeDto result = paymentRequestMapper.toFeeDto(feeDetails, VOLUME);

        assertThat(result.getCalculatedAmount()).isEqualByComparingTo(preciseAmount);
        assertThat(result.getCalculatedAmount().scale()).isEqualTo(preciseAmount.scale());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenFeeIsNull() {
        assertThatThrownBy(() -> paymentRequestMapper.toFeeDto(null, VOLUME))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("fee details must not be null");
    }

    @Test
    void shouldConvertVersionToStringCorrectly() {
        FeeDetails feeDetails = FeeDetails.builder()
            .code(FEE_CODE)
            .version(123)
            .feeAmount(FEE_AMOUNT)
            .build();

        FeeDto result = paymentRequestMapper.toFeeDto(feeDetails, VOLUME);

        assertThat(result.getVersion()).isEqualTo("123");
    }
}
