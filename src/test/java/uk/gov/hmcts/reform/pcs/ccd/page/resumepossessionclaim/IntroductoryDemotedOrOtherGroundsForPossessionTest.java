package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.IntroductoryDemotedOrOtherGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class IntroductoryDemotedOrOtherGroundsForPossessionTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new IntroductoryDemotedOrOtherGroundsForPossession());
    }

    @ParameterizedTest
    @MethodSource("provideRentDetailsPageScenarios")
    @DisplayName("Should set rent details page flag based on grounds selection")
    void shouldSetRentDetailsPageFlagForGroundsSelection(
        TenancyLicenceType tenancyType,
        Set<IntroductoryDemotedOrOtherGrounds> grounds,
        YesOrNo expectedShowRentDetailsPage) {
        // Given
        PCSCase caseData = PCSCase.builder()
            .typeOfTenancyLicence(tenancyType)
            .hasIntroductoryDemotedOtherGroundsForPossession(VerticalYesNo.YES)
            .introductoryDemotedOrOtherGrounds(grounds)
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        PCSCase updatedCaseData = response.getData();
        assertThat(updatedCaseData.getShowRentDetailsPage()).isEqualTo(expectedShowRentDetailsPage);
    }

    @ParameterizedTest
    @MethodSource("provideNoGroundsForPossessionScenarios")
    @DisplayName("Should set rent details page flag when no grounds for possession")
    void shouldSetRentDetailsPageFlagWhenNoGroundsSelected(
        TenancyLicenceType tenancyType,
        YesOrNo expectedShowRentDetailsPage) {
        // Given
        PCSCase caseData = PCSCase.builder()
            .typeOfTenancyLicence(tenancyType)
            .hasIntroductoryDemotedOtherGroundsForPossession(VerticalYesNo.NO)
            .introductoryDemotedOrOtherGrounds(null) // No grounds selected
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        PCSCase updatedCaseData = response.getData();
        assertThat(updatedCaseData.getShowRentDetailsPage()).isEqualTo(expectedShowRentDetailsPage);
    }

    private static Stream<Arguments> provideRentDetailsPageScenarios() {
        return Stream.of(
            // Rent arrears selected → show rent details
            arguments(TenancyLicenceType.INTRODUCTORY_TENANCY, rentArrearsOnly(), YesOrNo.YES),
            arguments(TenancyLicenceType.DEMOTED_TENANCY, rentArrearsOnly(), YesOrNo.YES),
            arguments(TenancyLicenceType.OTHER, rentArrearsOnly(), YesOrNo.YES),

            // Other grounds (not rent arrears) → don't show rent details
            arguments(TenancyLicenceType.INTRODUCTORY_TENANCY, antiSocialOnly(), YesOrNo.NO),
            arguments(TenancyLicenceType.DEMOTED_TENANCY, breachOfTenancyOnly(), YesOrNo.NO),
            arguments(TenancyLicenceType.OTHER, absoluteGroundsOnly(), YesOrNo.NO),

            // Multiple grounds including rent arrears → show rent details
            arguments(TenancyLicenceType.INTRODUCTORY_TENANCY, rentArrearsWithAntiSocial(), YesOrNo.YES),

            // No grounds selected → don't show rent details
            arguments(TenancyLicenceType.INTRODUCTORY_TENANCY, noGrounds(), YesOrNo.NO),

            // Multiple grounds without rent arrears → don't show rent details
            arguments(TenancyLicenceType.DEMOTED_TENANCY, antiSocialWithBreach(), YesOrNo.NO),
            arguments(TenancyLicenceType.OTHER, absoluteGroundsWithAntiSocial(), YesOrNo.NO)
        );
    }

    private static Stream<Arguments> provideNoGroundsForPossessionScenarios() {
        return Stream.of(
            // No grounds for possession → don't show rent details
            arguments(TenancyLicenceType.INTRODUCTORY_TENANCY, YesOrNo.NO),
            arguments(TenancyLicenceType.DEMOTED_TENANCY, YesOrNo.NO),
            arguments(TenancyLicenceType.OTHER, YesOrNo.NO)
        );
    }

    // Helper methods for common ground combinations
    private static Set<IntroductoryDemotedOrOtherGrounds> rentArrearsOnly() {
        return Set.of(IntroductoryDemotedOrOtherGrounds.RENT_ARREARS);
    }

    private static Set<IntroductoryDemotedOrOtherGrounds> antiSocialOnly() {
        return Set.of(IntroductoryDemotedOrOtherGrounds.ANTI_SOCIAL);
    }

    private static Set<IntroductoryDemotedOrOtherGrounds> breachOfTenancyOnly() {
        return Set.of(IntroductoryDemotedOrOtherGrounds.BREACH_OF_THE_TENANCY);
    }

    private static Set<IntroductoryDemotedOrOtherGrounds> absoluteGroundsOnly() {
        return Set.of(IntroductoryDemotedOrOtherGrounds.ABSOLUTE_GROUNDS);
    }

    private static Set<IntroductoryDemotedOrOtherGrounds> rentArrearsWithAntiSocial() {
        return Set.of(IntroductoryDemotedOrOtherGrounds.RENT_ARREARS,
                      IntroductoryDemotedOrOtherGrounds.ANTI_SOCIAL);
    }

    private static Set<IntroductoryDemotedOrOtherGrounds> antiSocialWithBreach() {
        return Set.of(IntroductoryDemotedOrOtherGrounds.ANTI_SOCIAL,
                      IntroductoryDemotedOrOtherGrounds.BREACH_OF_THE_TENANCY);
    }

    private static Set<IntroductoryDemotedOrOtherGrounds> absoluteGroundsWithAntiSocial() {
        return Set.of(IntroductoryDemotedOrOtherGrounds.ABSOLUTE_GROUNDS,
                      IntroductoryDemotedOrOtherGrounds.ANTI_SOCIAL);
    }

    private static Set<IntroductoryDemotedOrOtherGrounds> noGrounds() {
        return Set.of();
    }
}
