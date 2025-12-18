package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentDetailsSection;
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
                .rentDetails(RentDetailsSection.builder()
                        .currentRent(new BigDecimal("70.00"))
                        .rentFrequency(RentPaymentFrequency.WEEKLY)
                        .build())
                .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getRentDetails().getCalculatedDailyRentChargeAmount())
            .isEqualTo(new BigDecimal("10.00"));
    }

    @Test
    void shouldCalculateDailyRentForMonthlyFrequency() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .rentDetails(RentDetailsSection.builder()
                        .currentRent(new BigDecimal("300.00"))
                        .rentFrequency(RentPaymentFrequency.MONTHLY)
                        .build())
                .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getRentDetails().getCalculatedDailyRentChargeAmount()).isEqualTo("9.86");
    }

    @Test
    void shouldUseProvidedDailyRentForOtherFrequency() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .rentDetails(RentDetailsSection.builder()
                        .rentFrequency(RentPaymentFrequency.OTHER)
                        .dailyRentChargeAmount(new BigDecimal("15.00"))
                        .build())
                .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getRentDetails().getDailyRentChargeAmount()).isEqualTo(new BigDecimal("15.00"));
    }

    @Test
    void shouldSetShowRentArrearsPageToNoForWeeklyFrequency() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .rentDetails(RentDetailsSection.builder()
                        .rentFrequency(RentPaymentFrequency.WEEKLY)
                        .currentRent(new BigDecimal("70.00"))
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
                .rentDetails(RentDetailsSection.builder()
                        .rentFrequency(RentPaymentFrequency.OTHER)
                        .build())
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
                .rentDetails(RentDetailsSection.builder()
                        .currentRent(new BigDecimal("140.00"))
                        .rentFrequency(RentPaymentFrequency.FORTNIGHTLY)
                        .build())
                .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getRentDetails().getCalculatedDailyRentChargeAmount()).isEqualTo(new BigDecimal("10.00"));
        assertThat(caseData.getRentDetails().getFormattedCalculatedDailyRentChargeAmount()).isEqualTo("£10.00");
    }

    @Test
    void shouldSetFormattedCurrencyWhenCalculatingDailyRent() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .rentDetails(RentDetailsSection.builder()
                        .currentRent(new BigDecimal("70.00"))
                        .rentFrequency(RentPaymentFrequency.WEEKLY)
                        .build())
                .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getRentDetails().getCalculatedDailyRentChargeAmount())
            .isEqualTo(new BigDecimal("10.00"));
        assertThat(caseData.getRentDetails().getFormattedCalculatedDailyRentChargeAmount())
            .isEqualTo("£10.00");
    }

    @Test
    void shouldNotProcessWhenRentFrequencyIsNull() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .rentDetails(RentDetailsSection.builder()
                        .currentRent(new BigDecimal("70.00"))
                        .rentFrequency(null)
                        .build())
                .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getRentDetails().getCalculatedDailyRentChargeAmount()).isNull();
        assertThat(caseData.getRentDetails().getFormattedCalculatedDailyRentChargeAmount()).isNull();
        assertThat(caseData.getShowRentArrearsPage()).isNull();
    }

    @Test
    void shouldSetShowRentArrearsPageWhenCurrentRentIsNull() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .rentDetails(RentDetailsSection.builder()
                        .currentRent(null)
                        .rentFrequency(RentPaymentFrequency.WEEKLY)
                        .build())
                .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getRentDetails().getCalculatedDailyRentChargeAmount()).isNull();
        assertThat(caseData.getRentDetails().getFormattedCalculatedDailyRentChargeAmount()).isNull();
        assertThat(caseData.getShowRentArrearsPage()).isEqualTo(YesOrNo.NO);
    }

    @Test
    void shouldSetShowRentArrearsPageToNoForMonthlyFrequency() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .rentDetails(RentDetailsSection.builder()
                        .rentFrequency(RentPaymentFrequency.MONTHLY)
                        .currentRent(new BigDecimal("300.00"))
                        .build())
                .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getShowRentArrearsPage()).isEqualTo(YesOrNo.NO);
    }
}
