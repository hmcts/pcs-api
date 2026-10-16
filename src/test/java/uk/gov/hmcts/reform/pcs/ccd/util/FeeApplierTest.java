package uk.gov.hmcts.reform.pcs.ccd.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeType;
import uk.gov.hmcts.reform.pcs.feesandpay.service.FeeService;

import java.math.BigDecimal;
import java.util.function.BiConsumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.feesandpay.model.FeeType.HEARING_FEE;

@ExtendWith(MockitoExtension.class)
class FeeApplierTest extends BaseEventTest {

    @InjectMocks
    private FeeApplier underTest;

    @Mock
    private FeeService feeService;

    @Spy
    private MoneyFormatter moneyFormatter;

    @Test
    void shouldSetFormattedFeeWhenFeeServiceReturnsFee() {
        // Given
        PCSCase pcsCase = PCSCase.builder().build();
        FeeType feeType = HEARING_FEE;
        BigDecimal feeAmount = BigDecimal.valueOf(123.45);
        final String expectedFormattedFee = "£123.45";

        when(feeService.getFee(HEARING_FEE)).thenReturn(
            FeeDetails
                .builder()
                .feeAmount(feeAmount)
                .build()
        );
        BiConsumer<PCSCase, String> setter = PCSCase::setFeeAmount;

        // When
        underTest.applyFeeAmount(pcsCase, feeType, setter);

        // Then
        verify(feeService).getFee(feeType);
        assertThat(pcsCase.getFeeAmount()).isEqualTo(expectedFormattedFee);
    }

    @Test
    void shouldSetDefaultFeeWhenFeeServiceThrows() {
        // Given
        PCSCase pcsCase = PCSCase.builder().build();
        FeeType feeType = FeeType.HEARING_FEE;
        final String expectedFormattedFee = FeeApplier.UNABLE_TO_RETRIEVE;

        when(feeService.getFee(feeType))
            .thenThrow(new RuntimeException("Fee service error"));
        BiConsumer<PCSCase, String> setter = PCSCase::setFeeAmount;

        // When
        underTest.applyFeeAmount(pcsCase, feeType, setter);

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

        BiConsumer<PCSCase, String> setter = PCSCase::setFeeAmount;

        // When
        underTest.applyFeeAmount(pcsCase, feeType, setter);

        // Then
        verify(feeService).getFee(feeType);
        assertThat(pcsCase.getFeeAmount()).isEqualTo(expectedFormattedFee);
    }
}
