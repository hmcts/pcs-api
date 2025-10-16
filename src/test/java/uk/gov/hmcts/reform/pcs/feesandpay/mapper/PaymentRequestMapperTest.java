package uk.gov.hmcts.reform.pcs.feesandpay.mapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.feesandpay.dto.CasePaymentRequestDto;
import uk.gov.hmcts.reform.pcs.feesandpay.dto.FeeDto;
import uk.gov.hmcts.reform.pcs.feesandpay.entity.Fee;
import uk.gov.hmcts.reform.pcs.feesandpay.model.ServiceRequestBody;

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
    private static final String CALLBACK_URL = "https://example.com/callback";
    private static final String CASE_REFERENCE = "REF-2024-001";
    private static final String CCD_CASE_NUMBER = "1234567890123456";

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
    void shouldMapToServiceRequestBody() {
        FeeDto feeDto = FeeDto.builder()
            .code(FEE_CODE)
            .version(FEE_VERSION)
            .calculatedAmount(FEE_AMOUNT)
            .volume(VOLUME)
            .build();
        FeeDto[] fees = new FeeDto[]{feeDto};

        CasePaymentRequestDto casePaymentRequest = CasePaymentRequestDto.builder()
            .action(ACTION)
            .responsibleParty(RESPONSIBLE_PARTY)
            .build();

        ServiceRequestBody result = paymentRequestMapper.toServiceRequestBody(
            CALLBACK_URL,
            CASE_REFERENCE,
            CCD_CASE_NUMBER,
            fees,
            casePaymentRequest
        );

        assertThat(result).isNotNull();
        assertThat(result.getCallbackUrl()).isEqualTo(CALLBACK_URL);
        assertThat(result.getCaseReference()).isEqualTo(CASE_REFERENCE);
        assertThat(result.getCcdCaseNumber()).isEqualTo(CCD_CASE_NUMBER);
        assertThat(result.getFees()).hasSize(1);
        assertThat(result.getFees()[0]).isEqualTo(feeDto);
        assertThat(result.getCasePaymentRequest()).isEqualTo(casePaymentRequest);
        assertThat(result.getHmctsOrgId()).isEqualTo("AAA3"); // Default value
    }

    @Test
    void shouldMapToServiceRequestBodyWithMultipleFees() {
        FeeDto feeDto1 = FeeDto.builder()
            .code("FEE0001")
            .version("1")
            .calculatedAmount(new BigDecimal("100.00"))
            .volume(1)
            .build();

        FeeDto feeDto2 = FeeDto.builder()
            .code("FEE0002")
            .version("2")
            .calculatedAmount(new BigDecimal("200.00"))
            .volume(2)
            .build();

        FeeDto[] fees = new FeeDto[]{feeDto1, feeDto2};

        CasePaymentRequestDto casePaymentRequest = CasePaymentRequestDto.builder()
            .action(ACTION)
            .responsibleParty(RESPONSIBLE_PARTY)
            .build();

        ServiceRequestBody result = paymentRequestMapper.toServiceRequestBody(
            CALLBACK_URL,
            CASE_REFERENCE,
            CCD_CASE_NUMBER,
            fees,
            casePaymentRequest
        );

        assertThat(result).isNotNull();
        assertThat(result.getFees()).hasSize(2);
        assertThat(result.getFees()[0]).isEqualTo(feeDto1);
        assertThat(result.getFees()[1]).isEqualTo(feeDto2);
    }

    @Test
    void shouldMapToServiceRequestBodyWithEmptyFees() {
        FeeDto[] fees = new FeeDto[]{};

        CasePaymentRequestDto casePaymentRequest = CasePaymentRequestDto.builder()
            .action(ACTION)
            .responsibleParty(RESPONSIBLE_PARTY)
            .build();

        ServiceRequestBody result = paymentRequestMapper.toServiceRequestBody(
            CALLBACK_URL,
            CASE_REFERENCE,
            CCD_CASE_NUMBER,
            fees,
            casePaymentRequest
        );

        assertThat(result).isNotNull();
        assertThat(result.getFees()).isEmpty();
    }

    @Test
    void shouldMapToServiceRequestBodyWithNullCallback() {
        FeeDto feeDto = FeeDto.builder()
            .code(FEE_CODE)
            .version(FEE_VERSION)
            .calculatedAmount(FEE_AMOUNT)
            .volume(VOLUME)
            .build();
        FeeDto[] fees = new FeeDto[]{feeDto};

        CasePaymentRequestDto casePaymentRequest = CasePaymentRequestDto.builder()
            .action(ACTION)
            .responsibleParty(RESPONSIBLE_PARTY)
            .build();

        ServiceRequestBody result = paymentRequestMapper.toServiceRequestBody(
            null,
            CASE_REFERENCE,
            CCD_CASE_NUMBER,
            fees,
            casePaymentRequest
        );

        assertThat(result).isNotNull();
        assertThat(result.getCallbackUrl()).isNull();
        assertThat(result.getCaseReference()).isEqualTo(CASE_REFERENCE);
        assertThat(result.getCcdCaseNumber()).isEqualTo(CCD_CASE_NUMBER);
    }

    @Test
    void shouldMapCompleteWorkflow() {
        Fee feeEntity = Fee.builder()
            .code(FEE_CODE)
            .version(FEE_VERSION)
            .calculatedAmount(FEE_AMOUNT)
            .build();

        FeeDto feeDto = paymentRequestMapper.toFeeDto(feeEntity, VOLUME);

        CasePaymentRequestDto casePaymentRequest = paymentRequestMapper.toCasePaymentRequest(
            ACTION,
            RESPONSIBLE_PARTY
        );

        ServiceRequestBody serviceRequestBody = paymentRequestMapper.toServiceRequestBody(
            CALLBACK_URL,
            CASE_REFERENCE,
            CCD_CASE_NUMBER,
            new FeeDto[]{feeDto},
            casePaymentRequest
        );

        assertThat(serviceRequestBody).isNotNull();
        assertThat(serviceRequestBody.getCallbackUrl()).isEqualTo(CALLBACK_URL);
        assertThat(serviceRequestBody.getCaseReference()).isEqualTo(CASE_REFERENCE);
        assertThat(serviceRequestBody.getCcdCaseNumber()).isEqualTo(CCD_CASE_NUMBER);
        assertThat(serviceRequestBody.getFees()).hasSize(1);
        assertThat(serviceRequestBody.getFees()[0].getCode()).isEqualTo(FEE_CODE);
        assertThat(serviceRequestBody.getFees()[0].getVersion()).isEqualTo(FEE_VERSION);
        assertThat(serviceRequestBody.getFees()[0].getCalculatedAmount()).isEqualByComparingTo(FEE_AMOUNT);
        assertThat(serviceRequestBody.getFees()[0].getVolume()).isEqualTo(VOLUME);
        assertThat(serviceRequestBody.getCasePaymentRequest().getAction()).isEqualTo(ACTION);
        assertThat(serviceRequestBody.getCasePaymentRequest().getResponsibleParty()).isEqualTo(RESPONSIBLE_PARTY);
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
