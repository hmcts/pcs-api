package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentPaymentFrequency;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class RentDetailsTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new RentDetails());
    }

    @Test
    void shouldCalculateDailyRentForWeeklyFrequency() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .currentRent(new BigDecimal("70.00"))
                .rentFrequency(RentPaymentFrequency.WEEKLY)
                .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getCalculatedDailyRentChargeAmount()).isEqualTo(new BigDecimal("10.00"));
    }

    @Test
    void shouldCalculateDailyRentForMonthlyFrequency() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .currentRent(new BigDecimal("300.00"))
                .rentFrequency(RentPaymentFrequency.MONTHLY)
                .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getCalculatedDailyRentChargeAmount()).isEqualTo(new BigDecimal("9.86"));
    }

    @Test
    void shouldUseProvidedDailyRentForOtherFrequency() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .rentFrequency(RentPaymentFrequency.OTHER)
                .dailyRentChargeAmount(new BigDecimal("15.00"))
                .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getDailyRentChargeAmount()).isEqualTo(new BigDecimal("15.00"));
    }

    @Test
    void shouldSetShowRentArrearsPageToNoForWeeklyFrequency() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .rentFrequency(RentPaymentFrequency.WEEKLY)
                .currentRent(new BigDecimal("70.00"))
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
                .rentFrequency(RentPaymentFrequency.OTHER)
                .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getShowRentArrearsPage()).isEqualTo(YesOrNo.YES);
    }

    @Test
    void shouldCalculateDailyRentForFortnightlyFrequency() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .currentRent(new BigDecimal("140.00"))
                .rentFrequency(RentPaymentFrequency.FORTNIGHTLY)
                .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getCalculatedDailyRentChargeAmount()).isEqualTo(new BigDecimal("10.00"));
        assertThat(caseData.getFormattedCalculatedDailyRentChargeAmount()).isEqualTo("£10.00");
    }

    @Test
    void shouldSetFormattedCurrencyWhenCalculatingDailyRent() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .currentRent(new BigDecimal("70.00"))
                .rentFrequency(RentPaymentFrequency.WEEKLY)
                .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getCalculatedDailyRentChargeAmount()).isEqualTo(new BigDecimal("10.00"));
        assertThat(caseData.getFormattedCalculatedDailyRentChargeAmount()).isEqualTo("£10.00");
    }

    @Test
    void shouldNotProcessWhenRentFrequencyIsNull() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .currentRent(new BigDecimal("70.00"))
                .rentFrequency(null)
                .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getCalculatedDailyRentChargeAmount()).isNull();
        assertThat(caseData.getFormattedCalculatedDailyRentChargeAmount()).isNull();
        assertThat(caseData.getShowRentArrearsPage()).isNull();
    }

    @Test
    void shouldSetShowRentArrearsPageWhenCurrentRentIsNull() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .currentRent(null)
                .rentFrequency(RentPaymentFrequency.WEEKLY)
                .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getCalculatedDailyRentChargeAmount()).isNull();
        assertThat(caseData.getFormattedCalculatedDailyRentChargeAmount()).isNull();
        assertThat(caseData.getShowRentArrearsPage()).isEqualTo(YesOrNo.NO);
    }

    @Test
    void shouldSetShowRentArrearsPageToNoForMonthlyFrequency() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .rentFrequency(RentPaymentFrequency.MONTHLY)
                .currentRent(new BigDecimal("300.00"))
                .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getShowRentArrearsPage()).isEqualTo(YesOrNo.NO);
    }
}
