package uk.gov.hmcts.reform.pcs.ccd.service.ground;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class WalesStandardClaimGroundServiceTest {

    private WalesStandardClaimGroundService underTest;

    @BeforeEach
    void setUp() {
        underTest = new WalesStandardClaimGroundService();
    }

    @ParameterizedTest
    @MethodSource(value = "mandatoryGroundScenarios")
    void shouldCreateMandatoryClaimGroundEntities(MandatoryGroundWales ground,
                                                  ClaimGroundEntity expectedClaimGroundEntity) {

        // Given
        Set<MandatoryGroundWales> mandatoryGrounds = Set.of(ground);
        GroundsReasonsWales reasons = createMockReasons();

        PCSCase caseData = PCSCase.builder()
            .legislativeCountry(LegislativeCountry.WALES)
            .groundsForPossessionWales(
                GroundsForPossessionWales.builder()
                    .mandatoryGrounds(mandatoryGrounds)
                    .discretionaryGrounds(Set.of())
                    .estateManagementGrounds(Set.of())
                    .build()
            )
            .groundsReasonsWales(reasons)
            .build();

        // When
        List<ClaimGroundEntity> claimGroundEntities = underTest.createClaimGroundEntities(caseData);

        // Then
        assertThat(claimGroundEntities)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactly(expectedClaimGroundEntity);
    }

    @ParameterizedTest
    @MethodSource(value = "discretionaryGroundScenarios")
    void shouldCreateDiscretionaryClaimGroundEntities(DiscretionaryGroundWales ground,
                                                      ClaimGroundEntity expectedClaimGroundEntity) {

        Set<DiscretionaryGroundWales> discretionaryGrounds = Set.of(ground);
        GroundsReasonsWales reasons = createMockReasons();

        PCSCase caseData = PCSCase.builder()
            .legislativeCountry(LegislativeCountry.WALES)
            .groundsForPossessionWales(
                GroundsForPossessionWales.builder()
                    .mandatoryGrounds(Set.of())
                    .discretionaryGrounds(discretionaryGrounds)
                    .estateManagementGrounds(Set.of())
                    .build()
            )
            .groundsReasonsWales(reasons)
            .build();

        // When
        List<ClaimGroundEntity> claimGroundEntities = underTest.createClaimGroundEntities(caseData);

        // Then
        assertThat(claimGroundEntities)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactly(expectedClaimGroundEntity);
    }

    @ParameterizedTest
    @MethodSource(value = "estateManagementGroundScenarios")
    void shouldCreateEstateManagementClaimGroundEntities(EstateManagementGroundsWales ground,
                                                         ClaimGroundEntity expectedClaimGroundEntity) {

        Set<DiscretionaryGroundWales> discretionaryGrounds
            = Set.of(DiscretionaryGroundWales.ESTATE_MANAGEMENT_GROUNDS_S160);
        Set<EstateManagementGroundsWales> estateManagementGrounds = Set.of(ground);
        GroundsReasonsWales reasons = createMockReasons();

        PCSCase caseData = PCSCase.builder()
            .legislativeCountry(LegislativeCountry.WALES)
            .groundsForPossessionWales(
                GroundsForPossessionWales.builder()
                    .mandatoryGrounds(Set.of())
                    .discretionaryGrounds(discretionaryGrounds)
                    .estateManagementGrounds(estateManagementGrounds)
                    .build()
            )
            .groundsReasonsWales(reasons)
            .build();

        ClaimGroundEntity topLevelEstateManagementGround = ClaimGroundEntity.builder()
            .category(ClaimGroundCategory.WALES_STANDARD_OTHER_DISCRETIONARY)
            .code("ESTATE_MANAGEMENT_GROUNDS_S160")
            .reason(null)
            .isRentArrears(false)
            .build();

        // When
        List<ClaimGroundEntity> claimGroundEntities = underTest.createClaimGroundEntities(caseData);

        // Then
        assertThat(claimGroundEntities)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactly(topLevelEstateManagementGround, expectedClaimGroundEntity);
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

    private GroundsReasonsWales createMockReasons() {
        Answer<String> defaultReasonAnswer = invocation -> "Reason from " + invocation.getMethod().getName();
        return mock(GroundsReasonsWales.class, defaultReasonAnswer);
    }

    private static Stream<Arguments> mandatoryGroundScenarios() {
        return Stream.of(
            Arguments.arguments(MandatoryGroundWales.FAILURE_TO_GIVE_UP_POSSESSION_S170,
                                ClaimGroundEntity.builder()
                                    .category(ClaimGroundCategory.WALES_STANDARD_OTHER_MANDATORY)
                                    .code("FAILURE_TO_GIVE_UP_POSSESSION_S170")
                                    .reason("Reason from getFailToGiveUpS170Reason")
                                    .isRentArrears(false)
                                    .build()),
            Arguments.arguments(MandatoryGroundWales.LANDLORD_NOTICE_PERIODIC_S178,
                                ClaimGroundEntity.builder()
                                    .category(ClaimGroundCategory.WALES_STANDARD_OTHER_MANDATORY)
                                    .code("LANDLORD_NOTICE_PERIODIC_S178")
                                    .reason("Reason from getLandlordNoticePeriodicS178Reason")
                                    .isRentArrears(false)
                                    .build()),
            Arguments.arguments(MandatoryGroundWales.SERIOUS_ARREARS_PERIODIC_S181,
                                ClaimGroundEntity.builder()
                                    .category(ClaimGroundCategory.WALES_STANDARD_OTHER_MANDATORY)
                                    .code("SERIOUS_ARREARS_PERIODIC_S181")
                                    .reason("Reason from getSeriousArrearsPeriodicS181Reason")
                                    .isRentArrears(true)
                                    .build()),
            Arguments.arguments(MandatoryGroundWales.LANDLORD_NOTICE_FT_END_S186,
                                ClaimGroundEntity.builder()
                                    .category(ClaimGroundCategory.WALES_STANDARD_OTHER_MANDATORY)
                                    .code("LANDLORD_NOTICE_FT_END_S186")
                                    .reason("Reason from getLandlordNoticeFtEndS186Reason")
                                    .isRentArrears(false)
                                    .build()),
            Arguments.arguments(MandatoryGroundWales.SERIOUS_ARREARS_FIXED_TERM_S187,
                                ClaimGroundEntity.builder()
                                    .category(ClaimGroundCategory.WALES_STANDARD_OTHER_MANDATORY)
                                    .code("SERIOUS_ARREARS_FIXED_TERM_S187")
                                    .reason("Reason from getSeriousArrearsFixedTermS187Reason")
                                    .isRentArrears(true)
                                    .build()),
            Arguments.arguments(MandatoryGroundWales.FAIL_TO_GIVE_UP_BREAK_NOTICE_S191,
                                ClaimGroundEntity.builder()
                                    .category(ClaimGroundCategory.WALES_STANDARD_OTHER_MANDATORY)
                                    .code("FAIL_TO_GIVE_UP_BREAK_NOTICE_S191")
                                    .reason("Reason from getFailToGiveUpBreakNoticeS191Reason")
                                    .isRentArrears(false)
                                    .build()),
            Arguments.arguments(MandatoryGroundWales.LANDLORD_BREAK_CLAUSE_S199,
                                ClaimGroundEntity.builder()
                                    .category(ClaimGroundCategory.WALES_STANDARD_OTHER_MANDATORY)
                                    .code("LANDLORD_BREAK_CLAUSE_S199")
                                    .reason("Reason from getLandlordBreakClauseS199Reason")
                                    .isRentArrears(false)
                                    .build()),
            Arguments.arguments(MandatoryGroundWales.CONVERTED_FIXED_TERM_SCH12_25B2,
                                ClaimGroundEntity.builder()
                                    .category(ClaimGroundCategory.WALES_STANDARD_OTHER_MANDATORY)
                                    .code("CONVERTED_FIXED_TERM_SCH12_25B2")
                                    .reason("Reason from getConvertedFixedTermSch1225B2Reason")
                                    .isRentArrears(false)
                                    .build())
        );
    }

    private static Stream<Arguments> discretionaryGroundScenarios() {
        return Stream.of(
            Arguments.arguments(DiscretionaryGroundWales.RENT_ARREARS_S157,
                                ClaimGroundEntity.builder()
                                    .category(ClaimGroundCategory.WALES_STANDARD_OTHER_DISCRETIONARY)
                                    .code("RENT_ARREARS_S157")
                                    .reason(null)
                                    .isRentArrears(true)
                                    .build()),
            Arguments.arguments(DiscretionaryGroundWales.ANTISOCIAL_BEHAVIOUR_S157,
                                ClaimGroundEntity.builder()
                                    .category(ClaimGroundCategory.WALES_STANDARD_OTHER_DISCRETIONARY)
                                    .code("ANTISOCIAL_BEHAVIOUR_S157")
                                    .reason(null)
                                    .isRentArrears(false)
                                    .build()),
            Arguments.arguments(DiscretionaryGroundWales.OTHER_BREACH_OF_CONTRACT_S157,
                                ClaimGroundEntity.builder()
                                    .category(ClaimGroundCategory.WALES_STANDARD_OTHER_DISCRETIONARY)
                                    .code("OTHER_BREACH_OF_CONTRACT_S157")
                                    .reason("Reason from getOtherBreachSection157Reason")
                                    .isRentArrears(false)
                                    .build()),
            Arguments.arguments(DiscretionaryGroundWales.ESTATE_MANAGEMENT_GROUNDS_S160,
                                ClaimGroundEntity.builder()
                                    .category(ClaimGroundCategory.WALES_STANDARD_OTHER_DISCRETIONARY)
                                    .code("ESTATE_MANAGEMENT_GROUNDS_S160")
                                    .reason(null)
                                    .isRentArrears(false)
                                    .build())
        );
    }

    private static Stream<Arguments> estateManagementGroundScenarios() {
        return Stream.of(
            Arguments.arguments(EstateManagementGroundsWales.BUILDING_WORKS,
                                ClaimGroundEntity.builder()
                                    .category(ClaimGroundCategory.WALES_STANDARD_OTHER_ESTATE_MANAGEMENT)
                                    .code("BUILDING_WORKS")
                                    .reason("Reason from getBuildingWorksReason")
                                    .isRentArrears(false)
                                    .build()),
            Arguments.arguments(EstateManagementGroundsWales.REDEVELOPMENT_SCHEMES,
                                ClaimGroundEntity.builder()
                                    .category(ClaimGroundCategory.WALES_STANDARD_OTHER_ESTATE_MANAGEMENT)
                                    .code("REDEVELOPMENT_SCHEMES")
                                    .reason("Reason from getRedevelopmentSchemesReason")
                                    .isRentArrears(false)
                                    .build()),
            Arguments.arguments(EstateManagementGroundsWales.CHARITIES,
                                ClaimGroundEntity.builder()
                                    .category(ClaimGroundCategory.WALES_STANDARD_OTHER_ESTATE_MANAGEMENT)
                                    .code("CHARITIES")
                                    .reason("Reason from getCharitiesReason")
                                    .isRentArrears(false)
                                    .build()),
            Arguments.arguments(EstateManagementGroundsWales.DISABLED_SUITABLE_DWELLING,
                                ClaimGroundEntity.builder()
                                    .category(ClaimGroundCategory.WALES_STANDARD_OTHER_ESTATE_MANAGEMENT)
                                    .code("DISABLED_SUITABLE_DWELLING")
                                    .reason("Reason from getDisabledSuitableDwellingReason")
                                    .isRentArrears(false)
                                    .build()),
            Arguments.arguments(EstateManagementGroundsWales.HOUSING_ASSOCIATIONS_AND_TRUSTS,
                                ClaimGroundEntity.builder()
                                    .category(ClaimGroundCategory.WALES_STANDARD_OTHER_ESTATE_MANAGEMENT)
                                    .code("HOUSING_ASSOCIATIONS_AND_TRUSTS")
                                    .reason("Reason from getHousingAssociationsAndTrustsReason")
                                    .isRentArrears(false)
                                    .build()),
            Arguments.arguments(EstateManagementGroundsWales.SPECIAL_NEEDS_DWELLINGS,
                                ClaimGroundEntity.builder()
                                    .category(ClaimGroundCategory.WALES_STANDARD_OTHER_ESTATE_MANAGEMENT)
                                    .code("SPECIAL_NEEDS_DWELLINGS")
                                    .reason("Reason from getSpecialNeedsDwellingsReason")
                                    .isRentArrears(false)
                                    .build()),
            Arguments.arguments(EstateManagementGroundsWales.RESERVE_SUCCESSORS,
                                ClaimGroundEntity.builder()
                                    .category(ClaimGroundCategory.WALES_STANDARD_OTHER_ESTATE_MANAGEMENT)
                                    .code("RESERVE_SUCCESSORS")
                                    .reason("Reason from getReserveSuccessorsReason")
                                    .isRentArrears(false)
                                    .build()),
            Arguments.arguments(EstateManagementGroundsWales.JOINT_CONTRACT_HOLDERS,
                                ClaimGroundEntity.builder()
                                    .category(ClaimGroundCategory.WALES_STANDARD_OTHER_ESTATE_MANAGEMENT)
                                    .code("JOINT_CONTRACT_HOLDERS")
                                    .reason("Reason from getJointContractHoldersReason")
                                    .isRentArrears(false)
                                    .build()),
            Arguments.arguments(EstateManagementGroundsWales.OTHER_ESTATE_MANAGEMENT_REASONS,
                                ClaimGroundEntity.builder()
                                    .category(ClaimGroundCategory.WALES_STANDARD_OTHER_ESTATE_MANAGEMENT)
                                    .code("OTHER_ESTATE_MANAGEMENT_REASONS")
                                    .reason("Reason from getOtherEstateManagementReasonsReason")
                                    .isRentArrears(false)
                                    .build())
        );
    }

}
