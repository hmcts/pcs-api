package uk.gov.hmcts.reform.pcs.feesandpay.mapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.payments.client.models.CasePaymentRequestDto;
import uk.gov.hmcts.reform.payments.client.models.FeeDto;
import uk.gov.hmcts.reform.pcs.feesandpay.entity.Fee;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PaymentRequestMapperTest {

    @InjectMocks
    private PaymentRequestMapper paymentRequestMapper;

    private static final String FEE_CODE = "FEE0412";
    private static final String FEE_VERSION = "4";
    private static final BigDecimal FEE_AMOUNT = new BigDecimal("404.00");
    private static final int VOLUME = 1;
    private static final String ACTION = "case-issue";
    private static final String RESPONSIBLE_PARTY = "Test Claimant";

    @Test
    void shouldMapFeeEntityToFeeDto() {
        Fee fee = Fee.builder()
            .code(FEE_CODE)
            .version(FEE_VERSION)
            .calculatedAmount(FEE_AMOUNT)
            .build();

        FeeDto result = paymentRequestMapper.toFeeDto(fee, VOLUME);

        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo(FEE_CODE);
        assertThat(result.getVersion()).isEqualTo(FEE_VERSION);
        assertThat(result.getCalculatedAmount()).isEqualByComparingTo(FEE_AMOUNT);
        assertThat(result.getVolume()).isEqualTo(VOLUME);
    }

    @Test
    void shouldMapFeeEntityToFeeDtoWithDifferentVolume() {
        Fee fee = Fee.builder()
            .code(FEE_CODE)
            .version(FEE_VERSION)
            .calculatedAmount(FEE_AMOUNT)
            .build();
        int customVolume = 5;

        FeeDto result = paymentRequestMapper.toFeeDto(fee, customVolume);

        assertThat(result).isNotNull();
        assertThat(result.getVolume()).isEqualTo(customVolume);
    }

    @Test
    void shouldMapFeeEntityToFeeDtoWithZeroAmount() {
        Fee fee = Fee.builder()
            .code("FEE0000")
            .version("1")
            .calculatedAmount(BigDecimal.ZERO)
            .build();

        FeeDto result = paymentRequestMapper.toFeeDto(fee, VOLUME);

        assertThat(result).isNotNull();
        assertThat(result.getCalculatedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldMapFeeEntityToFeeDtoWithLargeAmount() {
        BigDecimal largeAmount = new BigDecimal("99999.99");
        Fee fee = Fee.builder()
            .code("FEE9999")
            .version("10")
            .calculatedAmount(largeAmount)
            .build();

        FeeDto result = paymentRequestMapper.toFeeDto(fee, VOLUME);

        assertThat(result).isNotNull();
        assertThat(result.getCalculatedAmount()).isEqualByComparingTo(largeAmount);
    }

    @Test
    void shouldNotIncludeOptionalFieldsInFeeDto() {
        Fee fee = Fee.builder()
            .code(FEE_CODE)
            .version(FEE_VERSION)
            .calculatedAmount(FEE_AMOUNT)
            .build();

        FeeDto result = paymentRequestMapper.toFeeDto(fee, VOLUME);

        assertThat(result.getMemoLine()).isNull();
        assertThat(result.getCcdCaseNumber()).isNull();
        assertThat(result.getReference()).isNull();
    }

    @Test
    void shouldMapToCasePaymentRequest() {
        CasePaymentRequestDto result = paymentRequestMapper.toCasePaymentRequest(ACTION, RESPONSIBLE_PARTY);

        assertThat(result).isNotNull();
        assertThat(result.getAction()).isEqualTo(ACTION);
        assertThat(result.getResponsibleParty()).isEqualTo(RESPONSIBLE_PARTY);
    }

    @Test
    void shouldMapToCasePaymentRequestWithDifferentAction() {
        String customAction = "hearing-fee";

        CasePaymentRequestDto result = paymentRequestMapper.toCasePaymentRequest(customAction, RESPONSIBLE_PARTY);

        assertThat(result).isNotNull();
        assertThat(result.getAction()).isEqualTo(customAction);
    }

    @Test
    void shouldMapToCasePaymentRequestWithDifferentResponsibleParty() {
        String customParty = "Another Claimant";

        CasePaymentRequestDto result = paymentRequestMapper.toCasePaymentRequest(ACTION, customParty);

        assertThat(result).isNotNull();
        assertThat(result.getResponsibleParty()).isEqualTo(customParty);
    }

    @Test
    void shouldPreserveDecimalPrecisionWhenMappingFee() {
        BigDecimal preciseAmount = new BigDecimal("404.567");
        Fee fee = Fee.builder()
            .code(FEE_CODE)
            .version(FEE_VERSION)
            .calculatedAmount(preciseAmount)
            .build();

        FeeDto result = paymentRequestMapper.toFeeDto(fee, VOLUME);

        assertThat(result.getCalculatedAmount()).isEqualByComparingTo(preciseAmount);
        assertThat(result.getCalculatedAmount().scale()).isEqualTo(preciseAmount.scale());
    }
}
