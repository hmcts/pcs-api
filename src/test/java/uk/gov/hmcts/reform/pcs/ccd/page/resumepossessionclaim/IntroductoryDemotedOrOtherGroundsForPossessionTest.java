package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.junit.jupiter.api.BeforeEach;
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
    void shouldSetCorrectShowRentDetailsPageFlagForIntroductoryDemotedOther(
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

    private static Stream<Arguments> provideRentDetailsPageScenarios() {
        return Stream.of(
            // AC01 & AC02: Introductory Tenancy + Rent Arrears selected - Should show Rent Details
            arguments(TenancyLicenceType.INTRODUCTORY_TENANCY,
                      Set.of(IntroductoryDemotedOrOtherGrounds.RENT_ARREARS),
                      YesOrNo.YES),
            // AC01 & AC02: Demoted Tenancy + Rent Arrears selected - Should show Rent Details
            arguments(TenancyLicenceType.DEMOTED_TENANCY,
                      Set.of(IntroductoryDemotedOrOtherGrounds.RENT_ARREARS),
                      YesOrNo.YES),
            // AC01 & AC02: Other Tenancy + Rent Arrears selected - Should show Rent Details
            arguments(TenancyLicenceType.OTHER,
                      Set.of(IntroductoryDemotedOrOtherGrounds.RENT_ARREARS),
                      YesOrNo.YES),

            // Introductory Tenancy + Other grounds (not rent arrears) - Should NOT show Rent Details
            arguments(TenancyLicenceType.INTRODUCTORY_TENANCY,
                      Set.of(IntroductoryDemotedOrOtherGrounds.ANTI_SOCIAL),
                      YesOrNo.NO),
            // Demoted Tenancy + Other grounds (not rent arrears) - Should NOT show Rent Details
            arguments(TenancyLicenceType.DEMOTED_TENANCY,
                      Set.of(IntroductoryDemotedOrOtherGrounds.BREACH_OF_THE_TENANCY),
                      YesOrNo.NO),
            // Other Tenancy + Other grounds (not rent arrears) - Should NOT show Rent Details
            arguments(TenancyLicenceType.OTHER,
                      Set.of(IntroductoryDemotedOrOtherGrounds.ABSOLUTE_GROUNDS),
                      YesOrNo.NO),

            // Introductory Tenancy + Multiple grounds including Rent Arrears - Should show Rent Details
            arguments(TenancyLicenceType.INTRODUCTORY_TENANCY,
                      Set.of(IntroductoryDemotedOrOtherGrounds.RENT_ARREARS,
                             IntroductoryDemotedOrOtherGrounds.ANTI_SOCIAL),
                      YesOrNo.YES),
            // Introductory Tenancy + No grounds selected - Should NOT show Rent Details
            arguments(TenancyLicenceType.INTRODUCTORY_TENANCY,
                      Set.of(),
                      YesOrNo.NO)
        );
    }
}
