package uk.gov.hmcts.reform.pcs.ccd.view.builder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentPaymentFrequency;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared.RentArrearsTabDetails;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

public class RentArrearsTabDetailsBuilderTest {

    private RentArrearsTabDetailsBuilder rentArrearsTabDetailsBuilder;

    @BeforeEach
    void setUp() {
        rentArrearsTabDetailsBuilder = new RentArrearsTabDetailsBuilder();
    }

    @Test
    void shouldSetRentArrearsDetailsFromStandardFrequencyAndDailyCharge() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .rentDetails(RentDetails.builder()
                             .frequency(RentPaymentFrequency.WEEKLY)
                             .dailyCharge(new BigDecimal("1.50"))
                             .build())
            .build();

        // When
        RentArrearsTabDetails rentArrearsDetails = rentArrearsTabDetailsBuilder.buildRentArrearsTabDetails(pcsCase);

        // Then
        assertThat(rentArrearsDetails.getCalculationFrequency()).isEqualTo("Weekly");
        assertThat(rentArrearsDetails.getDailyRate()).isEqualTo("£1.50");
    }

    @Test
    void shouldSetRentArrearsDetailsFromFormattedCalculatedDailyCharge() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .rentDetails(RentDetails.builder()
                             .formattedCalculatedDailyCharge("£2.34")
                             .build())
            .build();

        // When
        RentArrearsTabDetails rentArrearsDetails = rentArrearsTabDetailsBuilder.buildRentArrearsTabDetails(pcsCase);

        // Then
        assertThat(rentArrearsDetails.getDailyRate()).isEqualTo("£2.34");
    }

    @Test
    void shouldSetRentArrearsDetailsFromCalculatedDailyCharge() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .rentDetails(RentDetails.builder()
                             .calculatedDailyCharge(new BigDecimal("3.40"))
                             .build())
            .build();

        // When
        RentArrearsTabDetails rentArrearsDetails = rentArrearsTabDetailsBuilder.buildRentArrearsTabDetails(pcsCase);

        // Then
        assertThat(rentArrearsDetails.getDailyRate()).isEqualTo("£3.40");
    }
}
