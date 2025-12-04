package uk.gov.hmcts.reform.pcs.ccd.service.routing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.IntroductoryDemotedOrOtherGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.IntroductoryDemotedOtherGroundsForPossession;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static uk.gov.hmcts.reform.pcs.ccd.domain.IntroductoryDemotedOrOtherGrounds.ABSOLUTE_GROUNDS;
import static uk.gov.hmcts.reform.pcs.ccd.domain.IntroductoryDemotedOrOtherGrounds.ANTI_SOCIAL;
import static uk.gov.hmcts.reform.pcs.ccd.domain.IntroductoryDemotedOrOtherGrounds.BREACH_OF_THE_TENANCY;
import static uk.gov.hmcts.reform.pcs.ccd.domain.IntroductoryDemotedOrOtherGrounds.RENT_ARREARS;
import static uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType.ASSURED_TENANCY;
import static uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType.DEMOTED_TENANCY;
import static uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType.FLEXIBLE_TENANCY;
import static uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType.INTRODUCTORY_TENANCY;
import static uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType.OTHER;
import static uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType.SECURE_TENANCY;

class IntroductoryDemotedOtherRentSectionRoutingPolicyTest {

    private IntroductoryDemotedOtherRentSectionRoutingPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new IntroductoryDemotedOtherRentSectionRoutingPolicy();
    }

    @Test
    void shouldSupportIntroductoryTenancy() {
        assertThat(policy.supports(INTRODUCTORY_TENANCY)).isTrue();
    }

    @Test
    void shouldSupportDemotedTenancy() {
        assertThat(policy.supports(DEMOTED_TENANCY)).isTrue();
    }

    @Test
    void shouldSupportOtherTenancy() {
        assertThat(policy.supports(OTHER)).isTrue();
    }

    @Test
    void shouldNotSupportOtherTenancyTypes() {
        assertThat(policy.supports(ASSURED_TENANCY)).isFalse();
        assertThat(policy.supports(SECURE_TENANCY)).isFalse();
        assertThat(policy.supports(FLEXIBLE_TENANCY)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("provideIntroductoryDemotedOtherScenarios")
    void shouldShowRentDetailsBasedOnGroundSelection(
        TenancyLicenceType tenancyType,
        VerticalYesNo hasGroundsForPossession,
        Set<IntroductoryDemotedOrOtherGrounds> grounds,
        YesOrNo expected) {

        IntroductoryDemotedOtherGroundsForPossession introductoryDemotedOtherGroundsForPossession =
            IntroductoryDemotedOtherGroundsForPossession.builder()
                .hasIntroductoryDemotedOtherGroundsForPossession(hasGroundsForPossession)
                .introductoryDemotedOrOtherGrounds(grounds)
                .build();

        PCSCase caseData = PCSCase.builder()
            .typeOfTenancyLicence(tenancyType)
            .introductoryDemotedOrOtherGroundsForPossession(introductoryDemotedOtherGroundsForPossession)
            .build();

        YesOrNo result = policy.shouldShowRentSection(caseData);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void shouldReturnNoWhenHasGroundsForPossessionIsNo() {

        IntroductoryDemotedOtherGroundsForPossession introductoryDemotedOtherGroundsForPossession =
            IntroductoryDemotedOtherGroundsForPossession.builder()
                .hasIntroductoryDemotedOtherGroundsForPossession(VerticalYesNo.NO)
                .introductoryDemotedOrOtherGrounds(null)
                .build();

        PCSCase caseData = PCSCase.builder()
            .typeOfTenancyLicence(INTRODUCTORY_TENANCY)
            .introductoryDemotedOrOtherGroundsForPossession(introductoryDemotedOtherGroundsForPossession)
            .build();

        YesOrNo result = policy.shouldShowRentSection(caseData);

        assertThat(result).isEqualTo(YesOrNo.NO);
    }

    @Test
    void shouldReturnNoWhenHasGroundsForPossessionIsNull() {

        IntroductoryDemotedOtherGroundsForPossession introductoryDemotedOtherGroundsForPossession =
            IntroductoryDemotedOtherGroundsForPossession.builder()
                .hasIntroductoryDemotedOtherGroundsForPossession(null)
                .introductoryDemotedOrOtherGrounds(Set.of(RENT_ARREARS))
                .build();

        PCSCase caseData = PCSCase.builder()
            .typeOfTenancyLicence(INTRODUCTORY_TENANCY)
            .introductoryDemotedOrOtherGroundsForPossession(introductoryDemotedOtherGroundsForPossession)
            .build();

        YesOrNo result = policy.shouldShowRentSection(caseData);

        assertThat(result).isEqualTo(YesOrNo.NO);
    }

    @Test
    void shouldReturnNoWhenGroundsIsNull() {

        IntroductoryDemotedOtherGroundsForPossession introductoryDemotedOtherGroundsForPossession =
            IntroductoryDemotedOtherGroundsForPossession.builder()
                .hasIntroductoryDemotedOtherGroundsForPossession(VerticalYesNo.YES)
                .introductoryDemotedOrOtherGrounds(null)
                .build();

        PCSCase caseData = PCSCase.builder()
            .typeOfTenancyLicence(INTRODUCTORY_TENANCY)
            .introductoryDemotedOrOtherGroundsForPossession(introductoryDemotedOtherGroundsForPossession)
            .build();

        YesOrNo result = policy.shouldShowRentSection(caseData);

        assertThat(result).isEqualTo(YesOrNo.NO);
    }

    @Test
    void shouldReturnNoWhenGroundsIsEmpty() {

        IntroductoryDemotedOtherGroundsForPossession introductoryDemotedOtherGroundsForPossession =
            IntroductoryDemotedOtherGroundsForPossession.builder()
                .hasIntroductoryDemotedOtherGroundsForPossession(VerticalYesNo.YES)
                .introductoryDemotedOrOtherGrounds(Set.of())
                .build();

        PCSCase caseData = PCSCase.builder()
            .typeOfTenancyLicence(INTRODUCTORY_TENANCY)
            .introductoryDemotedOrOtherGroundsForPossession(introductoryDemotedOtherGroundsForPossession)
            .build();

        YesOrNo result = policy.shouldShowRentSection(caseData);

        assertThat(result).isEqualTo(YesOrNo.NO);
    }

    private static Stream<Arguments> provideIntroductoryDemotedOtherScenarios() {
        return Stream.of(
            // RENT_ARREARS selected - Should show Rent Details
            arguments(INTRODUCTORY_TENANCY, VerticalYesNo.YES, Set.of(RENT_ARREARS), YesOrNo.YES),
            arguments(DEMOTED_TENANCY, VerticalYesNo.YES, Set.of(RENT_ARREARS), YesOrNo.YES),
            arguments(OTHER, VerticalYesNo.YES, Set.of(RENT_ARREARS), YesOrNo.YES),

            // Other grounds (not rent arrears) - Should NOT show Rent Details
            arguments(INTRODUCTORY_TENANCY, VerticalYesNo.YES, Set.of(ANTI_SOCIAL), YesOrNo.NO),
            arguments(DEMOTED_TENANCY, VerticalYesNo.YES, Set.of(BREACH_OF_THE_TENANCY), YesOrNo.NO),
            arguments(OTHER, VerticalYesNo.YES, Set.of(ABSOLUTE_GROUNDS), YesOrNo.NO),

            // Multiple grounds including RENT_ARREARS - Should show Rent Details
            arguments(
                INTRODUCTORY_TENANCY,
                VerticalYesNo.YES,
                Set.of(RENT_ARREARS, ANTI_SOCIAL, BREACH_OF_THE_TENANCY),
                YesOrNo.YES
            ),

            // Multiple grounds without RENT_ARREARS - Should NOT show Rent Details
            arguments(
                DEMOTED_TENANCY,
                VerticalYesNo.YES,
                Set.of(ANTI_SOCIAL, BREACH_OF_THE_TENANCY, ABSOLUTE_GROUNDS),
                YesOrNo.NO
            ),

            // Has grounds for possession is NO - Should NOT show Rent Details
            arguments(INTRODUCTORY_TENANCY, VerticalYesNo.NO, null, YesOrNo.NO)
        );
    }
}

