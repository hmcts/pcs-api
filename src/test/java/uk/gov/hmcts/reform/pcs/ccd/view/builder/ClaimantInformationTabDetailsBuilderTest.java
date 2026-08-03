package uk.gov.hmcts.reform.pcs.ccd.view.builder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantInformation;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared.ClaimantInformationTabDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ClaimantInformationTabDetailsBuilderTest {

    private ClaimantInformationTabDetailsBuilder claimantInformationTabDetailsBuilder;

    @BeforeEach
    void setUp() {
        claimantInformationTabDetailsBuilder = new ClaimantInformationTabDetailsBuilder();
    }

    @Test
    void shouldSetSummaryClaimantNameFromFallbackName() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .claimantInformation(ClaimantInformation.builder()
                                     .orgNameFound(YesOrNo.NO)
                                     .fallbackClaimantName("Fallback claimant")
                                     .build())
            .build();

        // When
        ClaimantInformationTabDetails claimantInformationTabDetails =
            claimantInformationTabDetailsBuilder.createSummaryClaimantTabDetails(pcsCase);

        // Then
        assertThat(claimantInformationTabDetails.getClaimantName()).isEqualTo("Fallback claimant");
    }

    @Test
    void shouldSetSummaryClaimantNameFromOverriddenName() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .claimantInformation(ClaimantInformation.builder()
                                     .isClaimantNameCorrect(VerticalYesNo.NO)
                                     .overriddenClaimantName("Overridden claimant")
                                     .build())
            .build();

        // When
        ClaimantInformationTabDetails claimantInformationTabDetails =
            claimantInformationTabDetailsBuilder.createSummaryClaimantTabDetails(pcsCase);

        // Then
        assertThat(claimantInformationTabDetails.getClaimantName()).isEqualTo("Overridden claimant");
    }

    @Test
    void shouldSetSummaryClaimantNameFromClaimantInformationName() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .claimantInformation(ClaimantInformation.builder()
                                     .claimantName("Claimant information name")
                                     .build())
            .build();

        // When
        ClaimantInformationTabDetails claimantInformationTabDetails =
            claimantInformationTabDetailsBuilder.createSummaryClaimantTabDetails(pcsCase);

        // Then
        assertThat(claimantInformationTabDetails.getClaimantName()).isEqualTo("Claimant information name");
    }

    @Test
    void shouldSetSummaryClaimantNameFromAllClaimants() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .allClaimants(List.of(listValue(Party.builder().orgName("Claimant party").build())))
            .build();

        // When
        ClaimantInformationTabDetails claimantInformationTabDetails =
            claimantInformationTabDetailsBuilder.createSummaryClaimantTabDetails(pcsCase);

        // Then
        assertThat(claimantInformationTabDetails.getClaimantName()).isEqualTo("Claimant party");
    }

    @Test
    void shouldReturnNullWithNoClaimantData() {
        // Given
        PCSCase pcsCase = PCSCase.builder().build();

        // When
        ClaimantInformationTabDetails claimantInformationTabDetails =
            claimantInformationTabDetailsBuilder.createSummaryClaimantTabDetails(pcsCase);

        // Then
        assertThat(claimantInformationTabDetails).isNull();
    }

    private static <T> ListValue<T> listValue(T value) {
        return ListValue.<T>builder()
            .value(value)
            .build();
    }
}
