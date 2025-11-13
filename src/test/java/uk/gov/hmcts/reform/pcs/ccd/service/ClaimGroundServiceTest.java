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
import uk.gov.hmcts.reform.pcs.ccd.domain.IntroductoryDemotedOtherGroundReason;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoRentArrearsDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoRentArrearsMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsGroundsReasons;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.model.NoRentArrearsReasonForGrounds;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundEntity;

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

        PCSCase caseData =
            PCSCase.builder()
                .hasIntroductoryDemotedOtherGroundsForPossession(VerticalYesNo.YES)
                .typeOfTenancyLicence(TenancyLicenceType.INTRODUCTORY_TENANCY)
                .introductoryDemotedOrOtherGrounds(grounds)
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

        PCSCase caseData =
            PCSCase.builder()
                .hasIntroductoryDemotedOtherGroundsForPossession(VerticalYesNo.YES)
                .typeOfTenancyLicence(TenancyLicenceType.INTRODUCTORY_TENANCY)
                .introductoryDemotedOrOtherGrounds(grounds)
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
        PCSCase caseData = PCSCase.builder()
            .hasIntroductoryDemotedOtherGroundsForPossession(VerticalYesNo.YES)
            .typeOfTenancyLicence(TenancyLicenceType.INTRODUCTORY_TENANCY)
            .introductoryDemotedOrOtherGrounds(null)
            .introductoryDemotedOtherGroundReason(
                IntroductoryDemotedOtherGroundReason.builder()
                    .noGrounds("No ground reason")
                    .build())
            .hasIntroductoryDemotedOtherGroundsForPossession(VerticalYesNo.NO)
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

        Set<NoRentArrearsMandatoryGrounds> mandatory = EnumSet.allOf(NoRentArrearsMandatoryGrounds.class);
        Set<NoRentArrearsDiscretionaryGrounds> discretionary = EnumSet.allOf(NoRentArrearsDiscretionaryGrounds.class);

        PCSCase caseData = PCSCase.builder()
            .noRentArrearsDiscretionaryGroundsOptions(discretionary)
            .noRentArrearsMandatoryGroundsOptions(mandatory)
            .noRentArrearsReasonForGrounds(grounds)
            .typeOfTenancyLicence(TenancyLicenceType.ASSURED_TENANCY)
            .build();

        List<ClaimGroundEntity> entities = claimGroundService.getGroundsWithReason(
            caseData
        );

        // Check size
        assertThat(entities).hasSize(mandatory.size() + discretionary.size());

        // Expected pairs: ground ID -> reason
        Map<String, String> expectedReasons = Map.ofEntries(
            entry("OWNER_OCCUPIER", "Owner occupier reason"),
            entry("REPOSSESSION_BY_LENDER", "Repossession reason"),
            entry("HOLIDAY_LET", "Holiday let reason"),
            entry("STUDENT_LET", "Student let reason"),
            entry("MINISTER_OF_RELIGION", "Minister of religion reason"),
            entry("REDEVELOPMENT", "Redevelopment reason"),
            entry("DEATH_OF_TENANT", "Death of tenant reason"),
            entry("ANTISOCIAL_BEHAVIOUR", "Antisocial behaviour reason"),
            entry("NO_RIGHT_TO_RENT", "No right to rent reason"),
            entry("SUITABLE_ACCOM", "Suitable alternative accommodation reason"),
            entry("BREACH_OF_TENANCY_CONDITIONS", "Breach of tenancy conditions reason"),
            entry("PROPERTY_DETERIORATION", "Property deterioration reason"),
            entry("NUISANCE_OR_ILLEGAL_USE", "Nuisance reason"),
            entry("DOMESTIC_VIOLENCE", "Domestic violence reason"),
            entry("OFFENCE_DURING_RIOT", "Offence during riot reason"),
            entry("FURNITURE_DETERIORATION", "Furniture deterioration reason"),
            entry("LANDLORD_EMPLOYEE", "Landlord employee reason"),
            entry("FALSE_STATEMENT", "False statement reason")
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
            .typeOfTenancyLicence(TenancyLicenceType.ASSURED_TENANCY)
            .build();

        // When
        List<ClaimGroundEntity> groundEntities = claimGroundService.getGroundsWithReason(caseData);

        // Then
        assertThat(groundEntities).isEmpty();
    }

    @Test
    void shouldReturnClaimGroundEntities_WhenAssuredTenancyRentArrears() {
        // Given
        Set<RentArrearsMandatoryGrounds> mandatoryGrounds = Set.of(
            RentArrearsMandatoryGrounds.OWNER_OCCUPIER_GROUND1,
            RentArrearsMandatoryGrounds.REDEVELOPMENT_GROUND6
        );

        Set<RentArrearsDiscretionaryGrounds> discretionaryGrounds = Set.of(
            RentArrearsDiscretionaryGrounds.BREACH_TENANCY_GROUND12,
            RentArrearsDiscretionaryGrounds.NUISANCE_ANNOYANCE_GROUND14
        );

        RentArrearsGroundsReasons reasons = RentArrearsGroundsReasons.builder()
            .ownerOccupierReason("Owner occupier needs the property")
            .redevelopmentReason("Redevelopment planned")
            .breachOfTenancyConditionsReason("Tenant breached agreement")
            .nuisanceAnnoyanceReason("Tenant caused nuisance")
            .build();

        PCSCase caseData = PCSCase.builder()
            .typeOfTenancyLicence(TenancyLicenceType.ASSURED_TENANCY)
            .claimDueToRentArrears(YesOrNo.YES)
            .rentArrearsMandatoryGrounds(mandatoryGrounds)
            .rentArrearsDiscretionaryGrounds(discretionaryGrounds)
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
            .typeOfTenancyLicence(TenancyLicenceType.ASSURED_TENANCY)
            .claimDueToRentArrears(YesOrNo.YES)
            .rentArrearsGrounds(rentArrearsGrounds)
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
}
