package uk.gov.hmcts.reform.pcs.ccd.service.ground;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.DiscretionaryGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.EstateManagementGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.GroundsForPossessionWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.GroundsReasonsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.MandatoryGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundCategory;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundEntity;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class WalesStandardClaimGroundServiceTest {

    private WalesStandardClaimGroundService underTest;

    @BeforeEach
    void setUp() {
        underTest = new WalesStandardClaimGroundService();
    }

    @Test
    void shouldCreateClaimGroundEntities() {
        // Given
        Set<MandatoryGroundWales> mandatoryGrounds
            = EnumSet.allOf(MandatoryGroundWales.class);

        Set<DiscretionaryGroundWales> discretionaryGrounds
            = EnumSet.allOf(DiscretionaryGroundWales.class);

        Set<EstateManagementGroundsWales> estateGrounds
            = EnumSet.allOf(EstateManagementGroundsWales.class);

        Answer<String> defaultReasonAnswer = invocation -> "Reason from " + invocation.getMethod().getName();
        GroundsReasonsWales reasons = mock(GroundsReasonsWales.class, defaultReasonAnswer);

        PCSCase caseData = PCSCase.builder()
            .legislativeCountry(LegislativeCountry.WALES)
            .groundsForPossessionWales(
                GroundsForPossessionWales.builder()
                    .mandatoryGrounds(mandatoryGrounds)
                    .discretionaryGrounds(discretionaryGrounds)
                    .estateManagementGrounds(estateGrounds)
                    .build()
            )
            .groundsReasonsWales(reasons)
            .build();

        // When
        List<ClaimGroundEntity> claimGroundEntities = underTest.createClaimGroundEntities(caseData);

        // Then
        assertThat(claimGroundEntities)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.WALES_STANDARD_OTHER_MANDATORY)
                    .code("FAILURE_TO_GIVE_UP_POSSESSION_S170")
                    .reason("Reason from getFailToGiveUpS170Reason")
                    .isRentArrears(false)
                    .build(),
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.WALES_STANDARD_OTHER_MANDATORY)
                    .code("LANDLORD_NOTICE_PERIODIC_S178")
                    .reason("Reason from getLandlordNoticePeriodicS178Reason")
                    .isRentArrears(false)
                    .build(),
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.WALES_STANDARD_OTHER_MANDATORY)
                    .code("SERIOUS_ARREARS_PERIODIC_S181")
                    .reason("Reason from getSeriousArrearsPeriodicS181Reason")
                    .isRentArrears(true)
                    .build(),
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.WALES_STANDARD_OTHER_MANDATORY)
                    .code("LANDLORD_NOTICE_FT_END_S186")
                    .reason("Reason from getLandlordNoticeFtEndS186Reason")
                    .isRentArrears(false)
                    .build(),
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.WALES_STANDARD_OTHER_MANDATORY)
                    .code("SERIOUS_ARREARS_FIXED_TERM_S187")
                    .reason("Reason from getSeriousArrearsFixedTermS187Reason")
                    .isRentArrears(true)
                    .build(),
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.WALES_STANDARD_OTHER_MANDATORY)
                    .code("FAIL_TO_GIVE_UP_BREAK_NOTICE_S191")
                    .reason("Reason from getFailToGiveUpBreakNoticeS191Reason")
                    .isRentArrears(false)
                    .build(),
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.WALES_STANDARD_OTHER_MANDATORY)
                    .code("LANDLORD_BREAK_CLAUSE_S199")
                    .reason("Reason from getLandlordBreakClauseS199Reason")
                    .isRentArrears(false)
                    .build(),
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.WALES_STANDARD_OTHER_MANDATORY)
                    .code("CONVERTED_FIXED_TERM_SCH12_25B2")
                    .reason("Reason from getConvertedFixedTermSch1225B2Reason")
                    .isRentArrears(false)
                    .build(),

                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.WALES_STANDARD_OTHER_DISCRETIONARY)
                    .code("RENT_ARREARS_S157")
                    .reason(null)
                    .isRentArrears(true)
                    .build(),
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.WALES_STANDARD_OTHER_DISCRETIONARY)
                    .code("ANTISOCIAL_BEHAVIOUR_S157")
                    .reason(null)
                    .isRentArrears(false)
                    .build(),
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.WALES_STANDARD_OTHER_DISCRETIONARY)
                    .code("OTHER_BREACH_OF_CONTRACT_S157")
                    .reason("Reason from getOtherBreachSection157Reason")
                    .isRentArrears(false)
                    .build(),
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.WALES_STANDARD_OTHER_DISCRETIONARY)
                    .code("ESTATE_MANAGEMENT_GROUNDS_S160")
                    .reason(null)
                    .isRentArrears(false)
                    .build(),

                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.WALES_STANDARD_OTHER_ESTATE_MANAGEMENT)
                    .code("BUILDING_WORKS")
                    .reason("Reason from getBuildingWorksReason")
                    .isRentArrears(false)
                    .build(),
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.WALES_STANDARD_OTHER_ESTATE_MANAGEMENT)
                    .code("REDEVELOPMENT_SCHEMES")
                    .reason("Reason from getRedevelopmentSchemesReason")
                    .isRentArrears(false)
                    .build(),
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.WALES_STANDARD_OTHER_ESTATE_MANAGEMENT)
                    .code("CHARITIES")
                    .reason("Reason from getCharitiesReason")
                    .isRentArrears(false)
                    .build(),
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.WALES_STANDARD_OTHER_ESTATE_MANAGEMENT)
                    .code("DISABLED_SUITABLE_DWELLING")
                    .reason("Reason from getDisabledSuitableDwellingReason")
                    .isRentArrears(false)
                    .build(),
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.WALES_STANDARD_OTHER_ESTATE_MANAGEMENT)
                    .code("HOUSING_ASSOCIATIONS_AND_TRUSTS")
                    .reason("Reason from getHousingAssociationsAndTrustsReason")
                    .isRentArrears(false)
                    .build(),
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.WALES_STANDARD_OTHER_ESTATE_MANAGEMENT)
                    .code("SPECIAL_NEEDS_DWELLINGS")
                    .reason("Reason from getSpecialNeedsDwellingsReason")
                    .isRentArrears(false)
                    .build(),
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.WALES_STANDARD_OTHER_ESTATE_MANAGEMENT)
                    .code("RESERVE_SUCCESSORS")
                    .reason("Reason from getReserveSuccessorsReason")
                    .isRentArrears(false)
                    .build(),
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.WALES_STANDARD_OTHER_ESTATE_MANAGEMENT)
                    .code("JOINT_CONTRACT_HOLDERS")
                    .reason("Reason from getJointContractHoldersReason")
                    .isRentArrears(false)
                    .build(),
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.WALES_STANDARD_OTHER_ESTATE_MANAGEMENT)
                    .code("OTHER_ESTATE_MANAGEMENT_REASONS")
                    .reason("Reason from getOtherEstateManagementReasonsReason")
                    .isRentArrears(false)
                    .build()
            );
    }

    @Test
    void shouldNotIncludeEstateManagementGroundsIfTopLevelNotSelected() {
        // Given
        Set<DiscretionaryGroundWales> discretionaryGrounds
            = Set.of(DiscretionaryGroundWales.RENT_ARREARS_S157,
                     DiscretionaryGroundWales.ANTISOCIAL_BEHAVIOUR_S157,
                     DiscretionaryGroundWales.OTHER_BREACH_OF_CONTRACT_S157);

        Set<EstateManagementGroundsWales> estateGroundsShouldBeIgnored
            = EnumSet.allOf(EstateManagementGroundsWales.class);

        PCSCase caseData = PCSCase.builder()
            .legislativeCountry(LegislativeCountry.WALES)
            .groundsForPossessionWales(
                GroundsForPossessionWales.builder()
                    .mandatoryGrounds(Set.of())
                    .discretionaryGrounds(discretionaryGrounds)
                    .estateManagementGrounds(estateGroundsShouldBeIgnored)
                    .build()
            )
            .groundsReasonsWales(mock(GroundsReasonsWales.class))
            .build();

        // When
        List<ClaimGroundEntity> claimGroundEntities = underTest.createClaimGroundEntities(caseData);

        // Then
        assertThat(claimGroundEntities)
            .extracting(ClaimGroundEntity::getCode)
            .containsExactlyInAnyOrder(
                "RENT_ARREARS_S157",
                "ANTISOCIAL_BEHAVIOUR_S157",
                "OTHER_BREACH_OF_CONTRACT_S157"
            );
    }

}
