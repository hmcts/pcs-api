package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.IntroductoryDemotedOrOtherGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoRentArrearsGroundsOptions;
import uk.gov.hmcts.reform.pcs.ccd.domain.IntroductoryDemotedOtherGroundReason;
import uk.gov.hmcts.reform.pcs.ccd.domain.IntroductoryDemotedOtherGroundsForPossession;
import uk.gov.hmcts.reform.pcs.ccd.domain.AssuredDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.AssuredMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsGroundsReasons;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsGroundsForPossession;
import uk.gov.hmcts.reform.pcs.ccd.domain.model.NoRentArrearsReasonForGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.MandatoryGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.EstateManagementGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.GroundsForPossessionWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractGroundsForPossessionWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.DiscretionaryGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.GroundsReasonsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractMandatoryGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractDiscretionaryGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundEntity;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static uk.gov.hmcts.reform.pcs.ccd.domain.IntroductoryDemotedOrOtherGrounds.ABSOLUTE_GROUNDS;
import static uk.gov.hmcts.reform.pcs.ccd.domain.IntroductoryDemotedOrOtherGrounds.ANTI_SOCIAL;
import static uk.gov.hmcts.reform.pcs.ccd.domain.IntroductoryDemotedOrOtherGrounds.BREACH_OF_THE_TENANCY;
import static uk.gov.hmcts.reform.pcs.ccd.domain.IntroductoryDemotedOrOtherGrounds.OTHER;
import static uk.gov.hmcts.reform.pcs.ccd.domain.IntroductoryDemotedOrOtherGrounds.RENT_ARREARS;
import static uk.gov.hmcts.reform.pcs.ccd.domain.IntroductoryDemotedOrOtherNoGrounds.NO_GROUNDS;
import static uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType.ASSURED_TENANCY;

@ExtendWith(MockitoExtension.class)
class ClaimGroundServiceTest {

    @InjectMocks
    private ClaimGroundService claimGroundService;

    @ParameterizedTest
    @MethodSource("groundsOtherThanRentArrearsScenarios")
    void shouldReturnClaimGroundEntities_WhenIntroDemotionOrOtherTenancy(
        Set<IntroductoryDemotedOrOtherGrounds> grounds) {
        // Given
        CaseDetails<PCSCase, State> caseDetails = new CaseDetails<>();

        IntroductoryDemotedOtherGroundsForPossession introductoryDemotedOtherGroundsForPossession =
            IntroductoryDemotedOtherGroundsForPossession.builder()
                .hasIntroductoryDemotedOtherGroundsForPossession(VerticalYesNo.YES)
                .introductoryDemotedOrOtherGrounds(grounds)
                .build();

        PCSCase caseData =
            PCSCase.builder()
                .introductoryDemotedOrOtherGroundsForPossession(introductoryDemotedOtherGroundsForPossession)
                .tenancyLicenceDetails(
                    TenancyLicenceDetails.builder()
                        .typeOfTenancyLicence(TenancyLicenceType.INTRODUCTORY_TENANCY)
                        .build()
                )
                .introductoryDemotedOtherGroundReason(getReasonForGround(grounds))
                .build();

        caseDetails.setData(caseData);

        // When
        List<ClaimGroundEntity> entities = claimGroundService.getGroundsWithReason(caseData);

        // Then
        assertThat(entities.getFirst().getGroundReason()).isNotBlank();
    }

    @Test
    void shouldNotHaveReasonIfRentArrearsGround_WhenIntroDemotionOrOtherTenancy() {
        // Given
        Set<IntroductoryDemotedOrOtherGrounds> grounds = Set.of(RENT_ARREARS);

        IntroductoryDemotedOtherGroundsForPossession introductoryDemotedOtherGroundsForPossession =
            IntroductoryDemotedOtherGroundsForPossession.builder()
                .hasIntroductoryDemotedOtherGroundsForPossession(VerticalYesNo.YES)
                .introductoryDemotedOrOtherGrounds(grounds)
                .build();

        PCSCase caseData =
            PCSCase.builder()
                .introductoryDemotedOrOtherGroundsForPossession(introductoryDemotedOtherGroundsForPossession)
                .tenancyLicenceDetails(
                    TenancyLicenceDetails.builder()
                        .typeOfTenancyLicence(TenancyLicenceType.INTRODUCTORY_TENANCY)
                        .build()
                )
                .introductoryDemotedOtherGroundReason(getReasonForGround(grounds))
                .build();

        // When
        List<ClaimGroundEntity> entities = claimGroundService.getGroundsWithReason(caseData);

        // Then
        assertThat(entities.getFirst().getGroundReason()).isBlank();
    }

    @Test
    void shouldSaveNoGroundsIfReasonIsPresent_WhenIntroDemotionOrOtherTenancy() {
        // Given

        IntroductoryDemotedOtherGroundsForPossession introductoryDemotedOtherGroundsForPossession =
            IntroductoryDemotedOtherGroundsForPossession.builder()
                .hasIntroductoryDemotedOtherGroundsForPossession(VerticalYesNo.YES)
                .introductoryDemotedOrOtherGrounds(null)
                .hasIntroductoryDemotedOtherGroundsForPossession(VerticalYesNo.NO)
                .build();

        PCSCase caseData = PCSCase.builder()
            .tenancyLicenceDetails(
                TenancyLicenceDetails.builder()
                    .typeOfTenancyLicence(TenancyLicenceType.INTRODUCTORY_TENANCY)
                    .build()
            )
            .introductoryDemotedOrOtherGroundsForPossession(introductoryDemotedOtherGroundsForPossession)
            .introductoryDemotedOtherGroundReason(
                IntroductoryDemotedOtherGroundReason.builder()
                    .noGrounds("No ground reason")
                    .build())
            .build();

        // When
        List<ClaimGroundEntity> entities = claimGroundService.getGroundsWithReason(caseData);

        // Then
        assertThat(entities.size()).isEqualTo(1);
        assertThat(entities.getFirst().getGroundReason()).isEqualTo("No ground reason");
        assertThat(entities.getFirst().getGroundId()).isEqualTo(NO_GROUNDS.name());
    }

    @Test
    void shouldReturnClaimGroundEntities_WhenAssuredTenancyNoRentArrears() {
        NoRentArrearsReasonForGrounds grounds = NoRentArrearsReasonForGrounds.builder()
            .ownerOccupierTextArea("Owner occupier reason")
            .repossessionByLenderTextArea("Repossession reason")
            .holidayLetTextArea("Holiday let reason")
            .studentLetTextArea("Student let reason")
            .ministerOfReligionTextArea("Minister of religion reason")
            .redevelopmentTextArea("Redevelopment reason")
            .deathOfTenantTextArea("Death of tenant reason")
            .antisocialBehaviourTextArea("Antisocial behaviour reason")
            .noRightToRentTextArea("No right to rent reason")
            .suitableAccomTextArea("Suitable alternative accommodation reason")
            .breachOfTenancyConditionsTextArea("Breach of tenancy conditions reason")
            .propertyDeteriorationTextArea("Property deterioration reason")
            .nuisanceOrIllegalUseTextArea("Nuisance reason")
            .domesticViolenceTextArea("Domestic violence reason")
            .offenceDuringRiotTextArea("Offence during riot reason")
            .furnitureDeteriorationTextArea("Furniture deterioration reason")
            .landlordEmployeeTextArea("Landlord employee reason")
            .falseStatementTextArea("False statement reason")
            .build();

        Set<AssuredMandatoryGrounds> mandatory = EnumSet.allOf(AssuredMandatoryGrounds.class);
        Set<AssuredDiscretionaryGrounds> discretionary = EnumSet.allOf(AssuredDiscretionaryGrounds.class);

        PCSCase caseData = PCSCase.builder()
            .noRentArrearsGroundsOptions(
                NoRentArrearsGroundsOptions.builder()
                    .mandatoryGrounds(mandatory)
                    .discretionaryGrounds(discretionary)
                    .build()
            )
            .noRentArrearsReasonForGrounds(grounds)
            .tenancyLicenceDetails(
                TenancyLicenceDetails.builder()
                    .typeOfTenancyLicence(TenancyLicenceType.ASSURED_TENANCY)
                    .build()
            )
            .build();

        List<ClaimGroundEntity> entities = claimGroundService.getGroundsWithReason(
            caseData
        );

        // Check size
        assertThat(entities).hasSize(mandatory.size() + discretionary.size());

        // Expected pairs: ground ID -> reason
        Map<String, String> expectedReasons = Map.ofEntries(
            entry("OWNER_OCCUPIER_GROUND1", "Owner occupier reason"),
            entry("REPOSSESSION_GROUND2", "Repossession reason"),
            entry("HOLIDAY_LET_GROUND3", "Holiday let reason"),
            entry("STUDENT_LET_GROUND4", "Student let reason"),
            entry("MINISTER_RELIGION_GROUND5", "Minister of religion reason"),
            entry("REDEVELOPMENT_GROUND6", "Redevelopment reason"),
            entry("DEATH_OF_TENANT_GROUND7", "Death of tenant reason"),
            entry("ANTISOCIAL_BEHAVIOUR_GROUND7A", "Antisocial behaviour reason"),
            entry("NO_RIGHT_TO_RENT_GROUND7B", "No right to rent reason"),
            entry("ALTERNATIVE_ACCOMMODATION_GROUND9", "Suitable alternative accommodation reason"),
            entry("BREACH_TENANCY_GROUND12", "Breach of tenancy conditions reason"),
            entry("DETERIORATION_PROPERTY_GROUND13", "Property deterioration reason"),
            entry("NUISANCE_ANNOYANCE_GROUND14", "Nuisance reason"),
            entry("DOMESTIC_VIOLENCE_GROUND14A", "Domestic violence reason"),
            entry("OFFENCE_RIOT_GROUND14ZA", "Offence during riot reason"),
            entry("DETERIORATION_FURNITURE_GROUND15", "Furniture deterioration reason"),
            entry("EMPLOYEE_LANDLORD_GROUND16", "Landlord employee reason"),
            entry("FALSE_STATEMENT_GROUND17", "False statement reason")
        );

        expectedReasons.forEach((groundId, reason) ->
                                    assertThat(entities.stream().anyMatch(
                                        e -> e.getGroundId().equals(groundId) && e.getGroundReason().equals(reason)
                                    )).isTrue()
        );
    }

    @Test
    void shouldIgnoreNullGrounds_WhenAssuredTenancyNoRentArrears() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .tenancyLicenceDetails(
                TenancyLicenceDetails.builder()
                    .typeOfTenancyLicence(ASSURED_TENANCY)
                    .build()
            )
            .noRentArrearsGroundsOptions(
                NoRentArrearsGroundsOptions.builder()
                    .mandatoryGrounds(null)
                    .discretionaryGrounds(null)
                    .build()
            )
            .build();

        // When
        List<ClaimGroundEntity> groundEntities = claimGroundService.getGroundsWithReason(caseData);

        // Then
        assertThat(groundEntities).isEmpty();
    }

    @Test
    void shouldReturnClaimGroundEntities_WhenAssuredTenancyRentArrears() {
        // Given
        Set<AssuredMandatoryGrounds> mandatoryGrounds = Set.of(
            AssuredMandatoryGrounds.OWNER_OCCUPIER_GROUND1,
            AssuredMandatoryGrounds.REDEVELOPMENT_GROUND6
        );

        Set<AssuredDiscretionaryGrounds> discretionaryGrounds = Set.of(
            AssuredDiscretionaryGrounds.BREACH_TENANCY_GROUND12,
            AssuredDiscretionaryGrounds.NUISANCE_ANNOYANCE_GROUND14
        );

        RentArrearsGroundsReasons reasons = RentArrearsGroundsReasons.builder()
            .ownerOccupierReason("Owner occupier needs the property")
            .redevelopmentReason("Redevelopment planned")
            .breachOfTenancyConditionsReason("Tenant breached agreement")
            .nuisanceAnnoyanceReason("Tenant caused nuisance")
            .build();

        PCSCase caseData = PCSCase.builder()
            .tenancyLicenceDetails(
                TenancyLicenceDetails.builder()
                    .typeOfTenancyLicence(TenancyLicenceType.ASSURED_TENANCY)
                    .build()
            )
            .claimDueToRentArrears(YesOrNo.YES)
            .rentArrearsGroundsForPossession(
                RentArrearsGroundsForPossession.builder()
                .mandatoryGrounds(mandatoryGrounds)
                .discretionaryGrounds(discretionaryGrounds)
                    .build()
            )
            .rentArrearsGroundsReasons(reasons)
            .build();

        // When
        List<ClaimGroundEntity> result = claimGroundService.getGroundsWithReason(caseData);

        // Then
        assertThat(result).hasSize(mandatoryGrounds.size() + discretionaryGrounds.size());

        Map<String, String> groundAndReason = result.stream()
            .collect(Collectors.toMap(ClaimGroundEntity::getGroundId, ClaimGroundEntity::getGroundReason));

        assertThat(groundAndReason)
            .containsEntry("OWNER_OCCUPIER_GROUND1", "Owner occupier needs the property")
            .containsEntry("REDEVELOPMENT_GROUND6", "Redevelopment planned")
            .containsEntry("BREACH_TENANCY_GROUND12", "Tenant breached agreement")
            .containsEntry("NUISANCE_ANNOYANCE_GROUND14", "Tenant caused nuisance");
    }

    @Test
    void shouldSaveOnlyRentArrearsGrounds_WhenAssuredTenancyRentArrears() {
        // Given
        Set<RentArrearsGround> rentArrearsGrounds = Set.of(
            RentArrearsGround.SERIOUS_RENT_ARREARS_GROUND8,
            RentArrearsGround.RENT_ARREARS_GROUND10,
            RentArrearsGround.PERSISTENT_DELAY_GROUND11
        );

        PCSCase caseData = PCSCase.builder()
            .tenancyLicenceDetails(
                TenancyLicenceDetails.builder()
                    .typeOfTenancyLicence(TenancyLicenceType.ASSURED_TENANCY)
                    .build()
            )
            .claimDueToRentArrears(YesOrNo.YES)
            .rentArrearsGroundsForPossession(
                RentArrearsGroundsForPossession.builder()
                .rentArrearsGrounds(rentArrearsGrounds)
                    .build()
            )
            .build();

        // When
        List<ClaimGroundEntity> result = claimGroundService.getGroundsWithReason(caseData);

        // Then
        assertThat(result)
            .extracting(ClaimGroundEntity::getGroundId)
            .containsExactlyInAnyOrder(
                RentArrearsGround.SERIOUS_RENT_ARREARS_GROUND8.name(),
                RentArrearsGround.RENT_ARREARS_GROUND10.name(),
                RentArrearsGround.PERSISTENT_DELAY_GROUND11.name()
            );

    }

    private static IntroductoryDemotedOtherGroundReason getReasonForGround(
        Set<IntroductoryDemotedOrOtherGrounds> grounds) {
        IntroductoryDemotedOtherGroundReason reasonForGround = null;

        for (IntroductoryDemotedOrOtherGrounds ground : grounds) {
            if (ground.equals(ABSOLUTE_GROUNDS)) {
                reasonForGround =
                    IntroductoryDemotedOtherGroundReason.builder()
                        .absoluteGrounds("Absolute reason")
                        .build();
            } else if (ground.equals(ANTI_SOCIAL)) {
                reasonForGround =
                    IntroductoryDemotedOtherGroundReason.builder()
                        .antiSocialBehaviourGround("Antisocial behaviour reason")
                        .build();
            } else if (ground.equals(BREACH_OF_THE_TENANCY)) {
                reasonForGround =
                    IntroductoryDemotedOtherGroundReason.builder()
                        .breachOfTheTenancyGround("Breach of the tenancy reason")
                        .build();
            } else if (ground.equals(OTHER)) {
                reasonForGround =
                    IntroductoryDemotedOtherGroundReason.builder()
                        .otherGround("Other grounds reason")
                        .build();
            }
        }
        return reasonForGround;
    }

    private static Stream<Arguments> groundsOtherThanRentArrearsScenarios() {
        return Stream.of(
            arguments(Set.of(ABSOLUTE_GROUNDS)),
            arguments(Set.of(IntroductoryDemotedOrOtherGrounds.ANTI_SOCIAL)),
            arguments(Set.of(IntroductoryDemotedOrOtherGrounds.BREACH_OF_THE_TENANCY)),
            arguments(Set.of(IntroductoryDemotedOrOtherGrounds.OTHER))
        );
    }

    @Test
    void shouldReturnClaimGroundEntities_WhenWalesGrounds() {
        // Given
        Set<MandatoryGroundWales> mandatoryGrounds = Set.of(
            MandatoryGroundWales.FAIL_TO_GIVE_UP_S170,
            MandatoryGroundWales.LANDLORD_NOTICE_PERIODIC_S178,
            MandatoryGroundWales.SERIOUS_ARREARS_PERIODIC_S181,
            MandatoryGroundWales.LANDLORD_NOTICE_FT_END_S186,
            MandatoryGroundWales.SERIOUS_ARREARS_FIXED_TERM_S187,
            MandatoryGroundWales.FAIL_TO_GIVE_UP_BREAK_NOTICE_S191,
            MandatoryGroundWales.LANDLORD_BREAK_CLAUSE_S199,
            MandatoryGroundWales.CONVERTED_FIXED_TERM_SCH12_25B2
        );
        Set<DiscretionaryGroundWales> discretionaryGrounds = Set.of(
            DiscretionaryGroundWales.OTHER_BREACH_SECTION_157
        );
        Set<EstateManagementGroundsWales> estateGrounds = Set.of(
            EstateManagementGroundsWales.BUILDING_WORKS,
            EstateManagementGroundsWales.REDEVELOPMENT_SCHEMES,
            EstateManagementGroundsWales.CHARITIES,
            EstateManagementGroundsWales.DISABLED_SUITABLE_DWELLING,
            EstateManagementGroundsWales.HOUSING_ASSOCIATIONS_AND_TRUSTS,
            EstateManagementGroundsWales.SPECIAL_NEEDS_DWELLINGS,
            EstateManagementGroundsWales.RESERVE_SUCCESSORS,
            EstateManagementGroundsWales.JOINT_CONTRACT_HOLDERS,
            EstateManagementGroundsWales.OTHER_ESTATE_MANAGEMENT_REASONS
        );
        Set<SecureContractMandatoryGroundsWales> secureMandatoryGrounds = Set.of(
            SecureContractMandatoryGroundsWales.FAILURE_TO_GIVE_UP_POSSESSION_SECTION_170,
            SecureContractMandatoryGroundsWales.LANDLORD_NOTICE_SECTION_186,
            SecureContractMandatoryGroundsWales.FAILURE_TO_GIVE_UP_POSSESSION_SECTION_191,
            SecureContractMandatoryGroundsWales.LANDLORD_NOTICE_SECTION_199
        );
        Set<SecureContractDiscretionaryGroundsWales> secureDiscretionaryGrounds = Set.of(
            SecureContractDiscretionaryGroundsWales.OTHER_BREACH_OF_CONTRACT
        );

        GroundsReasonsWales reasons = GroundsReasonsWales.builder()
            .failToGiveUpS170Reason("Failure to give up S170")
            .landlordNoticePeriodicS178Reason("Landlord notice periodic S178")
            .seriousArrearsPeriodicS181Reason("Serious arrears periodic S181")
            .landlordNoticeFtEndS186Reason("Landlord notice FT end S186")
            .seriousArrearsFixedTermS187Reason("Serious arrears fixed term S187")
            .failToGiveUpBreakNoticeS191Reason("Fail to give up break notice S191")
            .landlordBreakClauseS199Reason("Landlord break clause S199")
            .convertedFixedTermSch1225B2Reason("Converted fixed term Sch12 25B2")
            .otherBreachSection157Reason("Other breach section 157")
            .buildingWorksReason("Building works")
            .redevelopmentSchemesReason("Redevelopment schemes")
            .charitiesReason("Charities")
            .disabledSuitableDwellingReason("Disabled suitable dwelling")
            .housingAssociationsAndTrustsReason("Housing associations and trusts")
            .specialNeedsDwellingsReason("Special needs dwellings")
            .reserveSuccessorsReason("Reserve successors")
            .jointContractHoldersReason("Joint contract holders")
            .otherEstateManagementReasonsReason("Other estate management reasons")
            .secureFailureToGiveUpPossessionSection170Reason("Secure failure S170")
            .secureLandlordNoticeSection186Reason("Secure landlord notice S186")
            .secureFailureToGiveUpPossessionSection191Reason("Secure failure S191")
            .secureLandlordNoticeSection199Reason("Secure landlord notice S199")
            .secureOtherBreachOfContractReason("Secure other breach")
            .build();

        PCSCase caseData = PCSCase.builder()
            .legislativeCountry(LegislativeCountry.WALES)
            .groundsForPossessionWales(GroundsForPossessionWales.builder()
                .mandatoryGroundsWales(mandatoryGrounds)
                .discretionaryGroundsWales(discretionaryGrounds)
                .estateManagementGroundsWales(estateGrounds)
                .build())
            .secureContractGroundsForPossessionWales(
                SecureContractGroundsForPossessionWales.builder()
                    .mandatoryGroundsWales(secureMandatoryGrounds)
                    .discretionaryGroundsWales(secureDiscretionaryGrounds)
                    .build()
            )

            .groundsReasonsWales(reasons)
            .build();

        // When
        List<ClaimGroundEntity> result = claimGroundService.getGroundsWithReason(caseData);

        // Then
        int expectedSize = mandatoryGrounds.size() + discretionaryGrounds.size() + estateGrounds.size()
            + secureMandatoryGrounds.size() + secureDiscretionaryGrounds.size();
        assertThat(result).hasSize(expectedSize);

        Map<String, String> groundAndReason = result.stream()
            .collect(Collectors.toMap(ClaimGroundEntity::getGroundId, ClaimGroundEntity::getGroundReason));

        assertThat(groundAndReason)
            .containsEntry("FAIL_TO_GIVE_UP_S170", "Failure to give up S170")
            .containsEntry("LANDLORD_NOTICE_PERIODIC_S178", "Landlord notice periodic S178")
            .containsEntry("SERIOUS_ARREARS_PERIODIC_S181", "Serious arrears periodic S181")
            .containsEntry("LANDLORD_NOTICE_FT_END_S186", "Landlord notice FT end S186")
            .containsEntry("SERIOUS_ARREARS_FIXED_TERM_S187", "Serious arrears fixed term S187")
            .containsEntry("FAIL_TO_GIVE_UP_BREAK_NOTICE_S191", "Fail to give up break notice S191")
            .containsEntry("LANDLORD_BREAK_CLAUSE_S199", "Landlord break clause S199")
            .containsEntry("CONVERTED_FIXED_TERM_SCH12_25B2", "Converted fixed term Sch12 25B2")
            .containsEntry("OTHER_BREACH_SECTION_157", "Other breach section 157")
            .containsEntry("BUILDING_WORKS", "Building works")
            .containsEntry("REDEVELOPMENT_SCHEMES", "Redevelopment schemes")
            .containsEntry("CHARITIES", "Charities")
            .containsEntry("DISABLED_SUITABLE_DWELLING", "Disabled suitable dwelling")
            .containsEntry("HOUSING_ASSOCIATIONS_AND_TRUSTS", "Housing associations and trusts")
            .containsEntry("SPECIAL_NEEDS_DWELLINGS", "Special needs dwellings")
            .containsEntry("RESERVE_SUCCESSORS", "Reserve successors")
            .containsEntry("JOINT_CONTRACT_HOLDERS", "Joint contract holders")
            .containsEntry("OTHER_ESTATE_MANAGEMENT_REASONS", "Other estate management reasons")
            .containsEntry("FAILURE_TO_GIVE_UP_POSSESSION_SECTION_170", "Secure failure S170")
            .containsEntry("LANDLORD_NOTICE_SECTION_186", "Secure landlord notice S186")
            .containsEntry("FAILURE_TO_GIVE_UP_POSSESSION_SECTION_191", "Secure failure S191")
            .containsEntry("LANDLORD_NOTICE_SECTION_199", "Secure landlord notice S199")
            .containsEntry("OTHER_BREACH_OF_CONTRACT", "Secure other breach");
    }

    @Test
    void shouldHandleWalesBeforeTenancyTypeCheck() {
        // Given
        Set<MandatoryGroundWales> mandatoryGrounds = Set.of(
            MandatoryGroundWales.FAIL_TO_GIVE_UP_S170
        );

        GroundsReasonsWales reasons = GroundsReasonsWales.builder()
            .failToGiveUpS170Reason("Test reason")
            .build();

        PCSCase caseData = PCSCase.builder()
            .legislativeCountry(LegislativeCountry.WALES)
            .tenancyLicenceDetails(
                TenancyLicenceDetails.builder()
                    .typeOfTenancyLicence(null)
                    .build()
            )
            .groundsForPossessionWales(GroundsForPossessionWales.builder()
                .mandatoryGroundsWales(mandatoryGrounds)
                .build())
            .groundsReasonsWales(reasons)
            .build();

        // When
        List<ClaimGroundEntity> result = claimGroundService.getGroundsWithReason(caseData);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getGroundId()).isEqualTo("FAIL_TO_GIVE_UP_S170");
        assertThat(result.getFirst().getGroundReason()).isEqualTo("Test reason");
    }
}
