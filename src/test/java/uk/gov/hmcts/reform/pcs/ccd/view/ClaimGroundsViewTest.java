package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredAdditionalDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredAdditionalMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredDiscretionaryGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredMandatoryGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredNoArrearsPossessionGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredRentArrearsGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredRentArrearsPossessionGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.ClaimGroundSummary;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.IntroductoryDemotedOtherGroundsForPossession;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.IntroductoryDemotedOtherGroundReason;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.IntroductoryDemotedOrOtherGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.IntroductoryDemotedOrOtherNoGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.RentArrearsGroundsReasons;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureAntisocialAdditionalGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleGroundsReasons;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleMandatoryGroundsAlternativeAccomm;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexiblePossessionGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.DiscretionaryGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.EstateManagementGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.GroundsForPossessionWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.MandatoryGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceDetailsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceTypeWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractGroundsForPossessionWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractDiscretionaryGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractMandatoryGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundCategory;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimGroundsViewTest {

    @Mock
    private PCSCase pcsCase;
    @Mock
    private PcsCaseEntity pcsCaseEntity;
    @Mock
    private ClaimEntity mainClaimEntity;
    @Captor
    private ArgumentCaptor<List<ListValue<ClaimGroundSummary>>> claimGroundListCaptor;

    private ClaimGroundsView underTest;

    @BeforeEach
    void setUp() {
        underTest = new ClaimGroundsView();
    }

    @Test
    void shouldNotSetAnythingIfNoMainClaim() {
        // Given
        when(pcsCaseEntity.getClaims()).thenReturn(List.of());

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        verify(pcsCase, never()).setClaimGroundSummaries(any());
    }

    @ParameterizedTest
    @MethodSource("claimGroundScenarios")
    void shouldSetClaimSummaryForAllCategories(ClaimGroundCategory groundCategory,
                                               String groundCode,
                                               String reason,
                                               String expectedLabel,
                                               boolean isRentArrears) {
        // Given
        when(pcsCaseEntity.getClaims()).thenReturn(List.of(mainClaimEntity));
        Set<ClaimGroundEntity> claimGrounds = Set.of(ClaimGroundEntity
                                                         .builder()
                                                         .id(UUID.randomUUID())
                                                         .category(groundCategory)
                                                         .code(groundCode)
                                                         .reason(reason)
                                                         .isRentArrears(isRentArrears)
                                                         .build());
        when(mainClaimEntity.getClaimGrounds()).thenReturn(claimGrounds);

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        verify(pcsCase).setClaimGroundSummaries(claimGroundListCaptor.capture());

        List<ListValue<ClaimGroundSummary>> claimGroundList = claimGroundListCaptor.getValue();
        assertThat(claimGroundList)
            .map(ListValue::getValue)
            .usingRecursiveFieldByFieldElementComparatorIgnoringFields("categoryRank", "groundRank")
            .containsExactly(
                ClaimGroundSummary.builder()
                    .category(groundCategory)
                    .code(groundCode)
                    .label(expectedLabel)
                    .reason(reason)
                    .isRentArrears(isRentArrears ? YesOrNo.YES : YesOrNo.NO)
                    .build()
            );
    }

    @Test
    void shouldSetDescriptionWhenGroundEntityHasItSet() {
        // Given
        String expectedDescription = "some ground description";

        when(pcsCaseEntity.getClaims()).thenReturn(List.of(mainClaimEntity));
        Set<ClaimGroundEntity> claimGrounds = Set.of(ClaimGroundEntity
                                                         .builder()
                                                         .id(UUID.randomUUID())
                                                         .category(ClaimGroundCategory.INTRODUCTORY_DEMOTED_OTHER)
                                                         .code(IntroductoryDemotedOrOtherGrounds.OTHER.name())
                                                         .description(expectedDescription)
                                                         .isRentArrears(false)
                                                         .build());
        when(mainClaimEntity.getClaimGrounds()).thenReturn(claimGrounds);

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        verify(pcsCase).setClaimGroundSummaries(claimGroundListCaptor.capture());

        List<ListValue<ClaimGroundSummary>> claimGroundList = claimGroundListCaptor.getValue();
        assertThat(claimGroundList)
            .map(ListValue::getValue)
            .usingRecursiveFieldByFieldElementComparatorIgnoringFields("categoryRank", "groundRank")
            .containsExactly(
                ClaimGroundSummary.builder()
                    .category(ClaimGroundCategory.INTRODUCTORY_DEMOTED_OTHER)
                    .code(IntroductoryDemotedOrOtherGrounds.OTHER.name())
                    .description(expectedDescription)
                    .label("Other")
                    .isRentArrears(YesOrNo.NO)
                    .build()
            );
    }

    @Test
    void shouldOrderGroundsByCategoryRankThenGroundRank() {
        // Given
        when(pcsCaseEntity.getClaims()).thenReturn(List.of(mainClaimEntity));

        // Grounds in different order to rank in ClaimGroundCategory and their respective ground enums
        Set<ClaimGroundEntity> claimGrounds = Set.of(ClaimGroundEntity
                                                         .builder()
                                                         .id(UUID.randomUUID())
                                                         .category(ClaimGroundCategory.INTRODUCTORY_DEMOTED_OTHER)
                                                         .code(IntroductoryDemotedOrOtherGrounds.ANTI_SOCIAL.name())
                                                         .build(),
                                                     ClaimGroundEntity
                                                         .builder()
                                                         .id(UUID.randomUUID())
                                                         .category(ClaimGroundCategory.ASSURED_MANDATORY)
                                                         .code(AssuredMandatoryGround.STUDENT_LET_GROUND4.name())
                                                         .build(),
                                                     ClaimGroundEntity
                                                         .builder()
                                                         .id(UUID.randomUUID())
                                                         .category(ClaimGroundCategory.ASSURED_DISCRETIONARY)
                                                         .code(AssuredDiscretionaryGround.RENT_ARREARS_GROUND10.name())
                                                         .build(),
                                                     ClaimGroundEntity
                                                         .builder()
                                                         .id(UUID.randomUUID())
                                                         .category(ClaimGroundCategory.ASSURED_MANDATORY)
                                                         .code(AssuredMandatoryGround.HOLIDAY_LET_GROUND3.name())
                                                         .build()
                                                     );
        when(mainClaimEntity.getClaimGrounds()).thenReturn(claimGrounds);

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        verify(pcsCase).setClaimGroundSummaries(claimGroundListCaptor.capture());

        List<ListValue<ClaimGroundSummary>> claimGroundList = claimGroundListCaptor.getValue();
        assertThat(claimGroundList)
            .map(ListValue::getValue)
            .map(ClaimGroundSummary::getCode)
            .containsExactly(
                AssuredMandatoryGround.HOLIDAY_LET_GROUND3.name(),
                AssuredMandatoryGround.STUDENT_LET_GROUND4.name(),
                AssuredDiscretionaryGround.RENT_ARREARS_GROUND10.name(),
                IntroductoryDemotedOrOtherGrounds.ANTI_SOCIAL.name()
            );
    }

    @Test
    void shouldBuildClaimGroundSummariesFromDraft() {
        // Given
        PCSCase draftCaseData = PCSCase.builder()
            .tenancyLicenceDetails(TenancyLicenceDetails.builder()
                                       .typeOfTenancyLicence(TenancyLicenceType.ASSURED_TENANCY)
                                       .build())
            .claimDueToRentArrears(YesOrNo.YES)
            .assuredRentArrearsPossessionGrounds(AssuredRentArrearsPossessionGrounds.builder()
                                                    .rentArrearsGrounds(Set.of(
                                                        AssuredRentArrearsGround.SERIOUS_RENT_ARREARS_GROUND8,
                                                        AssuredRentArrearsGround.RENT_ARREARS_GROUND10
                                                    ))
                                                    .additionalMandatoryGrounds(Set.of(
                                                        AssuredAdditionalMandatoryGrounds.OWNER_OCCUPIER_GROUND1
                                                    ))
                                                    .additionalDiscretionaryGrounds(Set.of(
                                                        AssuredAdditionalDiscretionaryGrounds.BREACH_TENANCY_GROUND12
                                                    ))
                                                    .build())
            .noRentArrearsGroundsOptions(AssuredNoArrearsPossessionGrounds.builder()
                                            .mandatoryGrounds(Set.of(AssuredMandatoryGround.HOLIDAY_LET_GROUND3))
                                            .discretionaryGrounds(Set.of(
                                                AssuredDiscretionaryGround.DETERIORATION_PROPERTY_GROUND13
                                            ))
                                            .build())
            .introductoryDemotedOrOtherGroundsForPossession(
                IntroductoryDemotedOtherGroundsForPossession.builder()
                    .introductoryDemotedOrOtherGrounds(Set.of(IntroductoryDemotedOrOtherGrounds.ANTI_SOCIAL))
                    .build()
            )
            .secureOrFlexiblePossessionGrounds(SecureOrFlexiblePossessionGrounds.builder()
                                                   .secureOrFlexibleMandatoryGrounds(Set.of(
                                                       SecureOrFlexibleMandatoryGrounds.ANTI_SOCIAL
                                                   ))
                                                   .secureOrFlexibleDiscretionaryGrounds(Set.of(
                                                       SecureOrFlexibleDiscretionaryGrounds
                                                           .RENT_ARREARS_OR_BREACH_OF_TENANCY
                                                   ))
                                                   .secureAntisocialAdditionalGrounds(Set.of(
                                                       SecureAntisocialAdditionalGrounds.S84A_CONDITION_1
                                                   ))
                                                   .secureOrFlexibleMandatoryGroundsAlt(Set.of(
                                                       SecureOrFlexibleMandatoryGroundsAlternativeAccomm.PROPERTY_SOLD
                                                   ))
                                                   .secureOrFlexibleDiscretionaryGroundsAlt(Set.of(
                                                       SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm
                                                           .ADAPTED_ACCOMMODATION
                                                       ))
                                                       .build())
            .rentArrearsGroundsReasons(RentArrearsGroundsReasons.builder()
                                           .ownerOccupierReason("Owner occupier reason")
                                           .breachOfTenancyConditionsReason("Breach reason")
                                           .build())
            .groundsForPossessionWales(GroundsForPossessionWales.builder()
                                           .mandatoryGrounds(Set.of(MandatoryGroundWales.LANDLORD_BREAK_CLAUSE_S199))
                                           .discretionaryGrounds(Set.of(DiscretionaryGroundWales.RENT_ARREARS_S157))
                                           .estateManagementGrounds(Set.of(
                                               EstateManagementGroundsWales.REDEVELOPMENT_SCHEMES
                                           ))
                                           .build())
            .secureContractGroundsForPossessionWales(SecureContractGroundsForPossessionWales.builder()
                                                        .mandatoryGrounds(Set.of(
                                                            SecureContractMandatoryGroundsWales.LANDLORD_NOTICE_S186
                                                        ))
                                                        .discretionaryGrounds(Set.of(
                                                            SecureContractDiscretionaryGroundsWales
                                                                .ANTISOCIAL_BEHAVIOUR_S157
                                                        ))
                                                        .estateManagementGrounds(Set.of(
                                                            EstateManagementGroundsWales.RESERVE_SUCCESSORS
                                                        ))
                                                        .build())
            .build();

        // When
        List<ListValue<ClaimGroundSummary>> summaries = underTest.buildClaimGroundSummariesFromDraft(draftCaseData);

        // Then
        assertThat(summaries)
            .map(ListValue::getValue)
            .map(ClaimGroundSummary::getLabel)
            .containsExactlyInAnyOrder(
                "Serious rent arrears (ground 8)",
                "Rent arrears (ground 10)",
                "Owner occupier (ground 1)",
                "Breach of tenancy conditions (ground 12)"
            );
        assertReason(summaries, "Owner occupier (ground 1)", "Owner occupier reason");
        assertReason(summaries, "Breach of tenancy conditions (ground 12)", "Breach reason");
        assertNoReason(summaries, "Serious rent arrears (ground 8)");
        assertNoReason(summaries, "Rent arrears (ground 10)");
    }

    @Test
    void shouldMapAllAssuredDraftGroundReasons() {
        // Given
        PCSCase draftCaseData = PCSCase.builder()
            .tenancyLicenceDetails(TenancyLicenceDetails.builder()
                                       .typeOfTenancyLicence(TenancyLicenceType.ASSURED_TENANCY)
                                       .build())
            .claimDueToRentArrears(YesOrNo.YES)
            .assuredRentArrearsPossessionGrounds(AssuredRentArrearsPossessionGrounds.builder()
                                                    .rentArrearsGrounds(Set.of(
                                                        AssuredRentArrearsGround.SERIOUS_RENT_ARREARS_GROUND8,
                                                        AssuredRentArrearsGround.RENT_ARREARS_GROUND10,
                                                        AssuredRentArrearsGround.PERSISTENT_DELAY_GROUND11
                                                    ))
                                                    .additionalMandatoryGrounds(Set.of(
                                                        AssuredAdditionalMandatoryGrounds.OWNER_OCCUPIER_GROUND1,
                                                        AssuredAdditionalMandatoryGrounds.REPOSSESSION_GROUND2,
                                                        AssuredAdditionalMandatoryGrounds.HOLIDAY_LET_GROUND3,
                                                        AssuredAdditionalMandatoryGrounds.STUDENT_LET_GROUND4,
                                                        AssuredAdditionalMandatoryGrounds.MINISTER_RELIGION_GROUND5,
                                                        AssuredAdditionalMandatoryGrounds.REDEVELOPMENT_GROUND6,
                                                        AssuredAdditionalMandatoryGrounds.DEATH_OF_TENANT_GROUND7,
                                                        AssuredAdditionalMandatoryGrounds.ANTISOCIAL_BEHAVIOUR_GROUND7A,
                                                        AssuredAdditionalMandatoryGrounds.NO_RIGHT_TO_RENT_GROUND7B
                                                    ))
                                                    .additionalDiscretionaryGrounds(Set.of(
                                                        AssuredAdditionalDiscretionaryGrounds
                                                            .ALTERNATIVE_ACCOMMODATION_GROUND9,
                                                        AssuredAdditionalDiscretionaryGrounds.BREACH_TENANCY_GROUND12,
                                                        AssuredAdditionalDiscretionaryGrounds
                                                            .DETERIORATION_PROPERTY_GROUND13,
                                                        AssuredAdditionalDiscretionaryGrounds
                                                            .NUISANCE_ANNOYANCE_GROUND14,
                                                        AssuredAdditionalDiscretionaryGrounds
                                                            .DOMESTIC_VIOLENCE_GROUND14A,
                                                        AssuredAdditionalDiscretionaryGrounds.OFFENCE_RIOT_GROUND14ZA,
                                                        AssuredAdditionalDiscretionaryGrounds
                                                            .DETERIORATION_FURNITURE_GROUND15,
                                                        AssuredAdditionalDiscretionaryGrounds
                                                            .EMPLOYEE_LANDLORD_GROUND16,
                                                        AssuredAdditionalDiscretionaryGrounds.FALSE_STATEMENT_GROUND17
                                                    ))
                                                    .build())
            .rentArrearsGroundsReasons(RentArrearsGroundsReasons.builder()
                                           .ownerOccupierReason("Owner reason")
                                           .repossessionByLenderReason("Lender reason")
                                           .holidayLetReason("Holiday reason")
                                           .studentLetReason("Student reason")
                                           .ministerOfReligionReason("Minister reason")
                                           .redevelopmentReason("Redevelopment reason")
                                           .deathOfTenantReason("Death reason")
                                           .antisocialBehaviourReason("ASB reason")
                                           .noRightToRentReason("No right reason")
                                           .suitableAltAccommodationReason("Alternative reason")
                                           .breachOfTenancyConditionsReason("Breach reason")
                                           .propertyDeteriorationReason("Property reason")
                                           .nuisanceAnnoyanceReason("Nuisance reason")
                                           .domesticViolenceReason("Domestic reason")
                                           .offenceDuringRiotReason("Riot reason")
                                           .furnitureDeteriorationReason("Furniture reason")
                                           .employeeOfLandlordReason("Employee reason")
                                           .tenancyByFalseStatementReason("False statement reason")
                                           .build())
            .build();

        // When
        List<ListValue<ClaimGroundSummary>> summaries =
            underTest.buildClaimGroundSummariesFromDraft(draftCaseData);

        // Then
        assertReason(summaries, AssuredMandatoryGround.OWNER_OCCUPIER_GROUND1.getLabel(), "Owner reason");
        assertReason(summaries, AssuredMandatoryGround.REPOSSESSION_GROUND2.getLabel(), "Lender reason");
        assertReason(summaries, AssuredMandatoryGround.HOLIDAY_LET_GROUND3.getLabel(), "Holiday reason");
        assertReason(summaries, AssuredMandatoryGround.STUDENT_LET_GROUND4.getLabel(), "Student reason");
        assertReason(summaries, AssuredMandatoryGround.MINISTER_RELIGION_GROUND5.getLabel(), "Minister reason");
        assertReason(summaries, AssuredMandatoryGround.REDEVELOPMENT_GROUND6.getLabel(), "Redevelopment reason");
        assertReason(summaries, AssuredMandatoryGround.DEATH_OF_TENANT_GROUND7.getLabel(), "Death reason");
        assertReason(summaries, AssuredMandatoryGround.ANTISOCIAL_BEHAVIOUR_GROUND7A.getLabel(), "ASB reason");
        assertReason(summaries, AssuredMandatoryGround.NO_RIGHT_TO_RENT_GROUND7B.getLabel(), "No right reason");
        assertReason(summaries, AssuredDiscretionaryGround.ALTERNATIVE_ACCOMMODATION_GROUND9.getLabel(),
                     "Alternative reason");
        assertReason(summaries, AssuredDiscretionaryGround.BREACH_TENANCY_GROUND12.getLabel(), "Breach reason");
        assertReason(summaries, AssuredDiscretionaryGround.DETERIORATION_PROPERTY_GROUND13.getLabel(),
                     "Property reason");
        assertReason(summaries, AssuredDiscretionaryGround.NUISANCE_ANNOYANCE_GROUND14.getLabel(), "Nuisance reason");
        assertReason(summaries, AssuredDiscretionaryGround.DOMESTIC_VIOLENCE_GROUND14A.getLabel(),
                     "Domestic reason");
        assertReason(summaries, AssuredDiscretionaryGround.OFFENCE_RIOT_GROUND14ZA.getLabel(), "Riot reason");
        assertReason(summaries, AssuredDiscretionaryGround.DETERIORATION_FURNITURE_GROUND15.getLabel(),
                     "Furniture reason");
        assertReason(summaries, AssuredDiscretionaryGround.EMPLOYEE_LANDLORD_GROUND16.getLabel(),
                     "Employee reason");
        assertReason(summaries, AssuredDiscretionaryGround.FALSE_STATEMENT_GROUND17.getLabel(),
                     "False statement reason");
        assertNoReason(summaries, AssuredMandatoryGround.SERIOUS_RENT_ARREARS_GROUND8.getLabel());
        assertNoReason(summaries, AssuredDiscretionaryGround.RENT_ARREARS_GROUND10.getLabel());
        assertNoReason(summaries, AssuredDiscretionaryGround.PERSISTENT_DELAY_GROUND11.getLabel());
    }

    @Test
    void shouldReturnEmptyClaimGroundSummariesFromEmptyDraft() {
        // When
        List<ListValue<ClaimGroundSummary>> summaries =
            underTest.buildClaimGroundSummariesFromDraft(PCSCase.builder().build());

        // Then
        assertThat(summaries).isEmpty();
    }

    @Test
    void shouldBuildSecureContractWalesClaimGroundSummariesFromDraft() {
        // Given
        PCSCase draftCaseData = PCSCase.builder()
            .occupationLicenceDetailsWales(OccupationLicenceDetailsWales.builder()
                                               .occupationLicenceTypeWales(OccupationLicenceTypeWales.SECURE_CONTRACT)
                                               .build())
            .secureContractGroundsForPossessionWales(SecureContractGroundsForPossessionWales.builder()
                                                        .mandatoryGrounds(Set.of(
                                                            SecureContractMandatoryGroundsWales.LANDLORD_NOTICE_S186
                                                        ))
                                                        .discretionaryGrounds(Set.of(
                                                            SecureContractDiscretionaryGroundsWales
                                                                .ANTISOCIAL_BEHAVIOUR_S157
                                                        ))
                                                        .estateManagementGrounds(Set.of(
                                                            EstateManagementGroundsWales.RESERVE_SUCCESSORS
                                                        ))
                                                        .build())
            .build();

        // When
        List<ListValue<ClaimGroundSummary>> summaries =
            underTest.buildClaimGroundSummariesFromDraft(draftCaseData);

        // Then
        assertThat(summaries)
            .map(ListValue::getValue)
            .map(ClaimGroundSummary::getLabel)
            .containsExactlyInAnyOrder(
                SecureContractMandatoryGroundsWales.LANDLORD_NOTICE_S186.getLabel(),
                SecureContractDiscretionaryGroundsWales.ANTISOCIAL_BEHAVIOUR_S157.getLabel(),
                EstateManagementGroundsWales.RESERVE_SUCCESSORS.getLabel()
            );
    }

    @Test
    void shouldBuildStandardContractWalesClaimGroundSummariesFromDraft() {
        // Given
        PCSCase draftCaseData = PCSCase.builder()
            .occupationLicenceDetailsWales(OccupationLicenceDetailsWales.builder()
                                               .occupationLicenceTypeWales(OccupationLicenceTypeWales.STANDARD_CONTRACT)
                                               .build())
            .groundsForPossessionWales(GroundsForPossessionWales.builder()
                                           .mandatoryGrounds(Set.of(MandatoryGroundWales.LANDLORD_BREAK_CLAUSE_S199))
                                           .discretionaryGrounds(Set.of(DiscretionaryGroundWales.RENT_ARREARS_S157))
                                           .estateManagementGrounds(Set.of(
                                               EstateManagementGroundsWales.REDEVELOPMENT_SCHEMES
                                           ))
                                           .build())
            .build();

        // When
        List<ListValue<ClaimGroundSummary>> summaries =
            underTest.buildClaimGroundSummariesFromDraft(draftCaseData);

        // Then
        assertThat(summaries)
            .map(ListValue::getValue)
            .map(ClaimGroundSummary::getLabel)
            .containsExactlyInAnyOrder(
                MandatoryGroundWales.LANDLORD_BREAK_CLAUSE_S199.getLabel(),
                DiscretionaryGroundWales.RENT_ARREARS_S157.getLabel(),
                EstateManagementGroundsWales.REDEVELOPMENT_SCHEMES.getLabel()
            );
    }

    @Test
    void shouldBuildAssuredNoRentArrearsClaimGroundSummariesFromDraft() {
        // Given
        PCSCase draftCaseData = PCSCase.builder()
            .tenancyLicenceDetails(TenancyLicenceDetails.builder()
                                       .typeOfTenancyLicence(TenancyLicenceType.ASSURED_TENANCY)
                                       .build())
            .claimDueToRentArrears(YesOrNo.NO)
            .noRentArrearsGroundsOptions(AssuredNoArrearsPossessionGrounds.builder()
                                            .mandatoryGrounds(Set.of(AssuredMandatoryGround.HOLIDAY_LET_GROUND3))
                                            .discretionaryGrounds(Set.of(
                                                AssuredDiscretionaryGround.DETERIORATION_PROPERTY_GROUND13
                                            ))
                                            .build())
            .rentArrearsGroundsReasons(RentArrearsGroundsReasons.builder()
                                           .holidayLetReason("Holiday reason")
                                           .propertyDeteriorationReason("Deterioration reason")
                                           .build())
            .build();

        // When
        List<ListValue<ClaimGroundSummary>> summaries =
            underTest.buildClaimGroundSummariesFromDraft(draftCaseData);

        // Then
        assertThat(summaries)
            .map(ListValue::getValue)
            .map(ClaimGroundSummary::getLabel)
            .containsExactlyInAnyOrder(
                AssuredMandatoryGround.HOLIDAY_LET_GROUND3.getLabel(),
                AssuredDiscretionaryGround.DETERIORATION_PROPERTY_GROUND13.getLabel()
            );
        assertReason(summaries, AssuredMandatoryGround.HOLIDAY_LET_GROUND3.getLabel(), "Holiday reason");
        assertReason(summaries, AssuredDiscretionaryGround.DETERIORATION_PROPERTY_GROUND13.getLabel(),
                     "Deterioration reason");
    }

    @Test
    void shouldBuildSecureOrFlexibleClaimGroundSummariesFromDraft() {
        // Given
        PCSCase draftCaseData = PCSCase.builder()
            .tenancyLicenceDetails(TenancyLicenceDetails.builder()
                                       .typeOfTenancyLicence(TenancyLicenceType.SECURE_TENANCY)
                                       .build())
            .secureOrFlexiblePossessionGrounds(SecureOrFlexiblePossessionGrounds.builder()
                                                   .secureOrFlexibleMandatoryGrounds(Set.of(
                                                       SecureOrFlexibleMandatoryGrounds.ANTI_SOCIAL
                                                   ))
                                                   .secureOrFlexibleDiscretionaryGrounds(Set.of(
                                                       SecureOrFlexibleDiscretionaryGrounds.RIOT_OFFENCE,
                                                       SecureOrFlexibleDiscretionaryGrounds
                                                           .PREMIUM_PAID_MUTUAL_EXCHANGE
                                                   ))
                                                   .secureAntisocialAdditionalGrounds(Set.of(
                                                       SecureAntisocialAdditionalGrounds.S84A_CONDITION_1
                                                   ))
                                                   .secureOrFlexibleMandatoryGroundsAlt(Set.of(
                                                       SecureOrFlexibleMandatoryGroundsAlternativeAccomm.LANDLORD_WORKS
                                                   ))
                                                   .secureOrFlexibleDiscretionaryGroundsAlt(Set.of(
                                                       SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm
                                                           .HOUSING_ASSOCIATION_SPECIAL_CIRCUMSTANCES
                                                   ))
                                                   .build())
            .secureOrFlexibleGroundsReasons(SecureOrFlexibleGroundsReasons.builder()
                                                .antiSocialGround("Antisocial reason")
                                                .riotOffenceGround("Riot reason")
                                                .premiumMutualExchangeGround("Premium reason")
                                                .antiSocialCondition1OfS84AGround("Condition 1 reason")
                                                .landlordWorksGround("Landlord works reason")
                                                .housingAssocSpecialGround("Housing association reason")
                                                .build())
            .build();

        // When
        List<ListValue<ClaimGroundSummary>> summaries =
            underTest.buildClaimGroundSummariesFromDraft(draftCaseData);

        // Then
        assertThat(summaries)
            .map(ListValue::getValue)
            .map(ClaimGroundSummary::getLabel)
            .containsExactlyInAnyOrder(
                SecureOrFlexibleMandatoryGrounds.ANTI_SOCIAL.getLabel(),
                SecureOrFlexibleDiscretionaryGrounds.RIOT_OFFENCE.getLabel(),
                SecureOrFlexibleDiscretionaryGrounds.PREMIUM_PAID_MUTUAL_EXCHANGE.getLabel(),
                SecureAntisocialAdditionalGrounds.S84A_CONDITION_1.getLabel(),
                SecureOrFlexibleMandatoryGroundsAlternativeAccomm.LANDLORD_WORKS.getLabel(),
                SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm.HOUSING_ASSOCIATION_SPECIAL_CIRCUMSTANCES
                    .getLabel()
            );
        assertReason(summaries, SecureOrFlexibleMandatoryGrounds.ANTI_SOCIAL.getLabel(), "Antisocial reason");
        assertReason(summaries, SecureOrFlexibleDiscretionaryGrounds.RIOT_OFFENCE.getLabel(), "Riot reason");
        assertReason(summaries, SecureOrFlexibleDiscretionaryGrounds.PREMIUM_PAID_MUTUAL_EXCHANGE.getLabel(),
                     "Premium reason");
        assertReason(summaries, SecureAntisocialAdditionalGrounds.S84A_CONDITION_1.getLabel(),
                     "Condition 1 reason");
        assertReason(summaries, SecureOrFlexibleMandatoryGroundsAlternativeAccomm.LANDLORD_WORKS.getLabel(),
                     "Landlord works reason");
        assertReason(summaries,
                     SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm
                         .HOUSING_ASSOCIATION_SPECIAL_CIRCUMSTANCES.getLabel(),
                     "Housing association reason");
    }

    @Test
    void shouldMapAllSecureAndFlexibleDraftGroundReasons() {
        // Given
        PCSCase draftCaseData = PCSCase.builder()
            .tenancyLicenceDetails(TenancyLicenceDetails.builder()
                                       .typeOfTenancyLicence(TenancyLicenceType.FLEXIBLE_TENANCY)
                                       .build())
            .secureOrFlexiblePossessionGrounds(SecureOrFlexiblePossessionGrounds.builder()
                                                   .secureOrFlexibleMandatoryGrounds(Set.of(
                                                       SecureOrFlexibleMandatoryGrounds.ANTI_SOCIAL
                                                   ))
                                                   .secureOrFlexibleDiscretionaryGrounds(Set.of(
                                                       SecureOrFlexibleDiscretionaryGrounds
                                                           .RENT_ARREARS_OR_BREACH_OF_TENANCY,
                                                       SecureOrFlexibleDiscretionaryGrounds.NUISANCE_OR_IMMORAL_USE,
                                                       SecureOrFlexibleDiscretionaryGrounds.DOMESTIC_VIOLENCE,
                                                       SecureOrFlexibleDiscretionaryGrounds.RIOT_OFFENCE,
                                                       SecureOrFlexibleDiscretionaryGrounds.PROPERTY_DETERIORATION,
                                                       SecureOrFlexibleDiscretionaryGrounds.FURNITURE_DETERIORATION,
                                                       SecureOrFlexibleDiscretionaryGrounds
                                                           .TENANCY_OBTAINED_BY_FALSE_STATEMENT,
                                                       SecureOrFlexibleDiscretionaryGrounds
                                                           .PREMIUM_PAID_MUTUAL_EXCHANGE,
                                                       SecureOrFlexibleDiscretionaryGrounds
                                                           .UNREASONABLE_CONDUCT_TIED_ACCOMMODATION,
                                                       SecureOrFlexibleDiscretionaryGrounds.REFUSAL_TO_MOVE_BACK
                                                   ))
                                                   .secureAntisocialAdditionalGrounds(Set.of(
                                                       SecureAntisocialAdditionalGrounds.S84A_CONDITION_1,
                                                       SecureAntisocialAdditionalGrounds.S84A_CONDITION_2,
                                                       SecureAntisocialAdditionalGrounds.S84A_CONDITION_3,
                                                       SecureAntisocialAdditionalGrounds.S84A_CONDITION_4,
                                                       SecureAntisocialAdditionalGrounds.S84A_CONDITION_5
                                                   ))
                                                   .secureOrFlexibleMandatoryGroundsAlt(Set.of(
                                                       SecureOrFlexibleMandatoryGroundsAlternativeAccomm.OVERCROWDING,
                                                       SecureOrFlexibleMandatoryGroundsAlternativeAccomm.LANDLORD_WORKS,
                                                       SecureOrFlexibleMandatoryGroundsAlternativeAccomm.PROPERTY_SOLD,
                                                       SecureOrFlexibleMandatoryGroundsAlternativeAccomm
                                                           .CHARITABLE_LANDLORD
                                                   ))
                                                   .secureOrFlexibleDiscretionaryGroundsAlt(Set.of(
                                                       SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm
                                                           .TIED_ACCOMMODATION_NEEDED_FOR_EMPLOYEE,
                                                       SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm
                                                           .ADAPTED_ACCOMMODATION,
                                                       SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm
                                                           .HOUSING_ASSOCIATION_SPECIAL_CIRCUMSTANCES,
                                                       SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm
                                                           .SPECIAL_NEEDS_ACCOMMODATION,
                                                       SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm
                                                           .UNDER_OCCUPYING_AFTER_SUCCESSION
                                                   ))
                                                   .build())
            .secureOrFlexibleGroundsReasons(SecureOrFlexibleGroundsReasons.builder()
                                                .antiSocialGround("ASB reason")
                                                .breachOfTenancyGround("Breach reason")
                                                .nuisanceOrImmoralUseGround("Nuisance reason")
                                                .domesticViolenceGround("Domestic reason")
                                                .riotOffenceGround("Riot reason")
                                                .propertyDeteriorationGround("Property reason")
                                                .furnitureDeteriorationGround("Furniture reason")
                                                .tenancyByFalseStatementGround("False statement reason")
                                                .premiumMutualExchangeGround("Premium reason")
                                                .unreasonableConductGround("Unreasonable reason")
                                                .refusalToMoveBackGround("Refusal reason")
                                                .antiSocialCondition1OfS84AGround("Condition 1 reason")
                                                .antiSocialCondition2OfS84AGround("Condition 2 reason")
                                                .antiSocialCondition3OfS84AGround("Condition 3 reason")
                                                .antiSocialCondition4OfS84AGround("Condition 4 reason")
                                                .antiSocialCondition5OfS84AGround("Condition 5 reason")
                                                .overcrowdingGround("Overcrowding reason")
                                                .landlordWorksGround("Works reason")
                                                .propertySoldGround("Sold reason")
                                                .charitableLandlordGround("Charitable reason")
                                                .tiedAccommodationGround("Tied reason")
                                                .adaptedAccommodationGround("Adapted reason")
                                                .housingAssocSpecialGround("Housing reason")
                                                .specialNeedsAccommodationGround("Special needs reason")
                                                .underOccupancySuccessionGround("Under occupation reason")
                                                .build())
            .build();

        // When
        List<ListValue<ClaimGroundSummary>> summaries =
            underTest.buildClaimGroundSummariesFromDraft(draftCaseData);

        // Then
        assertReason(summaries, SecureOrFlexibleMandatoryGrounds.ANTI_SOCIAL.getLabel(), "ASB reason");
        assertReason(summaries, SecureOrFlexibleDiscretionaryGrounds.RENT_ARREARS_OR_BREACH_OF_TENANCY.getLabel(),
                     "Breach reason");
        assertReason(summaries, SecureOrFlexibleDiscretionaryGrounds.NUISANCE_OR_IMMORAL_USE.getLabel(),
                     "Nuisance reason");
        assertReason(summaries, SecureOrFlexibleDiscretionaryGrounds.DOMESTIC_VIOLENCE.getLabel(),
                     "Domestic reason");
        assertReason(summaries, SecureOrFlexibleDiscretionaryGrounds.RIOT_OFFENCE.getLabel(), "Riot reason");
        assertReason(summaries, SecureOrFlexibleDiscretionaryGrounds.PROPERTY_DETERIORATION.getLabel(),
                     "Property reason");
        assertReason(summaries, SecureOrFlexibleDiscretionaryGrounds.FURNITURE_DETERIORATION.getLabel(),
                     "Furniture reason");
        assertReason(summaries, SecureOrFlexibleDiscretionaryGrounds.TENANCY_OBTAINED_BY_FALSE_STATEMENT.getLabel(),
                     "False statement reason");
        assertReason(summaries, SecureOrFlexibleDiscretionaryGrounds.PREMIUM_PAID_MUTUAL_EXCHANGE.getLabel(),
                     "Premium reason");
        assertReason(summaries, SecureOrFlexibleDiscretionaryGrounds.UNREASONABLE_CONDUCT_TIED_ACCOMMODATION
                     .getLabel(), "Unreasonable reason");
        assertReason(summaries, SecureOrFlexibleDiscretionaryGrounds.REFUSAL_TO_MOVE_BACK.getLabel(),
                     "Refusal reason");
        assertReason(summaries, SecureAntisocialAdditionalGrounds.S84A_CONDITION_1.getLabel(),
                     "Condition 1 reason");
        assertReason(summaries, SecureAntisocialAdditionalGrounds.S84A_CONDITION_2.getLabel(),
                     "Condition 2 reason");
        assertReason(summaries, SecureAntisocialAdditionalGrounds.S84A_CONDITION_3.getLabel(),
                     "Condition 3 reason");
        assertReason(summaries, SecureAntisocialAdditionalGrounds.S84A_CONDITION_4.getLabel(),
                     "Condition 4 reason");
        assertReason(summaries, SecureAntisocialAdditionalGrounds.S84A_CONDITION_5.getLabel(),
                     "Condition 5 reason");
        assertReason(summaries, SecureOrFlexibleMandatoryGroundsAlternativeAccomm.OVERCROWDING.getLabel(),
                     "Overcrowding reason");
        assertReason(summaries, SecureOrFlexibleMandatoryGroundsAlternativeAccomm.LANDLORD_WORKS.getLabel(),
                     "Works reason");
        assertReason(summaries, SecureOrFlexibleMandatoryGroundsAlternativeAccomm.PROPERTY_SOLD.getLabel(),
                     "Sold reason");
        assertReason(summaries, SecureOrFlexibleMandatoryGroundsAlternativeAccomm.CHARITABLE_LANDLORD.getLabel(),
                     "Charitable reason");
        assertReason(summaries,
                     SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm
                         .TIED_ACCOMMODATION_NEEDED_FOR_EMPLOYEE.getLabel(),
                     "Tied reason");
        assertReason(summaries, SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm.ADAPTED_ACCOMMODATION.getLabel(),
                     "Adapted reason");
        assertReason(summaries,
                     SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm
                         .HOUSING_ASSOCIATION_SPECIAL_CIRCUMSTANCES.getLabel(),
                     "Housing reason");
        assertReason(summaries,
                     SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm.SPECIAL_NEEDS_ACCOMMODATION.getLabel(),
                     "Special needs reason");
        assertReason(summaries,
                     SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm.UNDER_OCCUPYING_AFTER_SUCCESSION.getLabel(),
                     "Under occupation reason");
    }

    @Test
    void shouldBuildIntroductoryDemotedOrOtherClaimGroundSummariesFromDraft() {
        // Given
        PCSCase draftCaseData = PCSCase.builder()
            .tenancyLicenceDetails(TenancyLicenceDetails.builder()
                                       .typeOfTenancyLicence(TenancyLicenceType.INTRODUCTORY_TENANCY)
                                       .build())
            .introductoryDemotedOrOtherGroundsForPossession(
                IntroductoryDemotedOtherGroundsForPossession.builder()
                    .hasIntroductoryDemotedOtherGroundsForPossession(VerticalYesNo.YES)
                    .introductoryDemotedOrOtherGrounds(Set.of(
                        IntroductoryDemotedOrOtherGrounds.ANTI_SOCIAL,
                        IntroductoryDemotedOrOtherGrounds.RENT_ARREARS
                    ))
                    .build()
            )
            .introductoryDemotedOtherGroundReason(IntroductoryDemotedOtherGroundReason.builder()
                                                       .antiSocialBehaviourGround("Intro antisocial reason")
                                                       .build())
            .build();

        // When
        List<ListValue<ClaimGroundSummary>> summaries =
            underTest.buildClaimGroundSummariesFromDraft(draftCaseData);

        // Then
        assertThat(summaries)
            .map(ListValue::getValue)
            .map(ClaimGroundSummary::getLabel)
            .containsExactlyInAnyOrder(
                IntroductoryDemotedOrOtherGrounds.ANTI_SOCIAL.getLabel(),
                IntroductoryDemotedOrOtherGrounds.RENT_ARREARS.getLabel()
            );
        assertReason(summaries, IntroductoryDemotedOrOtherGrounds.ANTI_SOCIAL.getLabel(),
                     "Intro antisocial reason");
        assertNoReason(summaries, IntroductoryDemotedOrOtherGrounds.RENT_ARREARS.getLabel());
    }

    @Test
    void shouldBuildNoGroundsSummaryForIntroductoryDemotedOrOtherDraft() {
        // Given
        PCSCase draftCaseData = PCSCase.builder()
            .tenancyLicenceDetails(TenancyLicenceDetails.builder()
                                       .typeOfTenancyLicence(TenancyLicenceType.DEMOTED_TENANCY)
                                       .build())
            .introductoryDemotedOrOtherGroundsForPossession(
                IntroductoryDemotedOtherGroundsForPossession.builder()
                    .hasIntroductoryDemotedOtherGroundsForPossession(VerticalYesNo.NO)
                    .build()
            )
            .introductoryDemotedOtherGroundReason(IntroductoryDemotedOtherGroundReason.builder()
                                                       .noGrounds("No grounds reason")
                                                       .build())
            .build();

        // When
        List<ListValue<ClaimGroundSummary>> summaries =
            underTest.buildClaimGroundSummariesFromDraft(draftCaseData);

        // Then
        assertThat(summaries)
            .map(ListValue::getValue)
            .map(ClaimGroundSummary::getLabel)
            .containsExactly(IntroductoryDemotedOrOtherNoGrounds.NO_GROUNDS.getLabel());
        assertReason(summaries, IntroductoryDemotedOrOtherNoGrounds.NO_GROUNDS.getLabel(), "No grounds reason");
    }

    @Test
    void shouldBuildOtherTenancyClaimGroundSummariesFromDraftWithReasons() {
        // Given
        PCSCase draftCaseData = PCSCase.builder()
            .tenancyLicenceDetails(TenancyLicenceDetails.builder()
                                       .typeOfTenancyLicence(TenancyLicenceType.OTHER)
                                       .build())
            .introductoryDemotedOrOtherGroundsForPossession(
                IntroductoryDemotedOtherGroundsForPossession.builder()
                    .hasIntroductoryDemotedOtherGroundsForPossession(VerticalYesNo.YES)
                    .introductoryDemotedOrOtherGrounds(Set.of(
                        IntroductoryDemotedOrOtherGrounds.BREACH_OF_THE_TENANCY,
                        IntroductoryDemotedOrOtherGrounds.ABSOLUTE_GROUNDS,
                        IntroductoryDemotedOrOtherGrounds.OTHER
                    ))
                    .build()
            )
            .introductoryDemotedOtherGroundReason(IntroductoryDemotedOtherGroundReason.builder()
                                                       .breachOfTheTenancyGround("Breach reason")
                                                       .absoluteGrounds("Absolute reason")
                                                       .otherGround("Other reason")
                                                       .build())
            .build();

        // When
        List<ListValue<ClaimGroundSummary>> summaries =
            underTest.buildClaimGroundSummariesFromDraft(draftCaseData);

        // Then
        assertReason(summaries, IntroductoryDemotedOrOtherGrounds.BREACH_OF_THE_TENANCY.getLabel(),
                     "Breach reason");
        assertReason(summaries, IntroductoryDemotedOrOtherGrounds.ABSOLUTE_GROUNDS.getLabel(),
                     "Absolute reason");
        assertReason(summaries, IntroductoryDemotedOrOtherGrounds.OTHER.getLabel(), "Other reason");
    }

    @Test
    void shouldReturnEmptyClaimGroundSummariesForIntroductoryDraftWithoutSelectedGrounds() {
        // Given
        PCSCase draftCaseData = PCSCase.builder()
            .tenancyLicenceDetails(TenancyLicenceDetails.builder()
                                       .typeOfTenancyLicence(TenancyLicenceType.OTHER)
                                       .build())
            .build();

        // When
        List<ListValue<ClaimGroundSummary>> summaries =
            underTest.buildClaimGroundSummariesFromDraft(draftCaseData);

        // Then
        assertThat(summaries).isEmpty();
    }

    private static Stream<Arguments> claimGroundScenarios() {
        return Stream.of(
            argumentSet(
                "Assured tenancy mandatory non rent arrears ground",
                ClaimGroundCategory.ASSURED_MANDATORY,
                AssuredMandatoryGround.REDEVELOPMENT_GROUND6.name(),
                "Reason for redevelopment ground", // Reason
                "Property required for redevelopment (ground 6)", // Expected label
                false // Is rent arrears ground
            ),
            argumentSet(
                "Assured tenancy mandatory rent arrears ground",
                ClaimGroundCategory.ASSURED_MANDATORY,
                AssuredMandatoryGround.SERIOUS_RENT_ARREARS_GROUND8.name(),
                null, // Reason
                "Serious rent arrears (ground 8)", // Expected label
                true // Is rent arrears ground
            ),
            argumentSet(
                "Assured tenancy discretionary ground",
                ClaimGroundCategory.ASSURED_DISCRETIONARY,
                AssuredDiscretionaryGround.DETERIORATION_FURNITURE_GROUND15.name(),
                "Reason for deteriation of furniture ground", // Reason
                "Deterioration of furniture (ground 15)", // Expected label
                false // Is rent arrears ground
            ),
            argumentSet(
                "Secure or flexible tenancy mandatory ground",
                ClaimGroundCategory.SECURE_OR_FLEXIBLE_MANDATORY,
                SecureOrFlexibleMandatoryGrounds.ANTI_SOCIAL.name(),
                "Reason for deteriation of antisocial ground", // Reason
                "Antisocial behaviour", // Expected label
                false // Is rent arrears ground
            ),
            argumentSet(
                "Secure or flexible tenancy antisocial ground",
                ClaimGroundCategory.SECURE_OR_FLEXIBLE_ANTISOCIAL,
                SecureAntisocialAdditionalGrounds.S84A_CONDITION_1.name(),
                "Reason for antisocial ground", // Reason
                "Condition 1 of Section 84A of the Housing Act 1985", // Expected label
                false // Is rent arrears ground
            ),
            argumentSet(
                "Secure or flexible tenancy discretionary ground",
                ClaimGroundCategory.SECURE_OR_FLEXIBLE_DISCRETIONARY,
                SecureOrFlexibleDiscretionaryGrounds.RENT_ARREARS_OR_BREACH_OF_TENANCY.name(),
                "Reason for breach of tenancy ground", // Reason
                "Rent arrears or breach of the tenancy (ground 1)", // Expected label
                true // Is rent arrears ground
            ),
            argumentSet(
                "Secure or flexible tenancy mandatory alternative ground",
                ClaimGroundCategory.SECURE_OR_FLEXIBLE_MANDATORY_ALT,
                SecureOrFlexibleMandatoryGroundsAlternativeAccomm.PROPERTY_SOLD.name(),
                "Reason for property sold ground", // Reason
                "Property sold for redevelopment (ground 10A)", // Expected label
                false // Is rent arrears ground
            ),
            argumentSet(
                "Secure or flexible tenancy discretionary alternative ground",
                ClaimGroundCategory.SECURE_OR_FLEXIBLE_DISCRETIONARY_ALT,
                SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm.ADAPTED_ACCOMMODATION.name(),
                "Reason for adapted accomodation ground", // Reason
                "Adapted accommodation (ground 13)", // Expected label
                false // Is rent arrears ground
            ),
            argumentSet(
                "Introductory, demoted or other tenancy mandatory alternative ground",
                ClaimGroundCategory.INTRODUCTORY_DEMOTED_OTHER,
                IntroductoryDemotedOrOtherGrounds.ABSOLUTE_GROUNDS.name(),
                "Reason for absolute grounds", // Reason
                "Absolute grounds", // Expected label
                false // Is rent arrears ground
            ),
            argumentSet(
                "Introductory, demoted or other tenancy discretionary alternative ground",
                ClaimGroundCategory.INTRODUCTORY_DEMOTED_OTHER_NO_GROUNDS,
                IntroductoryDemotedOrOtherNoGrounds.NO_GROUNDS.name(),
                "Reason for no grounds", // Reason
                "No grounds", // Expected label
                false // Is rent arrears ground
            ),
            argumentSet(
                "Wales standard or other mandatory ground",
                ClaimGroundCategory.WALES_STANDARD_OTHER_MANDATORY,
                MandatoryGroundWales.LANDLORD_BREAK_CLAUSE_S199.name(),
                "Reason for break clause ground", // Reason
                "Notice given under a landlord’s break clause (section 199)", // Expected label
                false // Is rent arrears ground
            ),
            argumentSet(
                "Wales standard or other mandatory ground",
                ClaimGroundCategory.WALES_STANDARD_OTHER_DISCRETIONARY,
                DiscretionaryGroundWales.RENT_ARREARS_S157.name(),
                "Reason for rent arrears ground", // Reason
                "Rent arrears (breach of contract) (section 157)", // Expected label
                true // Is rent arrears ground
            ),
            argumentSet(
                "Wales standard or other estate management ground",
                ClaimGroundCategory.WALES_STANDARD_OTHER_ESTATE_MANAGEMENT,
                EstateManagementGroundsWales.REDEVELOPMENT_SCHEMES.name(),
                "Reason for redevelopment schemes ground", // Reason
                "Redevelopment schemes (ground B)", // Expected label
                false // Is rent arrears ground
            ),
            argumentSet(
                "Wales standard or other mandatory ground",
                ClaimGroundCategory.WALES_SECURE_MANDATORY,
                SecureContractMandatoryGroundsWales.LANDLORD_NOTICE_S186.name(),
                "Reason for landlord notice ground", // Reason
                "Landlord’s notice in connection with end of fixed term given (section 186)", // Expected label
                false // Is rent arrears ground
            ),
            argumentSet(
                "Wales standard or other mandatory ground",
                ClaimGroundCategory.WALES_SECURE_DISCRETIONARY,
                SecureContractDiscretionaryGroundsWales.ANTISOCIAL_BEHAVIOUR_S157.name(),
                "Reason for antisocial behaviour ground", // Reason
                "Antisocial behaviour (breach of contract) (section 157)", // Expected label
                true // Is rent arrears ground
            ),
            argumentSet(
                "Wales standard or other estate management ground",
                ClaimGroundCategory.WALES_SECURE_ESTATE_MANAGEMENT,
                EstateManagementGroundsWales.RESERVE_SUCCESSORS.name(),
                "Reason for reserve successors ground", // Reason
                "Reserve successors (ground G)", // Expected label
                false // Is rent arrears ground
            )
        );
    }

    private static void assertReason(List<ListValue<ClaimGroundSummary>> summaries,
                                     String label,
                                     String reason) {
        assertThat(summaries)
            .map(ListValue::getValue)
            .filteredOn(summary -> label.equals(summary.getLabel()))
            .singleElement()
            .extracting(ClaimGroundSummary::getReason)
            .isEqualTo(reason);
    }

    private static void assertNoReason(List<ListValue<ClaimGroundSummary>> summaries, String label) {
        assertThat(summaries)
            .map(ListValue::getValue)
            .filteredOn(summary -> label.equals(summary.getLabel()))
            .singleElement()
            .extracting(ClaimGroundSummary::getReason)
            .isNull();
    }

}
