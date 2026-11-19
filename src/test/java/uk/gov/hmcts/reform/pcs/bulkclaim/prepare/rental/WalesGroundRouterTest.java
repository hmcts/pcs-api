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
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.DiscretionaryGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.EstateManagementGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.MandatoryGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceTypeWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractDiscretionaryGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractMandatoryGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.service.rules.GroundsRules;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WalesGroundRouterTest {

    private static final String PATH = "ClaimParticulars/Grounds";
    private static final String ABSOLUTE = "/RentalClaimSet/RentalClaim[1]/ClaimParticulars/Grounds";

    private final WalesGroundRouter router = new WalesGroundRouter(new GroundsRules());
    private final BulkErrorCollector errors = BulkErrorCollector.forClaimPath("/RentalClaimSet/RentalClaim[1]");
    private final PCSCase target = PCSCase.builder().build();

    private static GroundType ground(String code, String reason, EstateManagementGroundType... estateGrounds) {
        GroundType ground = new GroundType();
        ground.setCode(code);
        ground.setReason(reason);
        ground.getEstateManagementGround().addAll(List.of(estateGrounds));
        return ground;
    }

    private static EstateManagementGroundType estate(EstateManagementGroundCodeType code, String reason) {
        EstateManagementGroundType estateGround = new EstateManagementGroundType();
        estateGround.setCode(code);
        estateGround.setReason(reason);
        return estateGround;
    }

    private static EstateManagementGroundType[] allEstateGrounds(String reasonPrefix) {
        return Arrays.stream(EstateManagementGroundCodeType.values())
            .map(code -> estate(code, reasonPrefix + code.value()))
            .toArray(EstateManagementGroundType[]::new);
    }

    private static GroundsType grounds(GroundType... grounds) {
        GroundsType type = new GroundsType();
        type.getGround().addAll(List.of(grounds));
        return type;
    }

    private void route(OccupationLicenceTypeWales contract, GroundsType grounds) {
        router.route(contract, grounds, target, PATH, errors);
    }

    private List<String> errorPaths() {
        return errors.errors().stream().map(error -> error.path()).toList();
    }

    @Test
    void standardRentArrearsOnlyShowsNeitherAsbNorReasonsPage() {
        route(OccupationLicenceTypeWales.STANDARD_CONTRACT, grounds(
            ground("RENT_ARREARS_S157", null), ground("SERIOUS_ARREARS_PERIODIC_S181", null)));

        assertThat(errors.hasErrors()).isFalse();
        assertThat(target.getGroundsForPossessionWales().getDiscretionaryGrounds())
            .containsExactly(DiscretionaryGroundWales.RENT_ARREARS_S157);
        assertThat(target.getGroundsForPossessionWales().getMandatoryGrounds())
            .containsExactly(MandatoryGroundWales.SERIOUS_ARREARS_PERIODIC_S181);
        assertThat(target.getShowASBQuestionsPageWales()).isEqualTo(YesOrNo.NO);
        assertThat(target.getShowReasonsForGroundsPageWales()).isEqualTo(YesOrNo.NO);
        assertThat(target.getSecureContractGroundsForPossessionWales()).isNull();
    }

    @Test
    void fixedTermSeriousArrearsAlsoNeedsNoReason() {
        route(OccupationLicenceTypeWales.OTHER, grounds(ground("SERIOUS_ARREARS_FIXED_TERM_S187", null)));

        assertThat(errors.hasErrors()).isFalse();
        assertThat(target.getShowReasonsForGroundsPageWales()).isEqualTo(YesOrNo.NO);
        assertThat(target.getGroundsReasonsWales().getSeriousArrearsFixedTermS187Reason()).isNull();
    }

    @Test
    void standardAntisocialBehaviourShowsAsbQuestionsOnly() {
        route(OccupationLicenceTypeWales.STANDARD_CONTRACT, grounds(ground("ANTISOCIAL_BEHAVIOUR_S157", null)));

        assertThat(errors.hasErrors()).isFalse();
        assertThat(target.getShowASBQuestionsPageWales()).isEqualTo(YesOrNo.YES);
        assertThat(target.getShowReasonsForGroundsPageWales()).isEqualTo(YesOrNo.NO);
    }

    @Test
    void standardAntisocialBehaviourWithOtherBreachShowsBothPages() {
        route(OccupationLicenceTypeWales.STANDARD_CONTRACT, grounds(
            ground("ANTISOCIAL_BEHAVIOUR_S157", null), ground("OTHER_BREACH_OF_CONTRACT_S157", "Sub-letting.")));

        assertThat(errors.hasErrors()).isFalse();
        assertThat(target.getShowASBQuestionsPageWales()).isEqualTo(YesOrNo.YES);
        assertThat(target.getShowReasonsForGroundsPageWales()).isEqualTo(YesOrNo.YES);
        assertThat(target.getGroundsReasonsWales().getOtherBreachSection157Reason()).isEqualTo("Sub-letting.");
    }

    @Test
    void standardMandatoryGroundsMapTheirReasons() {
        route(OccupationLicenceTypeWales.STANDARD_CONTRACT, grounds(
            ground("FAILURE_TO_GIVE_UP_POSSESSION_S170", "r170"),
            ground("LANDLORD_NOTICE_PERIODIC_S178", "r178"),
            ground("LANDLORD_NOTICE_FT_END_S186", "r186"),
            ground("FAIL_TO_GIVE_UP_BREAK_NOTICE_S191", "r191"),
            ground("LANDLORD_BREAK_CLAUSE_S199", "r199"),
            ground("CONVERTED_FIXED_TERM_SCH12_25B2", "r25b")));

        assertThat(errors.hasErrors()).isFalse();
        assertThat(target.getShowReasonsForGroundsPageWales()).isEqualTo(YesOrNo.YES);
        assertThat(target.getGroundsReasonsWales()).satisfies(reasons -> {
            assertThat(reasons.getFailToGiveUpS170Reason()).isEqualTo("r170");
            assertThat(reasons.getLandlordNoticePeriodicS178Reason()).isEqualTo("r178");
            assertThat(reasons.getLandlordNoticeFtEndS186Reason()).isEqualTo("r186");
            assertThat(reasons.getFailToGiveUpBreakNoticeS191Reason()).isEqualTo("r191");
            assertThat(reasons.getLandlordBreakClauseS199Reason()).isEqualTo("r199");
            assertThat(reasons.getConvertedFixedTermSch1225B2Reason()).isEqualTo("r25b");
        });
    }

    @Test
    void standardEstateManagementRecordsEachReasonAgainstItsEstateGround() {
        route(OccupationLicenceTypeWales.STANDARD_CONTRACT, grounds(
            ground("ESTATE_MANAGEMENT_GROUNDS_S160", null, allEstateGrounds("Reason for "))));

        assertThat(errors.hasErrors()).isFalse();
        assertThat(target.getGroundsForPossessionWales().getEstateManagementGrounds())
            .containsExactlyInAnyOrder(EstateManagementGroundsWales.values());
        assertThat(target.getGroundsReasonsWales()).satisfies(reasons -> {
            assertThat(reasons.getBuildingWorksReason()).isEqualTo("Reason for BUILDING_WORKS");
            assertThat(reasons.getRedevelopmentSchemesReason()).isEqualTo("Reason for REDEVELOPMENT_SCHEMES");
            assertThat(reasons.getCharitiesReason()).isEqualTo("Reason for CHARITIES");
            assertThat(reasons.getDisabledSuitableDwellingReason()).isEqualTo("Reason for DISABLED_SUITABLE_DWELLING");
            assertThat(reasons.getHousingAssociationsAndTrustsReason())
                .isEqualTo("Reason for HOUSING_ASSOCIATIONS_AND_TRUSTS");
            assertThat(reasons.getSpecialNeedsDwellingsReason()).isEqualTo("Reason for SPECIAL_NEEDS_DWELLINGS");
            assertThat(reasons.getReserveSuccessorsReason()).isEqualTo("Reason for RESERVE_SUCCESSORS");
            assertThat(reasons.getJointContractHoldersReason()).isEqualTo("Reason for JOINT_CONTRACT_HOLDERS");
            assertThat(reasons.getOtherEstateManagementReasonsReason())
                .isEqualTo("Reason for OTHER_ESTATE_MANAGEMENT_REASONS");
        });
        assertThat(target.getShowReasonsForGroundsPageWales()).isEqualTo(YesOrNo.YES);
    }

    @Test
    void estateManagementWithoutEstateGroundsIsRejected() {
        route(OccupationLicenceTypeWales.STANDARD_CONTRACT, grounds(
            ground("ESTATE_MANAGEMENT_GROUNDS_S160", null)));

        assertThat(errors.errors()).extracting(error -> error.message())
            .containsExactly(GroundsRules.WALES_ESTATE_MANAGEMENT_ERROR);
        assertThat(errorPaths()).containsExactly(ABSOLUTE);
    }

    @Test
    void estateManagementGroundsMustNotRepeatACode() {
        route(OccupationLicenceTypeWales.STANDARD_CONTRACT, grounds(
            ground("ESTATE_MANAGEMENT_GROUNDS_S160", null,
                   estate(EstateManagementGroundCodeType.CHARITIES, "First."),
                   estate(EstateManagementGroundCodeType.CHARITIES, "Second."))));

        assertThat(errorPaths()).containsExactly(ABSOLUTE + "/Ground[1]/EstateManagementGround[2]/Code");
        assertThat(target.getGroundsReasonsWales().getCharitiesReason()).isEqualTo("First.");
    }

    @Test
    void estateManagementGroundsRequireTheirOwnReason() {
        route(OccupationLicenceTypeWales.STANDARD_CONTRACT, grounds(
            ground("ESTATE_MANAGEMENT_GROUNDS_S160", null,
                   estate(EstateManagementGroundCodeType.CHARITIES, null),
                   estate(EstateManagementGroundCodeType.BUILDING_WORKS, "x".repeat(501)))));

        assertThat(errors.errors()).extracting(error -> error.path() + ":" + error.code()).containsExactly(
            ABSOLUTE + "/Ground[1]/EstateManagementGround[1]/Reason:REQUIRED",
            ABSOLUTE + "/Ground[1]/EstateManagementGround[2]/Reason:TOO_LONG");
    }

    @Test
    void estateManagementGroundsDoNotTakeAReasonOnTheParentGround() {
        route(OccupationLicenceTypeWales.STANDARD_CONTRACT, grounds(
            ground("ESTATE_MANAGEMENT_GROUNDS_S160", "Parent reason.",
                   estate(EstateManagementGroundCodeType.CHARITIES, "Charity."))));

        assertThat(errorPaths()).containsExactly(ABSOLUTE + "/Ground[1]/Reason");
    }

    @Test
    void estateManagementReasonOnAnotherGroundIsRejected() {
        route(OccupationLicenceTypeWales.STANDARD_CONTRACT, grounds(
            ground("ANTISOCIAL_BEHAVIOUR_S157", null, estate(EstateManagementGroundCodeType.CHARITIES, "Charity.")),
            ground("LANDLORD_NOTICE_PERIODIC_S178", "Reason.",
                   estate(EstateManagementGroundCodeType.CHARITIES, "Charity."))));

        assertThat(errorPaths()).containsExactly(
            ABSOLUTE + "/Ground[1]/EstateManagementGround", ABSOLUTE + "/Ground[2]/EstateManagementGround");
    }

    @Test
    void groundsThatNeedAReasonRequireOneWithinTheLimit() {
        route(OccupationLicenceTypeWales.STANDARD_CONTRACT, grounds(
            ground("OTHER_BREACH_OF_CONTRACT_S157", null),
            ground("LANDLORD_NOTICE_PERIODIC_S178", "x".repeat(501)),
            ground("ESTATE_MANAGEMENT_GROUNDS_S160", null,
                   estate(EstateManagementGroundCodeType.CHARITIES, "Charity."))));

        assertThat(errors.errors()).extracting(error -> error.path() + ":" + error.code()).containsExactly(
            ABSOLUTE + "/Ground[1]/Reason:REQUIRED",
            ABSOLUTE + "/Ground[2]/Reason:TOO_LONG");
    }

    @Test
    void standardRejectsSecureContractGroundsAndDuplicates() {
        route(OccupationLicenceTypeWales.STANDARD_CONTRACT, grounds(
            ground("LANDLORD_NOTICE_S199", "Reason."),
            ground("RENT_ARREARS_S157", null),
            ground("RENT_ARREARS_S157", null)));

        assertThat(errorPaths()).containsExactly(ABSOLUTE + "/Ground[3]/Code", ABSOLUTE + "/Ground[1]/Code");
    }

    @Test
    void noGroundsAtAllIsRejected() {
        route(OccupationLicenceTypeWales.STANDARD_CONTRACT, grounds(ground("NO_GROUNDS", "Reason.")));

        assertThat(errorPaths()).containsExactly(ABSOLUTE + "/Ground[1]/Code", ABSOLUTE);
        assertThat(target.getShowReasonsForGroundsPageWales()).isEqualTo(YesOrNo.NO);
    }

    @Test
    void rentArrearsOrBreachIsRejectedForWales() {
        GroundsType grounds = grounds(ground("RENT_ARREARS_S157", null));
        grounds.getRentArrearsOrBreach().add(RentArrearsOrBreachType.RENT_ARREARS);

        route(OccupationLicenceTypeWales.STANDARD_CONTRACT, grounds);

        assertThat(errorPaths()).containsExactly(ABSOLUTE + "/RentArrearsOrBreach");
    }

    @Test
    void secureContractAntisocialBehaviourShowsAsbQuestions() {
        route(OccupationLicenceTypeWales.SECURE_CONTRACT, grounds(ground("ANTISOCIAL_BEHAVIOUR_S157", null)));

        assertThat(errors.hasErrors()).isFalse();
        assertThat(target.getSecureContractGroundsForPossessionWales().getDiscretionaryGrounds())
            .containsExactly(SecureContractDiscretionaryGroundsWales.ANTISOCIAL_BEHAVIOUR_S157);
        assertThat(target.getGroundsForPossessionWales()).isNull();
        assertThat(target.getShowASBQuestionsPageWales()).isEqualTo(YesOrNo.YES);
        assertThat(target.getShowReasonsForGroundsPageWales()).isEqualTo(YesOrNo.NO);
    }

    @Test
    void secureContractMapsMandatoryOtherBreachAndEstateReasons() {
        route(OccupationLicenceTypeWales.SECURE_CONTRACT, grounds(
            ground("FAILURE_TO_GIVE_UP_POSSESSION_S170", "r170"),
            ground("LANDLORD_NOTICE_S186", "r186"),
            ground("FAILURE_TO_GIVE_UP_POSSESSION_S191", "r191"),
            ground("LANDLORD_NOTICE_S199", "r199"),
            ground("OTHER_BREACH_OF_CONTRACT_S157", "breach"),
            ground("ESTATE_MANAGEMENT_GROUNDS_S160", null, allEstateGrounds("Reason for "))));

        assertThat(errors.hasErrors()).isFalse();
        assertThat(target.getSecureContractGroundsForPossessionWales().getMandatoryGrounds())
            .containsExactlyInAnyOrder(SecureContractMandatoryGroundsWales.values());
        assertThat(target.getShowReasonsForGroundsPageWales()).isEqualTo(YesOrNo.YES);
        assertThat(target.getGroundsReasonsWales()).satisfies(reasons -> {
            assertThat(reasons.getSecureFailureToGiveUpPossessionSection170Reason()).isEqualTo("r170");
            assertThat(reasons.getSecureLandlordNoticeSection186Reason()).isEqualTo("r186");
            assertThat(reasons.getSecureFailureToGiveUpPossessionSection191Reason()).isEqualTo("r191");
            assertThat(reasons.getSecureLandlordNoticeSection199Reason()).isEqualTo("r199");
            assertThat(reasons.getSecureOtherBreachOfContractReason()).isEqualTo("breach");
            assertThat(reasons.getSecureBuildingWorksReason()).isEqualTo("Reason for BUILDING_WORKS");
            assertThat(reasons.getSecureRedevelopmentSchemesReason()).isEqualTo("Reason for REDEVELOPMENT_SCHEMES");
            assertThat(reasons.getSecureCharitiesReason()).isEqualTo("Reason for CHARITIES");
            assertThat(reasons.getSecureDisabledSuitableDwellingReason())
                .isEqualTo("Reason for DISABLED_SUITABLE_DWELLING");
            assertThat(reasons.getSecureHousingAssociationsAndTrustsReason())
                .isEqualTo("Reason for HOUSING_ASSOCIATIONS_AND_TRUSTS");
            assertThat(reasons.getSecureSpecialNeedsDwellingsReason()).isEqualTo("Reason for SPECIAL_NEEDS_DWELLINGS");
            assertThat(reasons.getSecureReserveSuccessorsReason()).isEqualTo("Reason for RESERVE_SUCCESSORS");
            assertThat(reasons.getSecureJointContractHoldersReason()).isEqualTo("Reason for JOINT_CONTRACT_HOLDERS");
            assertThat(reasons.getSecureOtherEstateManagementReasonsReason())
                .isEqualTo("Reason for OTHER_ESTATE_MANAGEMENT_REASONS");
        });
    }

    @Test
    void secureContractRentArrearsOnlyShowsNoExtraPages() {
        route(OccupationLicenceTypeWales.SECURE_CONTRACT, grounds(ground("RENT_ARREARS_S157", null)));

        assertThat(errors.hasErrors()).isFalse();
        assertThat(target.getShowASBQuestionsPageWales()).isEqualTo(YesOrNo.NO);
        assertThat(target.getShowReasonsForGroundsPageWales()).isEqualTo(YesOrNo.NO);
    }

    @Test
    void secureContractRejectsStandardOnlyGroundsAndMissingEstateGrounds() {
        route(OccupationLicenceTypeWales.SECURE_CONTRACT, grounds(
            ground("LANDLORD_NOTICE_PERIODIC_S178", "Reason."),
            ground("ESTATE_MANAGEMENT_GROUNDS_S160", null)));

        assertThat(errorPaths()).containsExactly(ABSOLUTE + "/Ground[1]/Code", ABSOLUTE);
        assertThat(errors.errors().get(1).message()).isEqualTo(GroundsRules.WALES_ESTATE_MANAGEMENT_ERROR);
    }

    @Test
    void secureContractWithNoGroundsIsRejected() {
        route(OccupationLicenceTypeWales.SECURE_CONTRACT, grounds(ground("RENT_ARREARS_OR_BREACH", null)));

        assertThat(errorPaths()).containsExactly(ABSOLUTE + "/Ground[1]/Code", ABSOLUTE);
        assertThat(errors.errors().get(1).message()).isEqualTo(GroundsRules.AT_LEAST_ONE_GROUND_ERROR);
    }

    @Test
    void secureContractEstateReasonOnAnotherGroundIsRejected() {
        route(OccupationLicenceTypeWales.SECURE_CONTRACT, grounds(
            ground("LANDLORD_NOTICE_S186", "Reason.", estate(EstateManagementGroundCodeType.CHARITIES, "Charity.")),
            ground("RENT_ARREARS_S157", null, estate(EstateManagementGroundCodeType.CHARITIES, "Charity."))));

        assertThat(errorPaths()).containsExactly(
            ABSOLUTE + "/Ground[1]/EstateManagementGround", ABSOLUTE + "/Ground[2]/EstateManagementGround");
    }
}
