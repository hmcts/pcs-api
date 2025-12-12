package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentSection;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentPaymentFrequency;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

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
                .rentSection(RentSection.builder()
                        .currentRent("7000") // £70.00 in pence
                        .rentFrequency(RentPaymentFrequency.WEEKLY)
                        .build())
                .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getRentSection().getCalculatedDailyRentCharge()).isEqualTo("1000"); // £10.00 per day
    }

    @Test
    void shouldCalculateDailyRentForMonthlyFrequency() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .rentSection(RentSection.builder()
                        .currentRent("30000") // £300.00 in pence
                        .rentFrequency(RentPaymentFrequency.MONTHLY)
                        .build())
                .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getRentSection().getCalculatedDailyRentCharge()).isEqualTo("986"); // £9.86 per day
    }

    @Test
    void shouldUseProvidedDailyRentForOtherFrequency() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .rentSection(RentSection.builder()
                        .rentFrequency(RentPaymentFrequency.OTHER)
                        .dailyRentCharge("1500") // £15.00 per day
                        .build())
                .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getRentSection().getDailyRentCharge()).isEqualTo("1500");
    }

    @Test
    void shouldSetShowRentArrearsPageToNoForWeeklyFrequency() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .rentSection(RentSection.builder()
                        .rentFrequency(RentPaymentFrequency.WEEKLY)
                        .currentRent("7000")
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
                .rentSection(RentSection.builder()
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
                .rentSection(RentSection.builder()
                        .currentRent("14000") // £140.00 in pence
                        .rentFrequency(RentPaymentFrequency.FORTNIGHTLY)
                        .build())
                .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getRentSection().getCalculatedDailyRentCharge()).isEqualTo("1000"); // £10.00 per day
        assertThat(caseData.getRentSection().getFormattedCalculatedDailyRentCharge()).isEqualTo("£10.00");
    }

    @Test
    void shouldSetFormattedCurrencyWhenCalculatingDailyRent() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .rentSection(RentSection.builder()
                        .currentRent("7000") // £70.00 in pence
                        .rentFrequency(RentPaymentFrequency.WEEKLY)
                        .build())
                .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getRentSection().getCalculatedDailyRentCharge())
            .isEqualTo("1000"); // £10.00 per day in pence
        assertThat(caseData.getRentSection().getFormattedCalculatedDailyRentCharge())
            .isEqualTo("£10.00");
    }

    @Test
    void shouldNotProcessWhenRentFrequencyIsNull() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .rentSection(RentSection.builder()
                        .currentRent("7000")
                        .rentFrequency(null)
                        .build())
                .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getRentSection().getCalculatedDailyRentCharge()).isNull();
        assertThat(caseData.getRentSection().getFormattedCalculatedDailyRentCharge()).isNull();
        assertThat(caseData.getShowRentArrearsPage()).isNull();
    }

    @Test
    void shouldSetShowRentArrearsPageWhenCurrentRentIsEmpty() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .rentSection(RentSection.builder()
                        .currentRent("")
                        .rentFrequency(RentPaymentFrequency.WEEKLY)
                        .build())
                .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getRentSection().getCalculatedDailyRentCharge()).isNull();
        assertThat(caseData.getRentSection().getFormattedCalculatedDailyRentCharge()).isNull();
        assertThat(caseData.getShowRentArrearsPage()).isEqualTo(YesOrNo.NO);
    }

    @Test
    void shouldSetShowRentArrearsPageToNoForMonthlyFrequency() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .rentSection(RentSection.builder()
                        .rentFrequency(RentPaymentFrequency.MONTHLY)
                        .currentRent("30000")
                        .build())
                .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getShowRentArrearsPage()).isEqualTo(YesOrNo.NO);
    }
}
