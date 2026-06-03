package uk.gov.hmcts.reform.pcs.ccd.view.builder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsSection;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentPaymentFrequency;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.ClaimGroundSummary;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared.RentArrearsTabDetails;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.math.BigDecimal;
import java.util.List;

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
        assertThat(rentArrearsDetails.getRentFrequency()).isNull();
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

    @Test
    void shouldSetCalculationFrequencyInRentArrearsWhenFrequencyIsOther() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .showRentSectionPage(YesOrNo.YES)
            .rentDetails(
                RentDetails.builder()
                    .currentRent(new BigDecimal("4.00"))
                    .frequency(RentPaymentFrequency.OTHER)
                    .otherFrequency("Other frequency")
                    .calculatedDailyCharge(new BigDecimal("3.40"))
                    .build())
            .arrearsJudgmentWanted(VerticalYesNo.YES)
            .rentArrears(
                RentArrearsSection.builder()
                    .recoveryAttempted(VerticalYesNo.YES)
                    .recoveryAttemptDetails("recovery details")
                    .total(new BigDecimal("100.00"))
                    .statementDocuments(
                        List.of(
                            ListValue.<Document>builder()
                                .value(Document.builder().build())
                                .build())
                    )
                    .build()
            )
            .build();

        // When
        RentArrearsTabDetails rentArrearsDetails = rentArrearsTabDetailsBuilder
            .buildRentArrearsTabDetails(pcsCase);

        // Then
        assertThat(rentArrearsDetails.getCalculationFrequency()).isEqualTo("Other frequency");
    }

    @Test
    void shouldNotBuildRentArrearsWithNoRentArrears() {
        // Given
        PCSCase pcsCase = PCSCase.builder().build();

        // When
        RentArrearsTabDetails rentArrearsDetails = rentArrearsTabDetailsBuilder
            .buildRentArrearsTabDetails(pcsCase);

        // Then
        assertThat(rentArrearsDetails).isNull();
    }

    @Test
    void shouldNotBuildRentArrearsWithRentArrearsDataNotSet() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .rentDetails(RentDetails.builder().build())
            .rentArrears(RentArrearsSection.builder().build())
            .build();

        // When
        RentArrearsTabDetails rentArrearsDetails = rentArrearsTabDetailsBuilder
            .buildRentArrearsTabDetails(pcsCase);

        // Then
        assertThat(rentArrearsDetails).isNull();
    }

    @Test
    void shouldNotBuildSummaryRentArrearsWhenRentArrearsGroundIsNoLongerSelected() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .claimGroundSummaries(List.of(listValue(ClaimGroundSummary.builder()
                                                    .label("Breach of tenancy conditions (ground 12)")
                                                    .isRentArrears(YesOrNo.NO)
                                                    .build())))
            .rentDetails(RentDetails.builder()
                             .currentRent(new BigDecimal("100.00"))
                             .frequency(RentPaymentFrequency.WEEKLY)
                             .dailyCharge(new BigDecimal("14.29"))
                             .build())
            .rentArrears(RentArrearsSection.builder()
                             .total(new BigDecimal("200.00"))
                             .build())
            .arrearsJudgmentWanted(VerticalYesNo.YES)
            .build();

        // When
        RentArrearsTabDetails rentArrearsDetails = rentArrearsTabDetailsBuilder
            .buildRentArrearsTabDetails(pcsCase);

        // Then
        assertThat(rentArrearsDetails).isNull();
    }

    @Test
    void shouldBuildSummaryRentArrearsWhenCurrentGroundSummariesIncludeRentArrears() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .claimGroundSummaries(List.of(listValue(ClaimGroundSummary.builder()
                                                    .label("Rent arrears (ground 10)")
                                                    .isRentArrears(YesOrNo.YES)
                                                    .build())))
            .rentDetails(RentDetails.builder()
                             .currentRent(new BigDecimal("100.00"))
                             .frequency(RentPaymentFrequency.WEEKLY)
                             .build())
            .build();

        // When
        RentArrearsTabDetails rentArrearsDetails = rentArrearsTabDetailsBuilder
            .buildRentArrearsTabDetails(pcsCase);

        // Then
        assertThat(rentArrearsDetails.getRentAmount()).isEqualTo("£100");
    }

    @Test
    void shouldBuildDetailedRentArrearsForCaseDetailsTab() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .showRentSectionPage(YesOrNo.YES)
            .rentDetails(
                RentDetails.builder()
                    .currentRent(new BigDecimal("4.00"))
                    .frequency(RentPaymentFrequency.WEEKLY)
                    .calculatedDailyCharge(new BigDecimal("3.40"))
                    .build())
            .arrearsJudgmentWanted(VerticalYesNo.YES)
            .rentArrears(
                RentArrearsSection.builder()
                    .recoveryAttempted(VerticalYesNo.YES)
                    .recoveryAttemptDetails("recovery details")
                    .total(new BigDecimal("100.00"))
                    .statementDocuments(
                        List.of(
                            ListValue.<Document>builder()
                                .value(Document.builder().build())
                                .build())
                    )
                    .build()
            )
            .build();

        // When
        RentArrearsTabDetails rentArrearsDetails = rentArrearsTabDetailsBuilder
            .buildDetailedRentArrearsTabDetails(pcsCase);

        // Then
        assertThat(rentArrearsDetails.getRentAmount()).isEqualTo("£4");
        assertThat(rentArrearsDetails.getCalculationFrequency()).isEqualTo("Weekly");
        assertThat(rentArrearsDetails.getRentFrequency()).isNull();
        assertThat(rentArrearsDetails.getFrequency()).isNull();
        assertThat(rentArrearsDetails.getDailyRate()).isEqualTo("£3.40");
        assertThat(rentArrearsDetails.getArrearsTotal()).isEqualTo("£100");
        assertThat(rentArrearsDetails.getStepsToRecoverArrears()).isEqualTo("Yes");
        assertThat(rentArrearsDetails.getStepsToRecoverArrearsDetails()).isEqualTo("recovery details");
        assertThat(rentArrearsDetails.getRentStatement()).hasSize(1);
        assertThat(rentArrearsDetails.getRentStatementPlaceholder()).isNull();
        assertThat(rentArrearsDetails.getJudgmentRequested()).isEqualTo("Yes");
    }

    @Test
    void shouldUsePlaceholderInDetailedRentArrearsWhenNoDocumentsUploaded() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .showRentSectionPage(YesOrNo.YES)
            .rentDetails(
                RentDetails.builder()
                    .currentRent(new BigDecimal("4.00"))
                    .frequency(RentPaymentFrequency.WEEKLY)
                    .calculatedDailyCharge(new BigDecimal("3.40"))
                    .build())
            .arrearsJudgmentWanted(VerticalYesNo.YES)
            .rentArrears(
                RentArrearsSection.builder()
                    .recoveryAttempted(VerticalYesNo.YES)
                    .recoveryAttemptDetails("recovery details")
                    .total(new BigDecimal("100.00"))
                    .build()
            )
            .build();

        // When
        RentArrearsTabDetails rentArrearsDetails = rentArrearsTabDetailsBuilder
            .buildDetailedRentArrearsTabDetails(pcsCase);

        // Then
        assertThat(rentArrearsDetails.getRentStatement()).isNull();
        assertThat(rentArrearsDetails.getRentStatementPlaceholder()).isEqualTo(" ");
    }

    @Test
    void shouldSetFrequencyInDetailedRentArrearsWhenCalculationFrequencyIsOther() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .showRentSectionPage(YesOrNo.YES)
            .rentDetails(
                RentDetails.builder()
                    .currentRent(new BigDecimal("4.00"))
                    .frequency(RentPaymentFrequency.OTHER)
                    .otherFrequency("Other frequency")
                    .calculatedDailyCharge(new BigDecimal("3.40"))
                    .build())
            .arrearsJudgmentWanted(VerticalYesNo.YES)
            .rentArrears(
                RentArrearsSection.builder()
                    .recoveryAttempted(VerticalYesNo.YES)
                    .recoveryAttemptDetails("recovery details")
                    .total(new BigDecimal("100.00"))
                    .statementDocuments(
                        List.of(
                            ListValue.<Document>builder()
                                .value(Document.builder().build())
                                .build())
                    )
                    .build()
            )
            .build();

        // When
        RentArrearsTabDetails rentArrearsDetails = rentArrearsTabDetailsBuilder
            .buildDetailedRentArrearsTabDetails(pcsCase);

        // Then
        assertThat(rentArrearsDetails.getCalculationFrequency()).isEqualTo("Other");
        assertThat(rentArrearsDetails.getFrequency()).isEqualTo("Other frequency");
    }

    @Test
    void shouldUsePlaceholderInDetailedRentArrearsWhenRentDetailsIsNull() {
        // Given
        PCSCase pcsCase = PCSCase.builder().showRentSectionPage(YesOrNo.YES).build();

        // When
        RentArrearsTabDetails rentArrearsDetails = rentArrearsTabDetailsBuilder
            .buildDetailedRentArrearsTabDetails(pcsCase);

        // Then
        assertThat(rentArrearsDetails.getRentAmount()).isEqualTo(" ");
        assertThat(rentArrearsDetails.getCalculationFrequency()).isEqualTo(" ");
        assertThat(rentArrearsDetails.getFrequency()).isNull();
        assertThat(rentArrearsDetails.getDailyRate()).isEqualTo(" ");
        assertThat(rentArrearsDetails.getArrearsTotal()).isEqualTo(" ");
        assertThat(rentArrearsDetails.getStepsToRecoverArrears()).isEqualTo(" ");
        assertThat(rentArrearsDetails.getStepsToRecoverArrearsDetails()).isNull();
        assertThat(rentArrearsDetails.getRentStatement()).isNull();
        assertThat(rentArrearsDetails.getRentStatementPlaceholder()).isEqualTo(" ");
        assertThat(rentArrearsDetails.getJudgmentRequested()).isEqualTo(" ");
    }

    @Test
    void shouldNotBuildDetailedRentArrearsWhenRentSectionPageIsNo() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .showRentSectionPage(YesOrNo.NO)
            .rentDetails(
                RentDetails.builder()
                    .currentRent(new BigDecimal("4.00"))
                    .frequency(RentPaymentFrequency.WEEKLY)
                    .calculatedDailyCharge(new BigDecimal("3.40"))
                    .build())
            .arrearsJudgmentWanted(VerticalYesNo.YES)
            .rentArrears(
                RentArrearsSection.builder()
                    .recoveryAttempted(VerticalYesNo.YES)
                    .recoveryAttemptDetails("recovery details")
                    .total(new BigDecimal("100.00"))
                    .statementDocuments(
                        List.of(
                            ListValue.<Document>builder()
                                .value(Document.builder().build())
                                .build())
                    )
                    .build()
            )
            .build();

        // When
        RentArrearsTabDetails rentArrearsDetails = rentArrearsTabDetailsBuilder
            .buildDetailedRentArrearsTabDetails(pcsCase);

        // Then
        assertThat(rentArrearsDetails).isNull();
    }

    @Test
    void shouldBuildDetailedRentArrearsWithPlaceholdersWhenRentArrearsDataNotSet() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .showRentSectionPage(YesOrNo.YES)
            .rentDetails(RentDetails.builder().build())
            .rentArrears(RentArrearsSection.builder().build())
            .build();

        // When
        RentArrearsTabDetails rentArrearsDetails = rentArrearsTabDetailsBuilder
            .buildDetailedRentArrearsTabDetails(pcsCase);

        // Then
        assertThat(rentArrearsDetails.getRentAmount()).isEqualTo(" ");
        assertThat(rentArrearsDetails.getCalculationFrequency()).isEqualTo(" ");
        assertThat(rentArrearsDetails.getFrequency()).isNull();
        assertThat(rentArrearsDetails.getDailyRate()).isEqualTo(" ");
        assertThat(rentArrearsDetails.getArrearsTotal()).isEqualTo(" ");
        assertThat(rentArrearsDetails.getStepsToRecoverArrears()).isEqualTo(" ");
        assertThat(rentArrearsDetails.getStepsToRecoverArrearsDetails()).isNull();
        assertThat(rentArrearsDetails.getRentStatement()).isNull();
        assertThat(rentArrearsDetails.getRentStatementPlaceholder()).isEqualTo(" ");
        assertThat(rentArrearsDetails.getJudgmentRequested()).isEqualTo(" ");
    }

    @Test
    void shouldSetRentFrequencyInDetailedRentArrearsWhenCountryIsWales() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .legislativeCountry(LegislativeCountry.WALES)
            .showRentSectionPage(YesOrNo.YES)
            .rentDetails(
                RentDetails.builder()
                    .currentRent(new BigDecimal("4.00"))
                    .frequency(RentPaymentFrequency.WEEKLY)
                    .calculatedDailyCharge(new BigDecimal("3.40"))
                    .build())
            .arrearsJudgmentWanted(VerticalYesNo.YES)
            .rentArrears(
                RentArrearsSection.builder()
                    .recoveryAttempted(VerticalYesNo.YES)
                    .recoveryAttemptDetails("recovery details")
                    .total(new BigDecimal("100.00"))
                    .statementDocuments(
                        List.of(
                            ListValue.<Document>builder()
                                .value(Document.builder().build())
                                .build())
                    )
                    .build()
            )
            .build();

        // When
        RentArrearsTabDetails rentArrearsDetails = rentArrearsTabDetailsBuilder.buildDetailedRentArrearsTabDetails(pcsCase);

        // Then
        assertThat(rentArrearsDetails.getCalculationFrequency()).isNull();
        assertThat(rentArrearsDetails.getRentFrequency()).isEqualTo("Weekly");
    }

    private static <T> ListValue<T> listValue(T value) {
        return ListValue.<T>builder()
            .value(value)
            .build();
    }
}
