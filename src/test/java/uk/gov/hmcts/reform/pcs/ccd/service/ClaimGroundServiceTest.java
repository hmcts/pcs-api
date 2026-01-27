package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.AssuredAdditionalDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.AssuredAdditionalMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.AssuredDiscretionaryGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.AssuredMandatoryGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.AssuredNoArrearsPossessionGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.AssuredRentArrearsGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.AssuredRentArrearsPossessionGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.IntroductoryDemotedOrOtherGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.IntroductoryDemotedOtherGroundReason;
import uk.gov.hmcts.reform.pcs.ccd.domain.IntroductoryDemotedOtherGroundsForPossession;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsGroundsReasons;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsOrBreachOfTenancy;
import uk.gov.hmcts.reform.pcs.ccd.domain.SecureOrFlexibleDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm;
import uk.gov.hmcts.reform.pcs.ccd.domain.SecureOrFlexibleGroundsReasons;
import uk.gov.hmcts.reform.pcs.ccd.domain.SecureOrFlexibleMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.SecureOrFlexibleMandatoryGroundsAlternativeAccomm;
import uk.gov.hmcts.reform.pcs.ccd.domain.SecureOrFlexiblePossessionGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.model.NoRentArrearsReasonForGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.DiscretionaryGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.EstateManagementGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.GroundsForPossessionWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.GroundsReasonsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.MandatoryGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractDiscretionaryGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractGroundsForPossessionWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractMandatoryGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundCategory;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundEntity;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static uk.gov.hmcts.reform.pcs.ccd.domain.IntroductoryDemotedOrOtherGrounds.ABSOLUTE_GROUNDS;
import static uk.gov.hmcts.reform.pcs.ccd.domain.IntroductoryDemotedOrOtherGrounds.ANTI_SOCIAL;
import static uk.gov.hmcts.reform.pcs.ccd.domain.IntroductoryDemotedOrOtherGrounds.BREACH_OF_THE_TENANCY;
import static uk.gov.hmcts.reform.pcs.ccd.domain.IntroductoryDemotedOrOtherGrounds.OTHER;
import static uk.gov.hmcts.reform.pcs.ccd.domain.IntroductoryDemotedOrOtherGrounds.RENT_ARREARS;
import static uk.gov.hmcts.reform.pcs.ccd.domain.SecureOrFlexibleDiscretionaryGrounds.RENT_ARREARS_OR_BREACH_OF_TENANCY;
import static uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType.ASSURED_TENANCY;

@ExtendWith(MockitoExtension.class)
class ClaimGroundServiceTest {

    @InjectMocks
    private ClaimGroundService claimGroundService;

    @ParameterizedTest
    @EnumSource(value = TenancyLicenceType.class, names = {"INTRODUCTORY_TENANCY", "DEMOTED_TENANCY", "OTHER"})
    void shouldReturnClaimGroundEntities_WhenIntroDemotionOrOtherTenancy(TenancyLicenceType tenancyType) {
        // Given
        CaseDetails<PCSCase, State> caseDetails = new CaseDetails<>();

        Set<IntroductoryDemotedOrOtherGrounds> grounds = Set.of(
            RENT_ARREARS,
            ANTI_SOCIAL,
            BREACH_OF_THE_TENANCY,
            ABSOLUTE_GROUNDS,
            OTHER
        );

        IntroductoryDemotedOtherGroundsForPossession introductoryDemotedOtherGroundsForPossession =
            IntroductoryDemotedOtherGroundsForPossession.builder()
                .hasIntroductoryDemotedOtherGroundsForPossession(VerticalYesNo.YES)
                .introductoryDemotedOrOtherGrounds(grounds)
                .build();

        IntroductoryDemotedOtherGroundReason reasons = IntroductoryDemotedOtherGroundReason.builder()
            .antiSocialBehaviourGround("antisocial reason")
            .breachOfTheTenancyGround("breach of tenancy reason")
            .absoluteGrounds("absolute grounds reason")
            .otherGround("other ground reason")
            .noGrounds("should be ignored")
            .build();

        PCSCase caseData =
            PCSCase.builder()
                .introductoryDemotedOrOtherGroundsForPossession(introductoryDemotedOtherGroundsForPossession)
                .tenancyLicenceDetails(
                    TenancyLicenceDetails.builder()
                        .typeOfTenancyLicence(tenancyType)
                        .build()
                )
                .introductoryDemotedOtherGroundReason(reasons)
                .build();

        caseDetails.setData(caseData);

        // When
        List<ClaimGroundEntity> entities = claimGroundService.createClaimGroundEntities(caseData);

        // Then
        assertThat(entities)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.INTRODUCTORY_DEMOTED_OTHER)
                    .code("RENT_ARREARS")
                    .isRentArrears(true)
                    .build(),
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.INTRODUCTORY_DEMOTED_OTHER)
                    .code("ANTI_SOCIAL")
                    .reason("antisocial reason")
                    .isRentArrears(false)
                    .build(),
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.INTRODUCTORY_DEMOTED_OTHER)
                    .code("BREACH_OF_THE_TENANCY")
                    .reason("breach of tenancy reason")
                    .isRentArrears(false)
                    .build(),
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.INTRODUCTORY_DEMOTED_OTHER)
                    .code("ABSOLUTE_GROUNDS")
                    .reason("absolute grounds reason")
                    .isRentArrears(false)
                    .build(),
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.INTRODUCTORY_DEMOTED_OTHER)
                    .code("OTHER")
                    .reason("other ground reason")
                    .isRentArrears(false)
                    .build()
            );

    }

    @ParameterizedTest
    @EnumSource(value = TenancyLicenceType.class, names = {"INTRODUCTORY_TENANCY", "DEMOTED_TENANCY", "OTHER"})
    void shouldCreateNoGroundsEntity_WhenIntroDemotionOrOtherTenancy(TenancyLicenceType tenancyType) {
        // Given
        Set<IntroductoryDemotedOrOtherGrounds> groundsThatShouldBeIgnored = Set.of(
            RENT_ARREARS,
            ANTI_SOCIAL
        );

        IntroductoryDemotedOtherGroundsForPossession introductoryDemotedOtherGroundsForPossession =
            IntroductoryDemotedOtherGroundsForPossession.builder()
                .hasIntroductoryDemotedOtherGroundsForPossession(VerticalYesNo.YES)
                .introductoryDemotedOrOtherGrounds(groundsThatShouldBeIgnored)
                .hasIntroductoryDemotedOtherGroundsForPossession(VerticalYesNo.NO)
                .build();

        PCSCase caseData = PCSCase.builder()
            .tenancyLicenceDetails(
                TenancyLicenceDetails.builder()
                    .typeOfTenancyLicence(tenancyType)
                    .build()
            )
            .introductoryDemotedOrOtherGroundsForPossession(introductoryDemotedOtherGroundsForPossession)
            .introductoryDemotedOtherGroundReason(
                IntroductoryDemotedOtherGroundReason.builder()
                    .noGrounds("No grounds reason")
                    .build())
            .build();

        // When
        List<ClaimGroundEntity> entities = claimGroundService.createClaimGroundEntities(caseData);

        // Then
        assertThat(entities)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.INTRODUCTORY_DEMOTED_OTHER_NO_GROUNDS)
                    .code("NO_GROUNDS")
                    .reason("No grounds reason")
                    .isRentArrears(false)
                    .build()
            );
    }

    @Test
    void shouldReturnClaimGroundEntities_WhenAssuredTenancyNoRentArrears() {
        Answer<String> defaultReasonAnswer = invocation -> "Reason from " + invocation.getMethod().getName();
        NoRentArrearsReasonForGrounds reasons = mock(NoRentArrearsReasonForGrounds.class, defaultReasonAnswer);

        Set<AssuredMandatoryGround> mandatory = EnumSet.allOf(AssuredMandatoryGround.class);
        Set<AssuredDiscretionaryGround> discretionary = EnumSet.allOf(AssuredDiscretionaryGround.class);

        PCSCase caseData = PCSCase.builder()
            .claimDueToRentArrears(YesOrNo.NO)
            .noRentArrearsGroundsOptions(
                AssuredNoArrearsPossessionGrounds.builder()
                    .mandatoryGrounds(mandatory)
                    .discretionaryGrounds(discretionary)
                    .build()
            )
            .noRentArrearsReasonForGrounds(reasons)
            .tenancyLicenceDetails(
                TenancyLicenceDetails.builder()
                    .typeOfTenancyLicence(TenancyLicenceType.ASSURED_TENANCY)
                    .build()
            )
            .build();

        List<ClaimGroundEntity> entities = claimGroundService.createClaimGroundEntities(
            caseData
        );

        assertThat(entities)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.ASSURED_MANDATORY)
                    .code("OWNER_OCCUPIER_GROUND1")
                    .reason("Reason from getOwnerOccupier")
                    .isRentArrears(false)
                    .build(),
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.ASSURED_MANDATORY)
                    .code("REPOSSESSION_GROUND2")
                    .reason("Reason from getRepossessionByLender")
                    .isRentArrears(false)
                    .build(),
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.ASSURED_MANDATORY)
                    .code("HOLIDAY_LET_GROUND3")
                    .reason("Reason from getHolidayLet")
                    .isRentArrears(false)
                    .build(),
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.ASSURED_MANDATORY)
                    .code("STUDENT_LET_GROUND4")
                    .reason("Reason from getStudentLet")
                    .isRentArrears(false)
                    .build(),
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.ASSURED_MANDATORY)
                    .code("MINISTER_RELIGION_GROUND5")
                    .reason("Reason from getMinisterOfReligion")
                    .isRentArrears(false)
                    .build(),
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.ASSURED_MANDATORY)
                    .code("REDEVELOPMENT_GROUND6")
                    .reason("Reason from getRedevelopment")
                    .isRentArrears(false)
                    .build(),
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.ASSURED_MANDATORY)
                    .code("DEATH_OF_TENANT_GROUND7")
                    .reason("Reason from getDeathOfTenant")
                    .isRentArrears(false)
                    .build(),
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.ASSURED_MANDATORY)
                    .code("ANTISOCIAL_BEHAVIOUR_GROUND7A")
                    .reason("Reason from getAntisocialBehaviour")
                    .isRentArrears(false)
                    .build(),
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.ASSURED_MANDATORY)
                    .code("NO_RIGHT_TO_RENT_GROUND7B")
                    .reason("Reason from getNoRightToRent")
                    .isRentArrears(false)
                    .build(),
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.ASSURED_MANDATORY)
                    .code("SERIOUS_RENT_ARREARS_GROUND8")
                    .reason(null)
                    .isRentArrears(true)
                    .build(),
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.ASSURED_DISCRETIONARY)
                    .code("ALTERNATIVE_ACCOMMODATION_GROUND9")
                    .reason("Reason from getSuitableAlternativeAccomodation")
                    .isRentArrears(false)
                    .build(),
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.ASSURED_DISCRETIONARY)
                    .code("RENT_ARREARS_GROUND10")
                    .reason(null)
                    .isRentArrears(true)
                    .build(),
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.ASSURED_DISCRETIONARY)
                    .code("PERSISTENT_DELAY_GROUND11")
                    .reason(null)
                    .isRentArrears(true)
                    .build(),
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.ASSURED_DISCRETIONARY)
                    .code("BREACH_TENANCY_GROUND12")
                    .reason("Reason from getBreachOfTenancyConditions")
                    .isRentArrears(false)
                    .build(),
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.ASSURED_DISCRETIONARY)
                    .code("DETERIORATION_PROPERTY_GROUND13")
                    .reason("Reason from getPropertyDeterioration")
                    .isRentArrears(false)
                    .build(),
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.ASSURED_DISCRETIONARY)
                    .code("NUISANCE_ANNOYANCE_GROUND14")
                    .reason("Reason from getNuisanceOrIllegalUse")
                    .isRentArrears(false)
                    .build(),
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.ASSURED_DISCRETIONARY)
                    .code("DOMESTIC_VIOLENCE_GROUND14A")
                    .reason("Reason from getDomesticViolence")
                    .isRentArrears(false)
                    .build(),
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.ASSURED_DISCRETIONARY)
                    .code("OFFENCE_RIOT_GROUND14ZA")
                    .reason("Reason from getOffenceDuringRiot")
                    .isRentArrears(false)
                    .build(),
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.ASSURED_DISCRETIONARY)
                    .code("DETERIORATION_FURNITURE_GROUND15")
                    .reason("Reason from getFurnitureDeterioration")
                    .isRentArrears(false)
                    .build(),
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.ASSURED_DISCRETIONARY)
                    .code("EMPLOYEE_LANDLORD_GROUND16")
                    .reason("Reason from getLandlordEmployee")
                    .isRentArrears(false)
                    .build(),
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.ASSURED_DISCRETIONARY)
                    .code("FALSE_STATEMENT_GROUND17")
                    .reason("Reason from getFalseStatement")
                    .isRentArrears(false)
                    .build()
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
                AssuredNoArrearsPossessionGrounds.builder()
                    .mandatoryGrounds(null)
                    .discretionaryGrounds(null)
                    .build()
            )
            .build();

        // When
        List<ClaimGroundEntity> groundEntities = claimGroundService.createClaimGroundEntities(caseData);

        // Then
        assertThat(groundEntities).isEmpty();
    }

    @Test
    void shouldReturnClaimGroundEntities_WhenAssuredTenancyRentArrears() {
        // Given
        Set<AssuredRentArrearsGround> rentArrearsGrounds = Set.of(
            AssuredRentArrearsGround.RENT_ARREARS_GROUND10
        );

        Set<AssuredAdditionalMandatoryGrounds> mandatoryGrounds = Set.of(
            AssuredAdditionalMandatoryGrounds.OWNER_OCCUPIER_GROUND1,
            AssuredAdditionalMandatoryGrounds.REDEVELOPMENT_GROUND6
        );

        Set<AssuredAdditionalDiscretionaryGrounds> discretionaryGrounds = Set.of(
            AssuredAdditionalDiscretionaryGrounds.BREACH_TENANCY_GROUND12,
            AssuredAdditionalDiscretionaryGrounds.NUISANCE_ANNOYANCE_GROUND14
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
            .assuredRentArrearsPossessionGrounds(
                AssuredRentArrearsPossessionGrounds.builder()
                    .rentArrearsGrounds(rentArrearsGrounds)
                    .additionalMandatoryGrounds(mandatoryGrounds)
                    .additionalDiscretionaryGrounds(discretionaryGrounds)
                    .build()
            )
            .rentArrearsGroundsReasons(reasons)
            .build();

        // When
        List<ClaimGroundEntity> result = claimGroundService.createClaimGroundEntities(caseData);

        // Then
        assertThat(result)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.ASSURED_DISCRETIONARY)
                    .code("RENT_ARREARS_GROUND10")
                    .isRentArrears(true)
                    .build(),
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.ASSURED_MANDATORY)
                    .code("OWNER_OCCUPIER_GROUND1")
                    .reason("Owner occupier needs the property")
                    .isRentArrears(false)
                    .build(),
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.ASSURED_MANDATORY)
                    .code("REDEVELOPMENT_GROUND6")
                    .reason("Redevelopment planned")
                    .isRentArrears(false)
                    .build(),
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.ASSURED_DISCRETIONARY)
                    .code("BREACH_TENANCY_GROUND12")
                    .reason("Tenant breached agreement")
                    .isRentArrears(false)
                    .build(),
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.ASSURED_DISCRETIONARY)
                    .code("NUISANCE_ANNOYANCE_GROUND14")
                    .reason("Tenant caused nuisance")
                    .isRentArrears(false)
                    .build()
            );
    }

    @Test
    void shouldSaveOnlyRentArrearsGrounds_WhenAssuredTenancyRentArrears() {
        // Given
        Set<AssuredRentArrearsGround> rentArrearsGrounds = Set.of(
            AssuredRentArrearsGround.SERIOUS_RENT_ARREARS_GROUND8,
            AssuredRentArrearsGround.RENT_ARREARS_GROUND10,
            AssuredRentArrearsGround.PERSISTENT_DELAY_GROUND11
        );

        PCSCase caseData = PCSCase.builder()
            .tenancyLicenceDetails(
                TenancyLicenceDetails.builder()
                    .typeOfTenancyLicence(TenancyLicenceType.ASSURED_TENANCY)
                    .build()
            )
            .claimDueToRentArrears(YesOrNo.YES)
            .assuredRentArrearsPossessionGrounds(
                AssuredRentArrearsPossessionGrounds.builder()
                    .rentArrearsGrounds(rentArrearsGrounds)
                    .additionalMandatoryGrounds(Set.of())
                    .additionalDiscretionaryGrounds(Set.of())
                    .build()
            )
            .build();

        // When
        List<ClaimGroundEntity> result = claimGroundService.createClaimGroundEntities(caseData);

        // Then
        assertThat(result)
            .extracting(ClaimGroundEntity::getCode)
            .containsExactlyInAnyOrder(
                AssuredRentArrearsGround.SERIOUS_RENT_ARREARS_GROUND8.name(),
                AssuredRentArrearsGround.RENT_ARREARS_GROUND10.name(),
                AssuredRentArrearsGround.PERSISTENT_DELAY_GROUND11.name()
            );

    }

    @ParameterizedTest
    @EnumSource(value = TenancyLicenceType.class, names = {"SECURE_TENANCY", "FLEXIBLE_TENANCY"})
    void shouldReturnClaimGroundEntities_WhenSecureOrFlexibleTenancy(TenancyLicenceType tenancyType) {
        // Given
        Set<SecureOrFlexibleDiscretionaryGrounds> discretionaryGrounds
            = EnumSet.allOf(SecureOrFlexibleDiscretionaryGrounds.class);
        Set<SecureOrFlexibleMandatoryGrounds> mandatoryGrounds
            = EnumSet.allOf(SecureOrFlexibleMandatoryGrounds.class);
        Set<SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm> discretionaryGroundsAlt
            = EnumSet.allOf(SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm.class);
        Set<SecureOrFlexibleMandatoryGroundsAlternativeAccomm> mandatoryGroundsAlt
            = EnumSet.allOf(SecureOrFlexibleMandatoryGroundsAlternativeAccomm.class);

        Answer<String> defaultReasonAnswer = invocation -> "Reason from " + invocation.getMethod().getName();
        SecureOrFlexibleGroundsReasons reasons = mock(SecureOrFlexibleGroundsReasons.class, defaultReasonAnswer);

        PCSCase caseData = PCSCase.builder()
            .tenancyLicenceDetails(TenancyLicenceDetails.builder().typeOfTenancyLicence(tenancyType).build())
            .secureOrFlexiblePossessionGrounds(
                SecureOrFlexiblePossessionGrounds.builder()
                    .secureOrFlexibleDiscretionaryGrounds(discretionaryGrounds)
                    .secureOrFlexibleMandatoryGrounds(mandatoryGrounds)
                    .secureOrFlexibleDiscretionaryGroundsAlt(discretionaryGroundsAlt)
                    .secureOrFlexibleMandatoryGroundsAlt(mandatoryGroundsAlt)
                    .build()
            )
            .secureOrFlexibleGroundsReasons(reasons)
            .rentArrearsOrBreachOfTenancy(Set.of(RentArrearsOrBreachOfTenancy.BREACH_OF_TENANCY))
            .build();

        // When
        List<ClaimGroundEntity> result = claimGroundService.createClaimGroundEntities(caseData);

        // Then
        assertThat(result)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.SECURE_OR_FLEXIBLE_DISCRETIONARY)
                    .code("RENT_ARREARS_OR_BREACH_OF_TENANCY")
                    .reason("Reason from getBreachOfTenancyGround")
                    .isRentArrears(false)
                    .build(),
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.SECURE_OR_FLEXIBLE_DISCRETIONARY)
                    .code("NUISANCE_OR_IMMORAL_USE")
                    .reason("Reason from getNuisanceOrImmoralUseGround")
                    .isRentArrears(false)
                    .build(),
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.SECURE_OR_FLEXIBLE_DISCRETIONARY)
                    .code("DOMESTIC_VIOLENCE")
                    .reason("Reason from getDomesticViolenceGround")
                    .isRentArrears(false)
                    .build(),
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.SECURE_OR_FLEXIBLE_DISCRETIONARY)
                    .code("RIOT_OFFENCE")
                    .reason("Reason from getRiotOffenceGround")
                    .isRentArrears(false)
                    .build(),
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.SECURE_OR_FLEXIBLE_DISCRETIONARY)
                    .code("PROPERTY_DETERIORATION")
                    .reason("Reason from getPropertyDeteriorationGround")
                    .isRentArrears(false)
                    .build(),
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.SECURE_OR_FLEXIBLE_DISCRETIONARY)
                    .code("FURNITURE_DETERIORATION")
                    .reason("Reason from getFurnitureDeteriorationGround")
                    .isRentArrears(false)
                    .build(),
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.SECURE_OR_FLEXIBLE_DISCRETIONARY)
                    .code("TENANCY_OBTAINED_BY_FALSE_STATEMENT")
                    .reason("Reason from getTenancyByFalseStatementGround")
                    .isRentArrears(false)
                    .build(),
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.SECURE_OR_FLEXIBLE_DISCRETIONARY)
                    .code("PREMIUM_PAID_MUTUAL_EXCHANGE")
                    .reason("Reason from getPremiumMutualExchangeGround")
                    .isRentArrears(false)
                    .build(),
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.SECURE_OR_FLEXIBLE_DISCRETIONARY)
                    .code("UNREASONABLE_CONDUCT_TIED_ACCOMMODATION")
                    .reason("Reason from getUnreasonableConductGround")
                    .isRentArrears(false)
                    .build(),
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.SECURE_OR_FLEXIBLE_DISCRETIONARY)
                    .code("REFUSAL_TO_MOVE_BACK")
                    .reason("Reason from getRefusalToMoveBackGround")
                    .isRentArrears(false)
                    .build(),
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.SECURE_OR_FLEXIBLE_MANDATORY)
                    .code("ANTI_SOCIAL")
                    .reason("Reason from getAntiSocialGround")
                    .isRentArrears(false)
                    .build(),
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.SECURE_OR_FLEXIBLE_DISCRETIONARY_ALT)
                    .code("TIED_ACCOMMODATION_NEEDED_FOR_EMPLOYEE")
                    .reason("Reason from getTiedAccommodationGround")
                    .isRentArrears(false)
                    .build(),
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.SECURE_OR_FLEXIBLE_DISCRETIONARY_ALT)
                    .code("ADAPTED_ACCOMMODATION")
                    .reason("Reason from getAdaptedAccommodationGround")
                    .isRentArrears(false)
                    .build(),
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.SECURE_OR_FLEXIBLE_DISCRETIONARY_ALT)
                    .code("HOUSING_ASSOCIATION_SPECIAL_CIRCUMSTANCES")
                    .reason("Reason from getHousingAssocSpecialGround")
                    .isRentArrears(false)
                    .build(),
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.SECURE_OR_FLEXIBLE_DISCRETIONARY_ALT)
                    .code("SPECIAL_NEEDS_ACCOMMODATION")
                    .reason("Reason from getSpecialNeedsAccommodationGround")
                    .isRentArrears(false)
                    .build(),
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.SECURE_OR_FLEXIBLE_DISCRETIONARY_ALT)
                    .code("UNDER_OCCUPYING_AFTER_SUCCESSION")
                    .reason("Reason from getUnderOccupancySuccessionGround")
                    .isRentArrears(false)
                    .build(),
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.SECURE_OR_FLEXIBLE_MANDATORY_ALT)
                    .code("OVERCROWDING")
                    .reason("Reason from getOvercrowdingGround")
                    .isRentArrears(false)
                    .build(),
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.SECURE_OR_FLEXIBLE_MANDATORY_ALT)
                    .code("LANDLORD_WORKS")
                    .reason("Reason from getLandlordWorksGround")
                    .isRentArrears(false)
                    .build(),
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.SECURE_OR_FLEXIBLE_MANDATORY_ALT)
                    .code("PROPERTY_SOLD")
                    .reason("Reason from getPropertySoldGround")
                    .isRentArrears(false)
                    .build(),
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.SECURE_OR_FLEXIBLE_MANDATORY_ALT)
                    .code("CHARITABLE_LANDLORD")
                    .reason("Reason from getCharitableLandlordGround")
                    .isRentArrears(false)
                    .build()
            );
    }

    @ParameterizedTest
    @EnumSource(value = TenancyLicenceType.class, names = {"SECURE_TENANCY", "FLEXIBLE_TENANCY"})
    void shouldSetRentArrearsFlagAndNoReasonOnGround_WhenSecureOrFlexibleTenancy(TenancyLicenceType tenancyType) {
        // Given
        Set<SecureOrFlexibleDiscretionaryGrounds> discretionaryGrounds = Set.of(RENT_ARREARS_OR_BREACH_OF_TENANCY);

        Answer<String> defaultReasonAnswer = invocation -> "Reason from " + invocation.getMethod().getName();
        SecureOrFlexibleGroundsReasons reasons = mock(SecureOrFlexibleGroundsReasons.class, defaultReasonAnswer);

        PCSCase caseData = PCSCase.builder()
            .tenancyLicenceDetails(TenancyLicenceDetails.builder().typeOfTenancyLicence(tenancyType).build())
            .secureOrFlexiblePossessionGrounds(
                SecureOrFlexiblePossessionGrounds.builder()
                    .secureOrFlexibleDiscretionaryGrounds(discretionaryGrounds)
                    .secureOrFlexibleMandatoryGrounds(Set.of())
                    .secureOrFlexibleDiscretionaryGroundsAlt(Set.of())
                    .secureOrFlexibleMandatoryGroundsAlt(Set.of())
                    .build()
            )
            .secureOrFlexibleGroundsReasons(reasons)
            .rentArrearsOrBreachOfTenancy(Set.of(RentArrearsOrBreachOfTenancy.RENT_ARREARS))
            .build();

        // When
        List<ClaimGroundEntity> result = claimGroundService.createClaimGroundEntities(caseData);

        // Then
        assertThat(result)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactly(
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.SECURE_OR_FLEXIBLE_DISCRETIONARY)
                    .code("RENT_ARREARS_OR_BREACH_OF_TENANCY")
                    .reason(null)
                    .isRentArrears(true)
                    .build()
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
            DiscretionaryGroundWales.OTHER_BREACH_S157
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
            SecureContractMandatoryGroundsWales.FAILURE_TO_GIVE_UP_POSSESSION_S170,
            SecureContractMandatoryGroundsWales.LANDLORD_NOTICE_S186,
            SecureContractMandatoryGroundsWales.FAILURE_TO_GIVE_UP_POSSESSION_S191,
            SecureContractMandatoryGroundsWales.LANDLORD_NOTICE_S199
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
                .mandatoryGrounds(mandatoryGrounds)
                .discretionaryGrounds(discretionaryGrounds)
                .estateManagementGrounds(estateGrounds)
                .build())
            .secureContractGroundsForPossessionWales(
                SecureContractGroundsForPossessionWales.builder()
                    .mandatoryGrounds(secureMandatoryGrounds)
                    .discretionaryGrounds(secureDiscretionaryGrounds)
                    .build()
            )

            .groundsReasonsWales(reasons)
            .build();

        // When
        List<ClaimGroundEntity> result = claimGroundService.createClaimGroundEntities(caseData);

        // Then
        int expectedSize = mandatoryGrounds.size() + discretionaryGrounds.size() + estateGrounds.size()
            + secureMandatoryGrounds.size() + secureDiscretionaryGrounds.size();
        assertThat(result).hasSize(expectedSize);

        Map<String, String> groundAndReason = result.stream()
            .collect(Collectors.toMap(ClaimGroundEntity::getCode, ClaimGroundEntity::getReason));

        assertThat(groundAndReason)
            .containsEntry("FAIL_TO_GIVE_UP_S170", "Failure to give up S170")
            .containsEntry("LANDLORD_NOTICE_PERIODIC_S178", "Landlord notice periodic S178")
            .containsEntry("SERIOUS_ARREARS_PERIODIC_S181", "Serious arrears periodic S181")
            .containsEntry("LANDLORD_NOTICE_FT_END_S186", "Landlord notice FT end S186")
            .containsEntry("SERIOUS_ARREARS_FIXED_TERM_S187", "Serious arrears fixed term S187")
            .containsEntry("FAIL_TO_GIVE_UP_BREAK_NOTICE_S191", "Fail to give up break notice S191")
            .containsEntry("LANDLORD_BREAK_CLAUSE_S199", "Landlord break clause S199")
            .containsEntry("CONVERTED_FIXED_TERM_SCH12_25B2", "Converted fixed term Sch12 25B2")
            .containsEntry("OTHER_BREACH_S157", "Other breach section 157")
            .containsEntry("BUILDING_WORKS", "Building works")
            .containsEntry("REDEVELOPMENT_SCHEMES", "Redevelopment schemes")
            .containsEntry("CHARITIES", "Charities")
            .containsEntry("DISABLED_SUITABLE_DWELLING", "Disabled suitable dwelling")
            .containsEntry("HOUSING_ASSOCIATIONS_AND_TRUSTS", "Housing associations and trusts")
            .containsEntry("SPECIAL_NEEDS_DWELLINGS", "Special needs dwellings")
            .containsEntry("RESERVE_SUCCESSORS", "Reserve successors")
            .containsEntry("JOINT_CONTRACT_HOLDERS", "Joint contract holders")
            .containsEntry("OTHER_ESTATE_MANAGEMENT_REASONS", "Other estate management reasons")
            .containsEntry("FAILURE_TO_GIVE_UP_POSSESSION_S170", "Secure failure S170")
            .containsEntry("LANDLORD_NOTICE_S186", "Secure landlord notice S186")
            .containsEntry("FAILURE_TO_GIVE_UP_POSSESSION_S191", "Secure failure S191")
            .containsEntry("LANDLORD_NOTICE_S199", "Secure landlord notice S199")
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
                .mandatoryGrounds(mandatoryGrounds)
                .build())
            .groundsReasonsWales(reasons)
            .build();

        // When
        List<ClaimGroundEntity> result = claimGroundService.createClaimGroundEntities(caseData);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getCode()).isEqualTo("FAIL_TO_GIVE_UP_S170");
        assertThat(result.getFirst().getReason()).isEqualTo("Test reason");
    }
}
