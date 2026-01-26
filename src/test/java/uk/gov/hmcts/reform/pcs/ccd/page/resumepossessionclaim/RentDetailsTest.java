package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentPaymentFrequency;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.util.MoneyConverter;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class RentDetailsTest extends BasePageTest {

    @Mock
    private MoneyConverter moneyConverter;

    @BeforeEach
    void setUp() {
        setPageUnderTest(new RentDetailsPage(moneyConverter));
    }

    @Test
    void shouldSetShowRentArrearsPageToNoForWeeklyFrequency() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .rentDetails(RentDetails.builder()
                             .frequency(RentPaymentFrequency.WEEKLY)
                             .build())
            .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getShowRentArrearsPage()).isEqualTo(YesOrNo.NO);
    }

    @Test
    void shouldSetShowRentArrearsPageToNoForMonthlyFrequency() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .rentDetails(RentDetails.builder()
                             .frequency(RentPaymentFrequency.MONTHLY)
                             .build())
            .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getShowRentArrearsPage()).isEqualTo(YesOrNo.NO);
    }

    @Test
    void shouldSetShowRentArrearsPageToNoForFortnightlyFrequency() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .rentDetails(RentDetails.builder()
                             .frequency(RentPaymentFrequency.FORTNIGHTLY)
                             .build())
            .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getShowRentArrearsPage()).isEqualTo(YesOrNo.NO);
    }

    @Test
    void shouldSetShowRentArrearsPageToYesForOtherFrequency() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .rentDetails(RentDetails.builder()
                             .frequency(RentPaymentFrequency.OTHER)
                             .build())
            .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getShowRentArrearsPage()).isEqualTo(YesOrNo.YES);
    }

    @Test
    void shouldNotProcessWhenRentFrequencyIsNull() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .rentDetails(RentDetails.builder().frequency(null).build())
            .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getShowRentArrearsPage()).isNull();
    }

    @Test
    void shouldNotProcessWhenRentDetailsIsNull() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .rentDetails(null)
            .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getShowRentArrearsPage()).isNull();
    }

    @Test
    void shouldSetShowRentArrearsPageWhenCurrentRentIsNull() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .rentDetails(RentDetails.builder()
                             .currentRent(null)
                             .frequency(RentPaymentFrequency.WEEKLY)
                             .build())
            .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getShowRentArrearsPage()).isEqualTo(YesOrNo.NO);
    }

    @Test
    void shouldCalculateDailyRentForWeeklyFrequency() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .rentDetails(RentDetails.builder()
                             .currentRent(new BigDecimal("300.00"))
                             .frequency(RentPaymentFrequency.WEEKLY)
                             .build())
            .build();

        // When
        callMidEventHandler(caseData);

        // Then
        // £300.00 / 7 = £42.8571 pence, rounded to £42.86
        assertThat(caseData.getRentDetails().getCalculatedDailyCharge()).isEqualTo(new BigDecimal("42.86"));
    }

    @Test
    void shouldCalculateDailyRentForMonthlyFrequency() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .rentDetails(RentDetails.builder()
                             .currentRent(new BigDecimal("300.00"))
                             .frequency(RentPaymentFrequency.MONTHLY)
                             .build())
            .build();

        // When
        callMidEventHandler(caseData);

        // Then
        // £300.00 / 30.44 = £9.8555, rounded to £9.86
        assertThat(caseData.getRentDetails().getCalculatedDailyCharge()).isEqualTo(new BigDecimal("9.86"));
    }

    @Test
    void shouldCalculateDailyRentForFortnightlyFrequency() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .rentDetails(RentDetails.builder()
                             .currentRent(new BigDecimal("300.00"))
                             .frequency(RentPaymentFrequency.FORTNIGHTLY)
                             .build())
            .build();

        // When
        callMidEventHandler(caseData);

        // Then
        // £300.00 / 14 = £21.4286 , rounded to £21.43
        assertThat(caseData.getRentDetails().getCalculatedDailyCharge()).isEqualTo(new BigDecimal("21.43"));
    }

    @Test
    void shouldSetFormattedCurrencyWhenCalculatingDailyRent() {
        // Given - rent that results in whole pounds (no pence)
        PCSCase caseData = PCSCase.builder()
            .rentDetails(RentDetails.builder()
                             .currentRent(new BigDecimal("70.00"))
                             .frequency(RentPaymentFrequency.WEEKLY)
                             .build())
            .build();

        // When
        callMidEventHandler(caseData);

        // Then
        // £70.00 / 7 = £10.00 , formatted should strip trailing zeros to "£10"
        assertThat(caseData.getRentDetails().getCalculatedDailyCharge()).isEqualTo(new BigDecimal("10.00"));
    }

    @Test
    void shouldNotCalculateDailyRentWhenCurrentRentIsNull() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .rentDetails(RentDetails.builder()
                             .currentRent(null)
                             .frequency(RentPaymentFrequency.WEEKLY)
                             .build())
            .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getRentDetails().getCalculatedDailyCharge()).isNull();
    }

}
