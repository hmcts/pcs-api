package uk.gov.hmcts.reform.pcs.ccd.service.rules;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsOrBreachOfTenancy;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredAdditionalDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredAdditionalOtherGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredDiscretionaryGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredMandatoryGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredNoArrearsPossessionGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredRentArrearsPossessionGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.IntroductoryDemotedOrOtherGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.IntroductoryDemotedOtherGroundsForPossession;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureAntisocialAdditionalGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleMandatoryGroundsAlternativeAccomm;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexiblePossessionGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.DiscretionaryGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.EstateManagementGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.GroundsForPossessionWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.MandatoryGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractDiscretionaryGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractGroundsForPossessionWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractMandatoryGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;
import uk.gov.hmcts.reform.pcs.ccd.service.rules.GroundsRules.WalesGroundsRouting;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GroundsRulesTest {

    private static final String TOO_LONG = "x".repeat(TextAreaValidationService.MEDIUM_TEXT_LIMIT + 1);

    private final GroundsRules underTest = new GroundsRules(new TextAreaValidationService());

    // ---- other ground description ----

    @Test
    void shouldAcceptMissingOrShortOtherGroundDescription() {
        assertThat(underTest.validateOtherGroundDescription(null)).isEmpty();
        assertThat(underTest.validateOtherGroundDescription("short")).isEmpty();
    }

    @Test
    void shouldRejectOverlongOtherGroundDescription() {
        assertThat(underTest.validateOtherGroundDescription(TOO_LONG)).hasSize(1);
    }

    // ---- secure or flexible ----

    private SecureOrFlexiblePossessionGrounds.SecureOrFlexiblePossessionGroundsBuilder emptySecureOrFlexible() {
        return SecureOrFlexiblePossessionGrounds.builder()
            .secureOrFlexibleDiscretionaryGrounds(Set.of())
            .secureOrFlexibleDiscretionaryGroundsAlt(Set.of())
            .secureOrFlexibleMandatoryGrounds(Set.of())
            .secureOrFlexibleMandatoryGroundsAlt(Set.of())
            .secureAntisocialAdditionalGrounds(Set.of());
    }

    @Test
    void shouldRequireAtLeastOneSecureOrFlexibleGround() {
        assertThat(underTest.validateSecureOrFlexibleGrounds(emptySecureOrFlexible().build()))
            .containsExactly("Please select at least one ground");
    }

    @Test
    void shouldRequireAbsoluteGroundWhenAntisocialMandatoryGroundSelected() {
        SecureOrFlexiblePossessionGrounds grounds = emptySecureOrFlexible()
            .secureOrFlexibleMandatoryGrounds(Set.of(SecureOrFlexibleMandatoryGrounds.ANTI_SOCIAL))
            .build();

        assertThat(underTest.validateSecureOrFlexibleGrounds(grounds)).containsExactly(
            "Please select at least one absolute ground for possession for antisocial behaviour");
    }

    @Test
    void shouldAcceptAntisocialWithAbsoluteGround() {
        SecureOrFlexiblePossessionGrounds grounds = emptySecureOrFlexible()
            .secureOrFlexibleMandatoryGrounds(Set.of(SecureOrFlexibleMandatoryGrounds.ANTI_SOCIAL))
            .secureAntisocialAdditionalGrounds(Set.of(SecureAntisocialAdditionalGrounds.S84A_CONDITION_1))
            .build();

        assertThat(underTest.validateSecureOrFlexibleGrounds(grounds)).isEmpty();
    }

    @Test
    void shouldDetectRentArrearsOrBreachGroundSelection() {
        SecureOrFlexiblePossessionGrounds with = emptySecureOrFlexible()
            .secureOrFlexibleDiscretionaryGrounds(
                Set.of(SecureOrFlexibleDiscretionaryGrounds.RENT_ARREARS_OR_BREACH_OF_TENANCY))
            .build();

        assertThat(underTest.isRentArrearsOrBreachOfTenancyGroundSelected(with)).isTrue();
        assertThat(underTest.isRentArrearsOrBreachOfTenancyGroundSelected(emptySecureOrFlexible().build()))
            .isFalse();
    }

    @Test
    void shouldHideSecureOrFlexibleReasonsWhenOnlyRentArrearsOrBreachSelected() {
        SecureOrFlexiblePossessionGrounds grounds = emptySecureOrFlexible()
            .secureOrFlexibleDiscretionaryGrounds(
                Set.of(SecureOrFlexibleDiscretionaryGrounds.RENT_ARREARS_OR_BREACH_OF_TENANCY))
            .build();

        assertThat(underTest.showReasonsForSecureOrFlexibleGrounds(grounds)).isEqualTo(YesOrNo.NO);
    }

    @Test
    void shouldShowSecureOrFlexibleReasonsForOtherDiscretionaryGround() {
        SecureOrFlexiblePossessionGrounds grounds = emptySecureOrFlexible()
            .secureOrFlexibleDiscretionaryGrounds(Set.of(SecureOrFlexibleDiscretionaryGrounds.NUISANCE_OR_IMMORAL_USE))
            .build();

        assertThat(underTest.showReasonsForSecureOrFlexibleGrounds(grounds)).isEqualTo(YesOrNo.YES);
    }

    @Test
    void shouldShowSecureOrFlexibleReasonsForMandatoryGround() {
        SecureOrFlexiblePossessionGrounds grounds = emptySecureOrFlexible()
            .secureOrFlexibleMandatoryGrounds(Set.of(SecureOrFlexibleMandatoryGrounds.ANTI_SOCIAL))
            .build();

        assertThat(underTest.showReasonsForSecureOrFlexibleGrounds(grounds)).isEqualTo(YesOrNo.YES);
    }

    @Test
    void shouldShowSecureOrFlexibleReasonsForAlternativeAccommodationGrounds() {
        SecureOrFlexiblePossessionGrounds discretionaryAlt = emptySecureOrFlexible()
            .secureOrFlexibleDiscretionaryGroundsAlt(Set.of(
                SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm.values()[0]))
            .build();
        SecureOrFlexiblePossessionGrounds mandatoryAlt = emptySecureOrFlexible()
            .secureOrFlexibleMandatoryGroundsAlt(Set.of(
                SecureOrFlexibleMandatoryGroundsAlternativeAccomm.values()[0]))
            .build();

        assertThat(underTest.showReasonsForSecureOrFlexibleGrounds(discretionaryAlt)).isEqualTo(YesOrNo.YES);
        assertThat(underTest.showReasonsForSecureOrFlexibleGrounds(mandatoryAlt)).isEqualTo(YesOrNo.YES);
    }

    @Test
    void shouldShowBreachTextareaOnlyWhenBreachSelected() {
        assertThat(underTest.showBreachOfTenancyTextarea(Set.of(RentArrearsOrBreachOfTenancy.BREACH_OF_TENANCY)))
            .isEqualTo(YesOrNo.YES);
        assertThat(underTest.showBreachOfTenancyTextarea(
            Set.of(RentArrearsOrBreachOfTenancy.RENT_ARREARS, RentArrearsOrBreachOfTenancy.BREACH_OF_TENANCY)))
            .isEqualTo(YesOrNo.YES);
        assertThat(underTest.showBreachOfTenancyTextarea(Set.of(RentArrearsOrBreachOfTenancy.RENT_ARREARS)))
            .isEqualTo(YesOrNo.NO);
        assertThat(underTest.showBreachOfTenancyTextarea(Set.of())).isEqualTo(YesOrNo.NO);
    }

    // ---- assured: additional grounds ----

    @Test
    void shouldRequireAtLeastOneAssuredAdditionalGround() {
        AssuredRentArrearsPossessionGrounds grounds = AssuredRentArrearsPossessionGrounds.builder()
            .additionalMandatoryGrounds(Set.of())
            .additionalDiscretionaryGrounds(Set.of())
            .additionalOtherGround(Set.of())
            .build();

        assertThat(underTest.hasAtLeastOneAssuredAdditionalGround(grounds)).isFalse();
        assertThat(underTest.validateAssuredAdditionalGrounds(grounds))
            .containsExactly("Please select at least one ground");
    }

    @Test
    void shouldValidateAssuredAdditionalOtherGroundDescription() {
        AssuredRentArrearsPossessionGrounds valid = AssuredRentArrearsPossessionGrounds.builder()
            .additionalDiscretionaryGrounds(Set.of(AssuredAdditionalDiscretionaryGrounds.BREACH_TENANCY_GROUND12))
            .additionalOtherGroundDescription("fine")
            .build();
        AssuredRentArrearsPossessionGrounds tooLong = AssuredRentArrearsPossessionGrounds.builder()
            .additionalOtherGround(Set.of(AssuredAdditionalOtherGround.OTHER))
            .additionalOtherGroundDescription(TOO_LONG)
            .build();

        assertThat(underTest.validateAssuredAdditionalGrounds(valid)).isEmpty();
        assertThat(underTest.validateAssuredAdditionalGrounds(tooLong)).hasSize(1);
    }

    // ---- assured: no arrears ----

    @Test
    void shouldRequireAtLeastOneAssuredNoArrearsGround() {
        AssuredNoArrearsPossessionGrounds grounds = AssuredNoArrearsPossessionGrounds.builder()
            .mandatoryGrounds(Set.of())
            .discretionaryGrounds(Set.of())
            .otherGround(Set.of())
            .build();

        assertThat(underTest.hasAtLeastOneAssuredNoArrearsGround(grounds)).isFalse();
        assertThat(underTest.validateAssuredNoArrearsGrounds(grounds))
            .containsExactly("Please select at least one ground");
    }

    @Test
    void shouldValidateAssuredNoArrearsOtherGroundDescription() {
        AssuredNoArrearsPossessionGrounds grounds = AssuredNoArrearsPossessionGrounds.builder()
            .otherGround(Set.of(AssuredAdditionalOtherGround.OTHER))
            .otherGroundDescription(TOO_LONG)
            .build();

        assertThat(underTest.validateAssuredNoArrearsGrounds(grounds)).hasSize(1);
    }

    @Test
    void shouldHideAssuredNoArrearsReasonsForRentArrearsGroundsOnly() {
        AssuredNoArrearsPossessionGrounds grounds = AssuredNoArrearsPossessionGrounds.builder()
            .mandatoryGrounds(Set.of(AssuredMandatoryGround.SERIOUS_RENT_ARREARS_GROUND8))
            .discretionaryGrounds(Set.of(AssuredDiscretionaryGround.RENT_ARREARS_GROUND10,
                                         AssuredDiscretionaryGround.PERSISTENT_DELAY_GROUND11))
            .otherGround(Set.of())
            .build();

        assertThat(underTest.showReasonsForAssuredNoArrearsGrounds(grounds)).isEqualTo(YesOrNo.NO);
    }

    @Test
    void shouldShowAssuredNoArrearsReasonsForOtherMandatoryGround() {
        AssuredNoArrearsPossessionGrounds grounds = AssuredNoArrearsPossessionGrounds.builder()
            .mandatoryGrounds(Set.of(AssuredMandatoryGround.DEATH_OF_TENANT_GROUND7))
            .build();

        assertThat(underTest.showReasonsForAssuredNoArrearsGrounds(grounds)).isEqualTo(YesOrNo.YES);
    }

    @Test
    void shouldShowAssuredNoArrearsReasonsForOtherDiscretionaryGround() {
        AssuredNoArrearsPossessionGrounds grounds = AssuredNoArrearsPossessionGrounds.builder()
            .discretionaryGrounds(Set.of(AssuredDiscretionaryGround.BREACH_TENANCY_GROUND12))
            .build();

        assertThat(underTest.showReasonsForAssuredNoArrearsGrounds(grounds)).isEqualTo(YesOrNo.YES);
    }

    @Test
    void shouldShowAssuredNoArrearsReasonsForOtherGround() {
        AssuredNoArrearsPossessionGrounds grounds = AssuredNoArrearsPossessionGrounds.builder()
            .otherGround(Set.of(AssuredAdditionalOtherGround.OTHER))
            .build();

        assertThat(underTest.showReasonsForAssuredNoArrearsGrounds(grounds)).isEqualTo(YesOrNo.YES);
    }

    // ---- introductory, demoted or other ----

    @Test
    void shouldValidateIntroductoryDemotedOtherGroundDescription() {
        IntroductoryDemotedOtherGroundsForPossession ok = IntroductoryDemotedOtherGroundsForPossession.builder()
            .otherGroundDescription("fine").build();
        IntroductoryDemotedOtherGroundsForPossession tooLong = IntroductoryDemotedOtherGroundsForPossession.builder()
            .otherGroundDescription(TOO_LONG).build();

        assertThat(underTest.validateIntroductoryDemotedOrOtherGrounds(ok)).isEmpty();
        assertThat(underTest.validateIntroductoryDemotedOrOtherGrounds(tooLong)).hasSize(1);
    }

    @Test
    void shouldHideIntroductoryReasonsWhenOnlyRentArrearsSelected() {
        IntroductoryDemotedOtherGroundsForPossession grounds = IntroductoryDemotedOtherGroundsForPossession.builder()
            .hasIntroductoryDemotedOtherGroundsForPossession(VerticalYesNo.YES)
            .introductoryDemotedOrOtherGrounds(Set.of(IntroductoryDemotedOrOtherGrounds.RENT_ARREARS))
            .build();

        assertThat(underTest.showReasonsForIntroductoryDemotedOrOtherGrounds(grounds)).isEqualTo(YesOrNo.NO);
    }

    @Test
    void shouldShowIntroductoryReasonsForOtherGround() {
        IntroductoryDemotedOtherGroundsForPossession grounds = IntroductoryDemotedOtherGroundsForPossession.builder()
            .hasIntroductoryDemotedOtherGroundsForPossession(VerticalYesNo.YES)
            .introductoryDemotedOrOtherGrounds(Set.of(IntroductoryDemotedOrOtherGrounds.RENT_ARREARS,
                                                      IntroductoryDemotedOrOtherGrounds.ANTI_SOCIAL))
            .build();

        assertThat(underTest.showReasonsForIntroductoryDemotedOrOtherGrounds(grounds)).isEqualTo(YesOrNo.YES);
    }

    @Test
    void shouldShowIntroductoryReasonsWhenNoGroundsSelectedByClaimant() {
        IntroductoryDemotedOtherGroundsForPossession grounds = IntroductoryDemotedOtherGroundsForPossession.builder()
            .hasIntroductoryDemotedOtherGroundsForPossession(VerticalYesNo.NO)
            .build();

        assertThat(underTest.showReasonsForIntroductoryDemotedOrOtherGrounds(grounds)).isEqualTo(YesOrNo.YES);
    }

    @Test
    void shouldHideIntroductoryReasonsWhenGroundsAnswerAndSelectionMissing() {
        IntroductoryDemotedOtherGroundsForPossession grounds =
            IntroductoryDemotedOtherGroundsForPossession.builder().build();

        assertThat(underTest.showReasonsForIntroductoryDemotedOrOtherGrounds(grounds)).isEqualTo(YesOrNo.NO);
    }

    // ---- Wales standard contract ----

    private static GroundsForPossessionWales wales(Set<DiscretionaryGroundWales> discretionary,
                                                   Set<MandatoryGroundWales> mandatory,
                                                   Set<EstateManagementGroundsWales> estate) {
        return GroundsForPossessionWales.builder()
            .discretionaryGrounds(discretionary)
            .mandatoryGrounds(mandatory)
            .estateManagementGrounds(estate)
            .build();
    }

    @Test
    void shouldRequireAtLeastOneWalesGround() {
        assertThat(underTest.validateWalesGrounds(wales(Set.of(), Set.of(), Set.of())))
            .containsExactly("Please select at least one ground.");
        assertThat(underTest.validateWalesGrounds(null))
            .containsExactly("Please select at least one ground.");
        assertThat(underTest.validateWalesGrounds(GroundsForPossessionWales.builder().build()))
            .containsExactly("Please select at least one ground.");
    }

    @Test
    void shouldRequireEstateManagementSubSelectionForWalesParentGround() {
        GroundsForPossessionWales grounds = wales(
            Set.of(DiscretionaryGroundWales.ESTATE_MANAGEMENT_GROUNDS_S160), Set.of(), Set.of());

        assertThat(underTest.validateWalesGrounds(grounds))
            .containsExactly("Please select at least one ground in ‘Estate management grounds (section 160)’.");
    }

    @Test
    void shouldAcceptValidWalesGrounds() {
        GroundsForPossessionWales grounds = wales(
            Set.of(DiscretionaryGroundWales.ESTATE_MANAGEMENT_GROUNDS_S160), Set.of(),
            Set.of(EstateManagementGroundsWales.BUILDING_WORKS));

        assertThat(underTest.validateWalesGrounds(grounds)).isEmpty();
        assertThat(underTest.validateWalesGrounds(
            wales(Set.of(), Set.of(MandatoryGroundWales.SERIOUS_ARREARS_PERIODIC_S181), Set.of()))).isEmpty();
    }

    @Test
    void shouldRouteWalesRentArrearsOnlyToNeitherPage() {
        assertThat(underTest.deriveWalesGroundsRouting(
            wales(Set.of(DiscretionaryGroundWales.RENT_ARREARS_S157), Set.of(), Set.of())))
            .contains(new WalesGroundsRouting(YesOrNo.NO, YesOrNo.NO));
        assertThat(underTest.deriveWalesGroundsRouting(
            wales(Set.of(), Set.of(MandatoryGroundWales.SERIOUS_ARREARS_FIXED_TERM_S187), Set.of())))
            .contains(new WalesGroundsRouting(YesOrNo.NO, YesOrNo.NO));
    }

    @Test
    void shouldRouteWalesAsbWithRentArrearsToAsbPage() {
        assertThat(underTest.deriveWalesGroundsRouting(wales(
            Set.of(DiscretionaryGroundWales.ANTISOCIAL_BEHAVIOUR_S157, DiscretionaryGroundWales.RENT_ARREARS_S157),
            Set.of(), Set.of())))
            .contains(new WalesGroundsRouting(YesOrNo.YES, YesOrNo.NO));
    }

    @Test
    void shouldRouteWalesOtherOptionsToReasonsPage() {
        assertThat(underTest.deriveWalesGroundsRouting(wales(
            Set.of(DiscretionaryGroundWales.OTHER_BREACH_OF_CONTRACT_S157), Set.of(), Set.of())))
            .contains(new WalesGroundsRouting(YesOrNo.NO, YesOrNo.YES));
        assertThat(underTest.deriveWalesGroundsRouting(wales(
            Set.of(DiscretionaryGroundWales.ESTATE_MANAGEMENT_GROUNDS_S160), Set.of(), Set.of())))
            .contains(new WalesGroundsRouting(YesOrNo.NO, YesOrNo.YES));
        assertThat(underTest.deriveWalesGroundsRouting(wales(
            Set.of(DiscretionaryGroundWales.ANTISOCIAL_BEHAVIOUR_S157),
            Set.of(MandatoryGroundWales.LANDLORD_NOTICE_PERIODIC_S178), Set.of())))
            .contains(new WalesGroundsRouting(YesOrNo.NO, YesOrNo.YES));
    }

    @Test
    void shouldNotRouteWalesWhenNothingSelected() {
        assertThat(underTest.deriveWalesGroundsRouting(wales(Set.of(), Set.of(), Set.of()))).isEmpty();
        assertThat(underTest.deriveWalesGroundsRouting(null)).isEmpty();
    }

    // ---- Wales secure contract ----

    private static SecureContractGroundsForPossessionWales secure(
        Set<SecureContractDiscretionaryGroundsWales> discretionary,
        Set<SecureContractMandatoryGroundsWales> mandatory,
        Set<EstateManagementGroundsWales> estate) {
        return SecureContractGroundsForPossessionWales.builder()
            .discretionaryGrounds(discretionary)
            .mandatoryGrounds(mandatory)
            .estateManagementGrounds(estate)
            .build();
    }

    @Test
    void shouldRequireAtLeastOneSecureContractGround() {
        assertThat(underTest.validateSecureContractGrounds(secure(Set.of(), Set.of(), Set.of())))
            .containsExactly("Please select at least one ground");
    }

    @Test
    void shouldRequireEstateManagementSubSelectionForSecureContractParentGround() {
        SecureContractGroundsForPossessionWales grounds = secure(
            Set.of(SecureContractDiscretionaryGroundsWales.ESTATE_MANAGEMENT_GROUNDS_S160), Set.of(), Set.of());

        assertThat(underTest.validateSecureContractGrounds(grounds))
            .containsExactly("Please select at least one ground in ‘Estate management grounds (section 160)’.");
    }

    @Test
    void shouldAcceptValidSecureContractGrounds() {
        assertThat(underTest.validateSecureContractGrounds(
            secure(Set.of(SecureContractDiscretionaryGroundsWales.RENT_ARREARS_S157), Set.of(), Set.of())))
            .isEmpty();
        assertThat(underTest.validateSecureContractGrounds(
            secure(Set.of(), Set.of(SecureContractMandatoryGroundsWales.LANDLORD_NOTICE_S186), Set.of())))
            .isEmpty();
    }

    @Test
    void shouldRouteSecureContractGrounds() {
        assertThat(underTest.deriveSecureContractGroundsRouting(
            secure(Set.of(SecureContractDiscretionaryGroundsWales.RENT_ARREARS_S157), Set.of(), Set.of())))
            .contains(new WalesGroundsRouting(YesOrNo.NO, YesOrNo.NO));
        assertThat(underTest.deriveSecureContractGroundsRouting(
            secure(Set.of(SecureContractDiscretionaryGroundsWales.ANTISOCIAL_BEHAVIOUR_S157), Set.of(), Set.of())))
            .contains(new WalesGroundsRouting(YesOrNo.YES, YesOrNo.NO));
        assertThat(underTest.deriveSecureContractGroundsRouting(
            secure(Set.of(SecureContractDiscretionaryGroundsWales.OTHER_BREACH_OF_CONTRACT_S157), Set.of(), Set.of())))
            .contains(new WalesGroundsRouting(YesOrNo.NO, YesOrNo.YES));
        assertThat(underTest.deriveSecureContractGroundsRouting(
            secure(Set.of(SecureContractDiscretionaryGroundsWales.ESTATE_MANAGEMENT_GROUNDS_S160), Set.of(),
                   Set.of(EstateManagementGroundsWales.CHARITIES))))
            .contains(new WalesGroundsRouting(YesOrNo.NO, YesOrNo.YES));
        assertThat(underTest.deriveSecureContractGroundsRouting(
            secure(Set.of(), Set.of(SecureContractMandatoryGroundsWales.LANDLORD_NOTICE_S186), Set.of())))
            .contains(new WalesGroundsRouting(YesOrNo.NO, YesOrNo.YES));
    }

    @Test
    void shouldTreatSecureContractRentArrearsWithAsbAsAsbPage() {
        assertThat(underTest.deriveSecureContractGroundsRouting(secure(
            Set.of(SecureContractDiscretionaryGroundsWales.RENT_ARREARS_S157,
                   SecureContractDiscretionaryGroundsWales.ANTISOCIAL_BEHAVIOUR_S157), Set.of(), Set.of())))
            .contains(new WalesGroundsRouting(YesOrNo.YES, YesOrNo.NO));
    }

    @Test
    void shouldNotRouteSecureContractWhenNothingSelected() {
        assertThat(underTest.deriveSecureContractGroundsRouting(secure(Set.of(), Set.of(), Set.of()))).isEmpty();
    }

    // ---- Wales ASB ----

    @Test
    void shouldDetectWalesAntisocialBehaviourUnderEitherContract() {
        GroundsForPossessionWales standardAsb = wales(
            Set.of(DiscretionaryGroundWales.ANTISOCIAL_BEHAVIOUR_S157), Set.of(), Set.of());
        SecureContractGroundsForPossessionWales secureAsb = secure(
            Set.of(SecureContractDiscretionaryGroundsWales.ANTISOCIAL_BEHAVIOUR_S157), Set.of(), Set.of());
        GroundsForPossessionWales standardOther = wales(
            Set.of(DiscretionaryGroundWales.RENT_ARREARS_S157), Set.of(), Set.of());

        assertThat(underTest.hasWalesAntisocialBehaviourGround(standardAsb, null)).isTrue();
        assertThat(underTest.hasWalesAntisocialBehaviourGround(null, secureAsb)).isTrue();
        assertThat(underTest.hasWalesAntisocialBehaviourGround(standardOther, secure(Set.of(), Set.of(), Set.of())))
            .isFalse();
        assertThat(underTest.hasWalesAntisocialBehaviourGround(null, null)).isFalse();
        assertThat(underTest.hasWalesAntisocialBehaviourGround(
            GroundsForPossessionWales.builder().build(), SecureContractGroundsForPossessionWales.builder().build()))
            .isFalse();
    }
}
