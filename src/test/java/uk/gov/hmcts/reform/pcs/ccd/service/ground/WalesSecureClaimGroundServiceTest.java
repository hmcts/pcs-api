package uk.gov.hmcts.reform.pcs.ccd.service.ground;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.EstateManagementGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.GroundsReasonsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractDiscretionaryGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractGroundsForPossessionWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractMandatoryGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundCategory;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundEntity;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class WalesSecureClaimGroundServiceTest {

    private WalesSecureClaimGroundService underTest;

    @BeforeEach
    void setUp() {
        underTest = new WalesSecureClaimGroundService();
    }

    @ParameterizedTest
    @MethodSource(value = "mandatoryGroundScenarios")
    void shouldCreateMandatoryClaimGroundEntities(SecureContractMandatoryGroundsWales ground,
                                                  ClaimGroundEntity expectedClaimGroundEntity) {

        // Given
        Set<SecureContractMandatoryGroundsWales> mandatoryGrounds = Set.of(ground);
        GroundsReasonsWales reasons = createMockReasons();

        PCSCase caseData = PCSCase.builder()
                .legislativeCountry(LegislativeCountry.WALES)
                .secureContractGroundsForPossessionWales(
                        SecureContractGroundsForPossessionWales.builder()
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
    void shouldCreateDiscretionaryClaimGroundEntities(SecureContractDiscretionaryGroundsWales ground,
                                                      ClaimGroundEntity expectedClaimGroundEntity) {

        Set<SecureContractDiscretionaryGroundsWales> discretionaryGrounds = Set.of(ground);
        GroundsReasonsWales reasons = createMockReasons();

        PCSCase caseData = PCSCase.builder()
                .legislativeCountry(LegislativeCountry.WALES)
                .secureContractGroundsForPossessionWales(
                        SecureContractGroundsForPossessionWales.builder()
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

        Set<SecureContractDiscretionaryGroundsWales> discretionaryGrounds
                = Set.of(SecureContractDiscretionaryGroundsWales.ESTATE_MANAGEMENT_GROUNDS_S160);
        Set<EstateManagementGroundsWales> estateManagementGrounds = Set.of(ground);
        GroundsReasonsWales reasons = createMockReasons();

        PCSCase caseData = PCSCase.builder()
                .legislativeCountry(LegislativeCountry.WALES)
                .secureContractGroundsForPossessionWales(
                        SecureContractGroundsForPossessionWales.builder()
                                .mandatoryGrounds(Set.of())
                                .discretionaryGrounds(discretionaryGrounds)
                                .estateManagementGrounds(estateManagementGrounds)
                                .build()
                )
                .groundsReasonsWales(reasons)
                .build();

        ClaimGroundEntity topLevelEstateManagementGround = ClaimGroundEntity.builder()
                .category(ClaimGroundCategory.WALES_SECURE_DISCRETIONARY)
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
        Set<SecureContractDiscretionaryGroundsWales> secureDiscretionaryGrounds
                = Set.of(SecureContractDiscretionaryGroundsWales.RENT_ARREARS_S157,
                SecureContractDiscretionaryGroundsWales.ANTISOCIAL_BEHAVIOUR_S157,
                SecureContractDiscretionaryGroundsWales.OTHER_BREACH_OF_CONTRACT_S157);

        Set<EstateManagementGroundsWales> estateGroundsShouldBeIgnored
                = EnumSet.allOf(EstateManagementGroundsWales.class);

        PCSCase caseData = PCSCase.builder()
                .legislativeCountry(LegislativeCountry.WALES)
                .secureContractGroundsForPossessionWales(
                        SecureContractGroundsForPossessionWales.builder()
                                .mandatoryGrounds(Set.of())
                                .discretionaryGrounds(secureDiscretionaryGrounds)
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
                Arguments.arguments(SecureContractMandatoryGroundsWales.FAILURE_TO_GIVE_UP_POSSESSION_S170,
                        ClaimGroundEntity.builder()
                                .category(ClaimGroundCategory.WALES_SECURE_MANDATORY)
                                .code("FAILURE_TO_GIVE_UP_POSSESSION_S170")
                                .reason("Reason from getSecureFailureToGiveUpPossessionSection170Reason")
                                .isRentArrears(false)
                                .build()),
                Arguments.arguments(SecureContractMandatoryGroundsWales.LANDLORD_NOTICE_S186,
                        ClaimGroundEntity.builder()
                                .category(ClaimGroundCategory.WALES_SECURE_MANDATORY)
                                .code("LANDLORD_NOTICE_S186")
                                .reason("Reason from getSecureLandlordNoticeSection186Reason")
                                .isRentArrears(false)
                                .build()),
                Arguments.arguments(SecureContractMandatoryGroundsWales.FAILURE_TO_GIVE_UP_POSSESSION_S191,
                        ClaimGroundEntity.builder()
                                .category(ClaimGroundCategory.WALES_SECURE_MANDATORY)
                                .code("FAILURE_TO_GIVE_UP_POSSESSION_S191")
                                .reason("Reason from getSecureFailureToGiveUpPossessionSection191Reason")
                                .isRentArrears(false)
                                .build()),
                Arguments.arguments(SecureContractMandatoryGroundsWales.LANDLORD_NOTICE_S199,
                        ClaimGroundEntity.builder()
                                .category(ClaimGroundCategory.WALES_SECURE_MANDATORY)
                                .code("LANDLORD_NOTICE_S199")
                                .reason("Reason from getSecureLandlordNoticeSection199Reason")
                                .isRentArrears(false)
                                .build())
        );
    }

    private static Stream<Arguments> discretionaryGroundScenarios() {
        return Stream.of(
                Arguments.arguments(SecureContractDiscretionaryGroundsWales.RENT_ARREARS_S157,
                        ClaimGroundEntity.builder()
                                .category(ClaimGroundCategory.WALES_SECURE_DISCRETIONARY)
                                .code("RENT_ARREARS_S157")
                                .reason(null)
                                .isRentArrears(true)
                                .build()),
                Arguments.arguments(SecureContractDiscretionaryGroundsWales.ANTISOCIAL_BEHAVIOUR_S157,
                        ClaimGroundEntity.builder()
                                .category(ClaimGroundCategory.WALES_SECURE_DISCRETIONARY)
                                .code("ANTISOCIAL_BEHAVIOUR_S157")
                                .reason(null)
                                .isRentArrears(false)
                                .build()),
                Arguments.arguments(SecureContractDiscretionaryGroundsWales.OTHER_BREACH_OF_CONTRACT_S157,
                        ClaimGroundEntity.builder()
                                .category(ClaimGroundCategory.WALES_SECURE_DISCRETIONARY)
                                .code("OTHER_BREACH_OF_CONTRACT_S157")
                                .reason("Reason from getSecureOtherBreachOfContractReason")
                                .isRentArrears(false)
                                .build()),
                Arguments.arguments(SecureContractDiscretionaryGroundsWales.ESTATE_MANAGEMENT_GROUNDS_S160,
                        ClaimGroundEntity.builder()
                                .category(ClaimGroundCategory.WALES_SECURE_DISCRETIONARY)
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
                                .category(ClaimGroundCategory.WALES_SECURE_ESTATE_MANAGEMENT)
                                .code("BUILDING_WORKS")
                                .reason("Reason from getSecureBuildingWorksReason")
                                .isRentArrears(false)
                                .build()),
                Arguments.arguments(EstateManagementGroundsWales.REDEVELOPMENT_SCHEMES,
                        ClaimGroundEntity.builder()
                                .category(ClaimGroundCategory.WALES_SECURE_ESTATE_MANAGEMENT)
                                .code("REDEVELOPMENT_SCHEMES")
                                .reason("Reason from getSecureRedevelopmentSchemesReason")
                                .isRentArrears(false)
                                .build()),
                Arguments.arguments(EstateManagementGroundsWales.CHARITIES,
                        ClaimGroundEntity.builder()
                                .category(ClaimGroundCategory.WALES_SECURE_ESTATE_MANAGEMENT)
                                .code("CHARITIES")
                                .reason("Reason from getSecureCharitiesReason")
                                .isRentArrears(false)
                                .build()),
                Arguments.arguments(EstateManagementGroundsWales.DISABLED_SUITABLE_DWELLING,
                        ClaimGroundEntity.builder()
                                .category(ClaimGroundCategory.WALES_SECURE_ESTATE_MANAGEMENT)
                                .code("DISABLED_SUITABLE_DWELLING")
                                .reason("Reason from getSecureDisabledSuitableDwellingReason")
                                .isRentArrears(false)
                                .build()),
                Arguments.arguments(EstateManagementGroundsWales.HOUSING_ASSOCIATIONS_AND_TRUSTS,
                        ClaimGroundEntity.builder()
                                .category(ClaimGroundCategory.WALES_SECURE_ESTATE_MANAGEMENT)
                                .code("HOUSING_ASSOCIATIONS_AND_TRUSTS")
                                .reason("Reason from getSecureHousingAssociationsAndTrustsReason")
                                .isRentArrears(false)
                                .build()),
                Arguments.arguments(EstateManagementGroundsWales.SPECIAL_NEEDS_DWELLINGS,
                        ClaimGroundEntity.builder()
                                .category(ClaimGroundCategory.WALES_SECURE_ESTATE_MANAGEMENT)
                                .code("SPECIAL_NEEDS_DWELLINGS")
                                .reason("Reason from getSecureSpecialNeedsDwellingsReason")
                                .isRentArrears(false)
                                .build()),
                Arguments.arguments(EstateManagementGroundsWales.RESERVE_SUCCESSORS,
                        ClaimGroundEntity.builder()
                                .category(ClaimGroundCategory.WALES_SECURE_ESTATE_MANAGEMENT)
                                .code("RESERVE_SUCCESSORS")
                                .reason("Reason from getSecureReserveSuccessorsReason")
                                .isRentArrears(false)
                                .build()),
                Arguments.arguments(EstateManagementGroundsWales.JOINT_CONTRACT_HOLDERS,
                        ClaimGroundEntity.builder()
                                .category(ClaimGroundCategory.WALES_SECURE_ESTATE_MANAGEMENT)
                                .code("JOINT_CONTRACT_HOLDERS")
                                .reason("Reason from getSecureJointContractHoldersReason")
                                .isRentArrears(false)
                                .build()),
                Arguments.arguments(EstateManagementGroundsWales.OTHER_ESTATE_MANAGEMENT_REASONS,
                        ClaimGroundEntity.builder()
                                .category(ClaimGroundCategory.WALES_SECURE_ESTATE_MANAGEMENT)
                                .code("OTHER_ESTATE_MANAGEMENT_REASONS")
                                .reason("Reason from getSecureOtherEstateManagementReasonsReason")
                                .isRentArrears(false)
                                .build())
        );
    }

}
