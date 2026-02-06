package uk.gov.hmcts.reform.pcs.ccd.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeType;
import uk.gov.hmcts.reform.pcs.feesandpay.service.FeeService;

import java.math.BigDecimal;
import java.util.function.BiConsumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.feesandpay.model.FeeType.CASE_ISSUE_FEE;
import static uk.gov.hmcts.reform.pcs.feesandpay.model.FeeType.ENFORCEMENT_WARRANT_FEE;

@ExtendWith(MockitoExtension.class)
class FeeApplierTest extends BaseEventTest {

    @Mock
    private FeeService feeService;
    @Spy
    private FeeFormatter feeFormatter;
    @InjectMocks
    private FeeApplier underTest;

    @Test
    void shouldSetFormattedFeeWhenFeeServiceReturnsFee() {
        // Given
        PCSCase pcsCase = PCSCase.builder().build();
        FeeType feeType = CASE_ISSUE_FEE;
        BigDecimal feeAmount = BigDecimal.valueOf(9999.99);
        final String expectedFormattedFee = "£9999.99";

        when(feeService.getFee(feeType)).thenReturn(
                FeeDetails.builder()
                    .feeAmount(feeAmount)
                    .build());
        BiConsumer<PCSCase, String> setter = PCSCase::setFeeAmount;

        // When
        underTest.applyFormattedFeeAmount(pcsCase, feeType, setter);

        // Then
        verify(feeService).getFee(feeType);
        assertThat(pcsCase.getFeeAmount()).isEqualTo(expectedFormattedFee);
    }

    @Test
    void shouldSetUnableToRetrieveWhenFeeServiceThrows() {
        PCSCase pcsCase = PCSCase.builder().build();
        FeeType feeType = FeeType.CASE_ISSUE_FEE;
        final String expectedFormattedFee = FeeApplier.UNABLE_TO_RETRIEVE;

        when(feeService.getFee(feeType))
                .thenThrow(new RuntimeException("Fee service error"));
        BiConsumer<PCSCase, String> setter = PCSCase::setFeeAmount;

        // When
        underTest.applyFormattedFeeAmount(pcsCase, feeType, setter);

        // Then
        verify(feeService).getFee(feeType);
        assertThat(pcsCase.getFeeAmount()).isEqualTo(expectedFormattedFee);
    }

    @Test
    void shouldSetUnableToRetrieveWhenFeeServiceReturnsNull() {
        PCSCase pcsCase = PCSCase.builder().build();
        FeeType feeType = FeeType.CASE_ISSUE_FEE;
        final String expectedFormattedFee = FeeApplier.UNABLE_TO_RETRIEVE;
        FeeDetails feeDetails = FeeDetails.builder()
                        .feeAmount(null)
                                .build();

        when(feeService.getFee(feeType)).thenReturn(feeDetails);
        when(feeFormatter.formatFee(null)).thenReturn(null);

        BiConsumer<PCSCase, String> setter = PCSCase::setFeeAmount;

        // When
        underTest.applyFormattedFeeAmount(pcsCase, feeType, setter);

        // Then
        verify(feeService).getFee(feeType);
        assertThat(pcsCase.getFeeAmount()).isEqualTo(expectedFormattedFee);
    }

    @Test
    void shouldSetUnFormattedFeeWhenFeeServiceReturnsFee() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder().build())
            .build();
        FeeType feeType = ENFORCEMENT_WARRANT_FEE;
        BigDecimal feeAmount = BigDecimal.valueOf(123.45);

        when(feeService.getFee(ENFORCEMENT_WARRANT_FEE)).thenReturn(
            FeeDetails
                .builder()
                .feeAmount(feeAmount)
                .build()
        );
        BiConsumer<PCSCase, BigDecimal> setter = (caseData, fee) -> caseData
            .getEnforcementOrder().setWarrantFeeAmount(fee);

        // When
        underTest.applyFeeAmount(pcsCase, feeType, setter);

        // Then
        verify(feeService).getFee(feeType);
        assertThat(pcsCase.getEnforcementOrder().getWarrantFeeAmount()).isEqualTo(feeAmount);
    }

    @Test
    void shouldSetDefaultFeeWhenFeeServiceThrows() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder().build())
            .build();
        FeeType feeType = FeeType.ENFORCEMENT_WARRANT_FEE;
        final BigDecimal expectedDefaultFee = new BigDecimal("0");

        when(feeService.getFee(feeType))
            .thenThrow(new RuntimeException("Fee service error"));
        BiConsumer<PCSCase, BigDecimal> setter = (caseData, fee) -> caseData
            .getEnforcementOrder().setWarrantFeeAmount(fee);

        // When
        underTest.applyFeeAmount(pcsCase, feeType, setter);

        // Then
        verify(feeService).getFee(feeType);
        assertThat(pcsCase.getEnforcementOrder().getWarrantFeeAmount()).isEqualTo(expectedDefaultFee);
    }

    @Test
    void shouldSetDefaultFeeWhenFeeServiceReturnsNull() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder().build())
            .build();
        FeeType feeType = FeeType.ENFORCEMENT_WARRANT_FEE;
        final BigDecimal expectedFee = new BigDecimal("0");

        FeeDetails feeDetails = FeeDetails.builder()
                .feeAmount(null)
                .build();

        when(feeService.getFee(feeType)).thenReturn(feeDetails);
        BiConsumer<PCSCase, BigDecimal> setter = (caseData, fee) -> caseData
            .getEnforcementOrder().setWarrantFeeAmount(fee);

        // When
        underTest.applyFeeAmount(pcsCase, feeType, setter);

        // Then
        verify(feeService).getFee(feeType);
        assertThat(pcsCase.getEnforcementOrder().getWarrantFeeAmount()).isEqualTo(expectedFee);
    }
}
