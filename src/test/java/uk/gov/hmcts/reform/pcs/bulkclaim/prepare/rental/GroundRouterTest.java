package uk.gov.hmcts.reform.pcs.bulkclaim.prepare.rental;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common.BulkErrorCollector;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.EstateManagementGroundCodeType;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.EstateManagementGroundType;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.GroundType;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.GroundsType;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.RentArrearsOrBreachType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsOrBreachOfTenancy;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredRentArrearsGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.IntroductoryDemotedOrOtherGrounds;
import uk.gov.hmcts.reform.pcs.ccd.service.rules.GroundsRules;

import static org.assertj.core.api.Assertions.assertThat;

class GroundRouterTest {

    private static final String PATH = "ClaimParticulars/Grounds";

    private final GroundRouter router = new GroundRouter(new GroundsRules());
    private final BulkErrorCollector errors = BulkErrorCollector.forClaimPath("/RentalClaimSet/RentalClaim[1]");
    private final PCSCase target = PCSCase.builder().build();

    private static GroundType ground(String code, String reason) {
        GroundType ground = new GroundType();
        ground.setCode(code);
        ground.setReason(reason);
        return ground;
    }

    private static GroundsType grounds(GroundType... grounds) {
        GroundsType type = new GroundsType();
        type.getGround().addAll(java.util.List.of(grounds));
        return type;
    }

    private void route(TenancyLicenceType tenancy, GroundsType grounds) {
        router.route(tenancy, grounds, target, PATH, errors);
    }

    @Test
    void assuredWithRentArrearsGroundsUsesArrearsContainers() {
        route(TenancyLicenceType.ASSURED_TENANCY, grounds(
            ground("SERIOUS_RENT_ARREARS_GROUND8", null),
            ground("PERSISTENT_DELAY_GROUND11", null),
            ground("HOLIDAY_LET_GROUND3", "Holiday let."),
            ground("BREACH_TENANCY_GROUND12", "Breach.")));

        assertThat(errors.hasErrors()).isFalse();
        assertThat(target.getClaimDueToRentArrears()).isEqualTo(YesOrNo.YES);
        assertThat(target.getAssuredRentArrearsPossessionGrounds().getRentArrearsGrounds())
            .containsExactlyInAnyOrder(AssuredRentArrearsGround.SERIOUS_RENT_ARREARS_GROUND8,
                                       AssuredRentArrearsGround.PERSISTENT_DELAY_GROUND11);
        assertThat(target.getRentArrearsGroundsReasons().getHolidayLetReason()).isEqualTo("Holiday let.");
        assertThat(target.getRentArrearsGroundsReasons().getBreachOfTenancyConditionsReason()).isEqualTo("Breach.");
        assertThat(target.getHasOtherAdditionalGrounds()).isEqualTo(YesOrNo.YES);
        assertThat(target.getNoRentArrearsGroundsOptions()).isNull();
    }

    @Test
    void assuredWithOnlyRentArrearsGroundsHasNoAdditionalGrounds() {
        route(TenancyLicenceType.ASSURED_TENANCY, grounds(ground("RENT_ARREARS_GROUND10", null)));

        assertThat(errors.hasErrors()).isFalse();
        assertThat(target.getHasOtherAdditionalGrounds()).isEqualTo(YesOrNo.NO);
    }

    @Test
    void assuredWithoutRentArrearsGroundsUsesNoArrearsContainers() {
        route(TenancyLicenceType.ASSURED_TENANCY, grounds(
            ground("DEATH_OF_TENANT_GROUND7", "Tenant died."),
            ground("OTHER", "Other reason.")));

        assertThat(errors.errors()).extracting(e -> e.path()).containsExactly(
            "/RentalClaimSet/RentalClaim[1]/ClaimParticulars/Grounds/Ground[2]/OtherDescription");
        assertThat(target.getClaimDueToRentArrears()).isEqualTo(YesOrNo.NO);
        assertThat(target.getNoRentArrearsGroundsOptions().getMandatoryGrounds()).hasSize(1);
        assertThat(target.getNoRentArrearsGroundsReasons().getDeathOfTenant()).isEqualTo("Tenant died.");
        assertThat(target.getNoRentArrearsGroundsOptions().getShowGroundReasonPage()).isEqualTo(YesOrNo.YES);
    }

    @Test
    void secureTenancyMapsAllGroundFamiliesAndS84aConditions() {
        GroundsType grounds = grounds(
            ground("RENT_ARREARS_OR_BREACH_OF_TENANCY", "Breach."),
            ground("DOMESTIC_VIOLENCE", "Violence."),
            ground("S84A_CONDITION_2", "Condition."),
            ground("OVERCROWDING", "Crowded."),
            ground("ADAPTED_ACCOMMODATION", "Adapted."));
        grounds.getRentArrearsOrBreach().add(RentArrearsOrBreachType.BREACH_OF_TENANCY);

        route(TenancyLicenceType.SECURE_TENANCY, grounds);

        assertThat(errors.hasErrors()).isFalse();
        var possession = target.getSecureOrFlexiblePossessionGrounds();
        assertThat(possession.getSecureOrFlexibleDiscretionaryGrounds()).hasSize(2);
        assertThat(possession.getSecureOrFlexibleMandatoryGrounds()).hasSize(1);
        assertThat(possession.getSecureAntisocialAdditionalGrounds()).hasSize(1);
        assertThat(possession.getSecureOrFlexibleMandatoryGroundsAlt()).hasSize(1);
        assertThat(possession.getSecureOrFlexibleDiscretionaryGroundsAlt()).hasSize(1);
        assertThat(target.getRentArrearsOrBreachOfTenancy())
            .containsExactly(RentArrearsOrBreachOfTenancy.BREACH_OF_TENANCY);
        assertThat(target.getShowBreachOfTenancyTextarea()).isEqualTo(YesOrNo.YES);
        assertThat(target.getSecureOrFlexibleGroundsReasons().getBreachOfTenancyGround()).isEqualTo("Breach.");
        assertThat(target.getSecureOrFlexibleGroundsReasons().getAntiSocialCondition2OfS84AGround())
            .isEqualTo("Condition.");
    }

    @Test
    void flexibleTenancyUsesTheSameContainersAsSecure() {
        route(TenancyLicenceType.FLEXIBLE_TENANCY, grounds(ground("RIOT_OFFENCE", "Riot.")));

        assertThat(errors.hasErrors()).isFalse();
        assertThat(target.getSecureOrFlexibleGroundsReasons().getRiotOffenceGround()).isEqualTo("Riot.");
    }

    @Test
    void secureRentArrearsGroundNeedsRentArrearsOrBreach() {
        route(TenancyLicenceType.SECURE_TENANCY, grounds(ground("RENT_ARREARS_OR_BREACH_OF_TENANCY", null)));

        assertThat(errors.errors()).extracting(e -> e.path())
            .containsExactly("/RentalClaimSet/RentalClaim[1]/ClaimParticulars/Grounds/RentArrearsOrBreach");
    }

    @Test
    void introductoryAndDemotedAndOtherUseIntroductoryContainers() {
        for (TenancyLicenceType tenancy : new TenancyLicenceType[] {
            TenancyLicenceType.INTRODUCTORY_TENANCY, TenancyLicenceType.DEMOTED_TENANCY, TenancyLicenceType.OTHER}) {
            PCSCase pcsCase = PCSCase.builder().build();
            BulkErrorCollector collector = BulkErrorCollector.forClaimPath("/c");
            GroundType other = ground("OTHER", "Why.");
            other.setOtherDescription("What.");

            router.route(tenancy, grounds(ground("RENT_ARREARS", null), ground("ANTI_SOCIAL", "Noise."), other),
                         pcsCase, PATH, collector);

            assertThat(collector.hasErrors()).isFalse();
            assertThat(pcsCase.getIntroductoryDemotedOrOtherGroundsForPossession()
                           .getIntroductoryDemotedOrOtherGrounds())
                .containsExactlyInAnyOrder(IntroductoryDemotedOrOtherGrounds.RENT_ARREARS,
                                           IntroductoryDemotedOrOtherGrounds.ANTI_SOCIAL,
                                           IntroductoryDemotedOrOtherGrounds.OTHER);
            assertThat(pcsCase.getIntroductoryDemotedOtherGroundReason().getOtherGround()).isEqualTo("Why.");
            assertThat(pcsCase.getShowIntroductoryDemotedOtherGroundReasonPage()).isEqualTo(YesOrNo.YES);
        }
    }

    @Test
    void noGroundsMustStandAlone() {
        route(TenancyLicenceType.DEMOTED_TENANCY, grounds(ground("NO_GROUNDS", "None."), ground("ANTI_SOCIAL", "x")));

        assertThat(errors.errors()).hasSize(1);
        route(TenancyLicenceType.DEMOTED_TENANCY, grounds(ground("NO_GROUNDS", "None.")));
        assertThat(target.getIntroductoryDemotedOrOtherGroundsForPossession()
                       .getHasIntroductoryDemotedOtherGroundsForPossession()).isEqualTo(VerticalYesNo.NO);
    }

    @Test
    void groundFromAnotherTenancyTypeIsRejected() {
        route(TenancyLicenceType.ASSURED_TENANCY, grounds(ground("NUISANCE_OR_IMMORAL_USE", "x")));

        assertThat(errors.errors()).extracting(e -> e.path())
            .contains("/RentalClaimSet/RentalClaim[1]/ClaimParticulars/Grounds/Ground[1]/Code");
    }

    @Test
    void missingReasonIsReportedAgainstTheGround() {
        route(TenancyLicenceType.INTRODUCTORY_TENANCY, grounds(ground("ABSOLUTE_GROUNDS", null)));

        assertThat(errors.errors()).extracting(e -> e.path())
            .containsExactly("/RentalClaimSet/RentalClaim[1]/ClaimParticulars/Grounds/Ground[1]/Reason");
    }

    @Test
    void reasonLongerThanTheUiLimitIsRejected() {
        route(TenancyLicenceType.INTRODUCTORY_TENANCY, grounds(ground("ABSOLUTE_GROUNDS", "x".repeat(501))));

        assertThat(errors.errors()).extracting(e -> e.code()).containsExactly(BulkErrorCollector.TOO_LONG);
    }

    @Test
    void duplicateGroundsAndEstateManagementGroundsAreRejected() {
        GroundType estate = ground("RIOT_OFFENCE", "Riot.");
        EstateManagementGroundType estateGround = new EstateManagementGroundType();
        estateGround.setCode(EstateManagementGroundCodeType.BUILDING_WORKS);
        estateGround.setReason("Works.");
        estate.getEstateManagementGround().add(estateGround);

        route(TenancyLicenceType.SECURE_TENANCY, grounds(estate, ground("RIOT_OFFENCE", "Riot.")));

        assertThat(errors.errors()).hasSize(2);
    }

    @Test
    void rentArrearsOrBreachIsNotAllowedForAssured() {
        GroundsType grounds = grounds(ground("OWNER_OCCUPIER_GROUND1", "Home."));
        grounds.getRentArrearsOrBreach().add(RentArrearsOrBreachType.RENT_ARREARS);

        route(TenancyLicenceType.ASSURED_TENANCY, grounds);

        assertThat(errors.errors()).extracting(e -> e.code()).containsExactly(BulkErrorCollector.NOT_ALLOWED);
    }
}
