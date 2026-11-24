package uk.gov.hmcts.reform.pcs.ccd.service.rules;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentPaymentFrequency;
import uk.gov.hmcts.reform.pcs.ccd.util.MoneyFormatter;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailyRentCalculatorTest {

    @Mock
    private MoneyFormatter moneyFormatter;

    private DailyRentCalculator underTest;

    @BeforeEach
    void setUp() {
        underTest = new DailyRentCalculator(moneyFormatter);
    }

    @ParameterizedTest
    @CsvSource({
        "WEEKLY, 70.00, 10.00",
        "WEEKLY, 100.00, 14.29",
        "FORTNIGHTLY, 140.00, 10.00",
        "FORTNIGHTLY, 100.00, 7.14",
        "MONTHLY, 304.40, 10.00",
        "MONTHLY, 500.00, 16.43",
    })
    void shouldDivideByFrequencyAndRoundHalfUp(RentPaymentFrequency frequency, BigDecimal rent,
                                               BigDecimal expected) {
        assertThat(underTest.calculateDailyRent(rent, frequency)).isEqualByComparingTo(expected);
    }

    @Test
    void shouldRoundHalfUp() {
        assertThat(underTest.calculateDailyRent(new BigDecimal("0.35"), RentPaymentFrequency.WEEKLY))
            .isEqualByComparingTo("0.05");
    }

    @Test
    void shouldRejectOtherFrequency() {
        assertThatThrownBy(() -> underTest.calculateDailyRent(BigDecimal.TEN, RentPaymentFrequency.OTHER))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Daily rent calculation not supported for frequency: OTHER");
    }

    @Test
    void shouldApplyCalculatedAndFormattedDailyCharge() {
        when(moneyFormatter.formatFee(new BigDecimal("10.00"))).thenReturn("£10");
        RentDetails rentDetails = RentDetails.builder()
            .currentRent(new BigDecimal("70.00"))
            .frequency(RentPaymentFrequency.WEEKLY)
            .build();

        underTest.applyCalculatedDailyCharge(rentDetails);

        assertThat(rentDetails.getCalculatedDailyCharge()).isEqualByComparingTo("10.00");
        assertThat(rentDetails.getFormattedCalculatedDailyCharge()).isEqualTo("£10");
    }

    @Test
    void shouldNotApplyForOtherFrequency() {
        RentDetails rentDetails = RentDetails.builder()
            .currentRent(new BigDecimal("70.00"))
            .frequency(RentPaymentFrequency.OTHER)
            .build();

        underTest.applyCalculatedDailyCharge(rentDetails);

        assertThat(rentDetails.getCalculatedDailyCharge()).isNull();
        verifyNoInteractions(moneyFormatter);
    }

    @Test
    void shouldNotApplyWhenRentOrFrequencyMissing() {
        RentDetails noRent = RentDetails.builder().frequency(RentPaymentFrequency.WEEKLY).build();
        RentDetails noFrequency = RentDetails.builder().currentRent(BigDecimal.TEN).build();

        underTest.applyCalculatedDailyCharge(noRent);
        underTest.applyCalculatedDailyCharge(noFrequency);
        underTest.applyCalculatedDailyCharge(null);

        assertThat(noRent.getCalculatedDailyCharge()).isNull();
        assertThat(noFrequency.getCalculatedDailyCharge()).isNull();
        verifyNoInteractions(moneyFormatter);
    }
}
