package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalReasons;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantContactPreferences;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.domain.DemotionOfTenancy;
import uk.gov.hmcts.reform.pcs.ccd.domain.DemotionOfTenancyHousingAct;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServedDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsSection;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentPaymentFrequency;
import uk.gov.hmcts.reform.pcs.ccd.domain.SuspensionOfRightToBuy;
import uk.gov.hmcts.reform.pcs.ccd.domain.SuspensionOfRightToBuyHousingAct;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.WalesNoticeDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredAdditionalOtherGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.ClaimGroundSummary;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.IntroductoryDemotedOrOtherGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details.CaseDetailsTab;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared.AdditionalDefendantInformationTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared.ClaimantInformationTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared.DefendantInformationTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared.ReasonsForPossessionTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared.RentArrearsTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.ASBQuestionsDetailsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceDetailsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceTypeWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.PeriodicContractTermsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.WalesDocuments;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.ccd.view.builder.AdditionalDefendantInformationTabDetailsBuilder;
import uk.gov.hmcts.reform.pcs.ccd.view.builder.ClaimantInformationTabDetailsBuilder;
import uk.gov.hmcts.reform.pcs.ccd.view.builder.DefendantInformationTabDetailsBuilder;
import uk.gov.hmcts.reform.pcs.ccd.view.builder.GroundsBuilder;
import uk.gov.hmcts.reform.pcs.ccd.view.builder.ReasonsForPossessionTabDetailsBuilder;
import uk.gov.hmcts.reform.pcs.ccd.view.builder.RequiredDocumentsTabDetailsBuilder;
import uk.gov.hmcts.reform.pcs.ccd.view.builder.RentArrearsTabDetailsBuilder;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.domain.AlternativesToPossession.DEMOTION_OF_TENANCY;
import static uk.gov.hmcts.reform.pcs.ccd.domain.AlternativesToPossession.SUSPENSION_OF_RIGHT_TO_BUY;
import static uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType.COMMUNITY_LANDLORD;
import static uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType.PROVIDER_OF_SOCIAL_HOUSING;

@ExtendWith(MockitoExtension.class)
public class CaseDetailsTabViewTest {

    private final String noAnswer = " ";

    @Mock
    private GroundsBuilder groundsBuilder;

    @Mock
    private RentArrearsTabDetailsBuilder rentArrearsTabDetailsBuilder;

    @Mock
    private ReasonsForPossessionTabDetailsBuilder reasonsForPossessionTabDetailsBuilder;

    @Mock
    private ClaimantInformationTabDetailsBuilder claimantInformationTabDetailsBuilder;

    @Mock
    private DefendantInformationTabDetailsBuilder defendantInformationTabDetailsBuilder;

    @Mock
    private AdditionalDefendantInformationTabDetailsBuilder additionalDefendantInformationTabDetailsBuilder;

    @Spy
    private RequiredDocumentsTabDetailsBuilder requiredDocumentsTabDetailsBuilder;

    @InjectMocks
    private CaseDetailsTabView caseDetailsTabView;

    @Test
    void shouldSetCaseDetailsTabFieldsForEngland() {
        AddressUK propertyAddress = AddressUK.builder().postCode("SW1A 1AA").build();
        AddressUK defendantAddress = AddressUK.builder().postCode("E1 1AA").build();
        AddressUK underlesseeAddress = AddressUK.builder().postCode("CV1 1DF").build();
        AddressUK claimantAddress = AddressUK.builder().postCode("L2 3RT").build();
        PCSCase pcsCase = PCSCase.builder()
            .legislativeCountry(LegislativeCountry.ENGLAND)
            .claimantType(
                DynamicStringList.builder().value(
                    DynamicStringListElement.builder().code(PROVIDER_OF_SOCIAL_HOUSING.name()).build())
                    .build())
            .claimAgainstTrespassers(VerticalYesNo.YES)
            .propertyAddress(propertyAddress)
            .dateSubmitted(LocalDateTime.of(2026, 1, 11, 17, 2, 31))
            .claimGroundSummaries(List.of(
                listValue(ClaimGroundSummary.builder()
                              .label("Rent arrears (ground 10)")
                              .reason("Ground 10 reason")
                              .code("ABSOLUTE_GROUNDS")
                              .build()),
                listValue(ClaimGroundSummary.builder()
                              .label("Condition 1 of Section 84A of the Housing Act 1985")
                              .reason("Condition 1 reason")
                              .code("ABSOLUTE_GROUNDS")
                              .build())
            ))
            .additionalReasonsForPossession(AdditionalReasons.builder()
                                                .hasReasons(VerticalYesNo.YES)
                                                .reasons("Additional reasons")
                                                .build())
            .allClaimants(
                List.of(listValue(
                    Party.builder()
                        .orgName("Claimant")
                        .address(claimantAddress)
                        .emailAddress("claimant@email.com")
                        .phoneNumberProvided(VerticalYesNo.YES)
                        .phoneNumber("phone number")
                        .build()
                ))
            )
            .claimantCircumstances(
                ClaimantCircumstances.builder()
                    .claimantCircumstancesSelect(VerticalYesNo.YES)
                    .claimantCircumstancesDetails("claimant circumstances")
                    .build()
            )
            .allDefendants(List.of(
                listValue(Party.builder()
                              .nameKnown(VerticalYesNo.YES)
                              .firstName("Defendant")
                              .lastName("One")
                              .addressKnown(VerticalYesNo.YES)
                              .build()),
                listValue(Party.builder()
                              .nameKnown(VerticalYesNo.YES)
                              .firstName("Defendant")
                              .lastName("Two")
                              .addressKnown(VerticalYesNo.YES)
                              .address(defendantAddress)
                              .build()),
                listValue(Party.builder()
                              .nameKnown(VerticalYesNo.NO)
                              .addressKnown(VerticalYesNo.NO)
                              .build())
            ))
            .defendantCircumstances(
                DefendantCircumstances.builder()
                    .hasDefendantCircumstancesInfo(VerticalYesNo.YES)
                    .defendantCircumstancesInfo("defendant circumstances")
                    .build()
            )
            .rentDetails(RentDetails.builder()
                             .currentRent(new BigDecimal("100.00"))
                             .frequency(RentPaymentFrequency.OTHER)
                             .otherFrequency("Every 4 weeks")
                             .perDayCorrect(VerticalYesNo.NO)
                             .amendedDailyCharge(new BigDecimal("12.30"))
                             .build())
            .rentArrears(RentArrearsSection.builder()
                             .total(new BigDecimal("450.75"))
                             .build())
            .arrearsJudgmentWanted(VerticalYesNo.YES)
            .tenancyLicenceDetails(TenancyLicenceDetails.builder()
                                       .typeOfTenancyLicence(TenancyLicenceType.OTHER)
                                       .tenancyLicenceDate(LocalDate.of(2024, 4, 16))
                                       .hasCopyOfTenancyLicence(VerticalYesNo.NO)
                                       .reasonsForNoTenancyLicenceDocuments("Reasons")
                                       .build())
            .noticeServed(YesOrNo.YES)
            .noticeServedDetails(NoticeServedDetails.builder()
                                     .noticeServiceMethod(NoticeServiceMethod.EMAIL)
                                     .noticeEmailSentDateTime(LocalDateTime.of(2026, 5, 11, 17, 2))
                                     .build())
            .preActionProtocolCompleted(VerticalYesNo.NO)
            .preActionProtocolIncompleteExplanation("preaction explanation")
            .mediationAttempted(VerticalYesNo.YES)
            .settlementAttempted(VerticalYesNo.YES)
            .applicationWithClaim(VerticalYesNo.YES)
            .allUnderlesseeOrMortgagees(List.of(
                listValue(
                    Party.builder()
                        .nameKnown(VerticalYesNo.YES)
                        .orgName("underlessee name")
                        .addressKnown(VerticalYesNo.YES)
                        .address(underlesseeAddress)
                        .build()
                ),
                listValue(
                    Party.builder()
                        .nameKnown(VerticalYesNo.NO)
                        .addressKnown(VerticalYesNo.NO)
                        .build()
                )
            ))
            .alternativesToPossession(Set.of(DEMOTION_OF_TENANCY, SUSPENSION_OF_RIGHT_TO_BUY))
            .demotionOfTenancy(
                DemotionOfTenancy.builder()
                    .housingAct(DemotionOfTenancyHousingAct.SECTION_6A_2)
                    .statementOfExpressTermsServed(VerticalYesNo.YES)
                    .statementOfExpressTermsDetails("terms")
                    .reason("demotion reason")
                    .build()
            )
            .suspensionOfRightToBuy(
                SuspensionOfRightToBuy.builder()
                    .housingAct(SuspensionOfRightToBuyHousingAct.SECTION_6A_2)
                    .reason("suspension reason")
                    .build()
            )
            .build();

        when(groundsBuilder.getGrounds(pcsCase)).thenReturn(
            "Rent arrears (ground 10)\n"
                + "Antisocial behaviour: Condition 1 of Section 84A of the Housing Act 1985"
        );

        when(rentArrearsTabDetailsBuilder.buildDetailedRentArrearsTabDetails(pcsCase)).thenReturn(
            RentArrearsTabDetails.builder()
                .rentAmount("£100")
                .calculationFrequency("Every 4 weeks")
                .dailyRate("£12.30")
                .arrearsTotal("£450.75")
                .judgmentRequested("Yes")
                .build()
        );

        when(reasonsForPossessionTabDetailsBuilder.buildDetailsReasonsForPossession(pcsCase)).thenReturn(
            ReasonsForPossessionTabDetails.builder()
                .ground10("Ground 10 reason")
                .condition1OfSection84A("Condition 1 reason")
                .additionalReasonsForPossession("Additional reasons")
                .build()
        );

        when(claimantInformationTabDetailsBuilder.createSummaryClaimantTabDetails(pcsCase)).thenReturn(
            ClaimantInformationTabDetails.builder()
                .claimantName("Claimant")
                .build()
        );

        when(defendantInformationTabDetailsBuilder.buildDetailedDefendantDetails(pcsCase)).thenReturn(
            DefendantInformationTabDetails.builder()
                .nameKnown("Yes")
                .firstName("Defendant")
                .lastName("One")
                .addressKnown("Yes")
                .addressForService(propertyAddress)
                .build()
        );

        when(additionalDefendantInformationTabDetailsBuilder.buildDetailedAdditionalDefendantsDetails(pcsCase))
            .thenReturn(
                List.of(
                    listValue(
                        AdditionalDefendantInformationTabDetails.builder()
                            .nameKnown("Yes")
                            .firstName("Defendant")
                            .lastName("Two")
                            .addressKnown("Yes")
                            .addressForService(defendantAddress)
                            .build()
                    ),
                    listValue(
                        AdditionalDefendantInformationTabDetails.builder()
                            .nameKnown("No")
                            .addressKnown("No")
                            .build()
                    )
                )
            );

        // When
        CaseDetailsTab caseDetailsTab = caseDetailsTabView.buildCaseDetailsTab(pcsCase);

        assertThat(caseDetailsTab.getPropertyAddress()).isEqualTo(propertyAddress);
        assertThat(caseDetailsTab.getGroundsForPossessionDetails().getGrounds())
            .isEqualTo(
                "Rent arrears (ground 10)\n"
                    + "Antisocial behaviour: Condition 1 of Section 84A of the Housing Act 1985"
            );
        assertThat(caseDetailsTab.getReasonsForPossessionDetails().getGround10())
            .isEqualTo("Ground 10 reason");
        assertThat(caseDetailsTab.getReasonsForPossessionDetails().getCondition1OfSection84A())
            .isEqualTo("Condition 1 reason");
        assertThat(caseDetailsTab.getReasonsForPossessionDetails().getAdditionalReasonsForPossession())
            .isEqualTo("Additional reasons");
        assertThat(caseDetailsTab.getDateClaimSubmitted()).isEqualTo("11 January 2026, 5:02:31PM");
        assertThat(caseDetailsTab.getClaimantInformation().getClaimantName()).isEqualTo("Claimant");
        assertThat(caseDetailsTab.getDefendantInformationDetails().getFirstName()).isEqualTo("Defendant");
        assertThat(caseDetailsTab.getDefendantInformationDetails().getLastName()).isEqualTo("One");
        assertThat(caseDetailsTab.getDefendantInformationDetails().getAddressForService()).isEqualTo(propertyAddress);
        assertThat(caseDetailsTab.getAdditionalDefendants()).hasSize(2);
        assertThat(caseDetailsTab.getAdditionalDefendants().getFirst().getValue().getFirstName())
            .isEqualTo("Defendant");
        assertThat(caseDetailsTab.getAdditionalDefendants().getFirst().getValue().getLastName()).isEqualTo("Two");
        assertThat(caseDetailsTab.getAdditionalDefendants().getFirst().getValue().getAddressForService())
            .isEqualTo(defendantAddress);
        assertThat(caseDetailsTab.getAdditionalDefendants().get(1).getValue().getNameKnown())
            .isEqualTo("No");
        assertThat(caseDetailsTab.getAdditionalDefendants().get(1).getValue().getAddressKnown())
            .isEqualTo("No");
        assertThat(caseDetailsTab.getRentArrearsDetails().getRentAmount()).isEqualTo("£100");
        assertThat(caseDetailsTab.getRentArrearsDetails().getCalculationFrequency()).isEqualTo("Every 4 weeks");
        assertThat(caseDetailsTab.getRentArrearsDetails().getDailyRate()).isEqualTo("£12.30");
        assertThat(caseDetailsTab.getRentArrearsDetails().getArrearsTotal()).isEqualTo("£450.75");
        assertThat(caseDetailsTab.getRentArrearsDetails().getJudgmentRequested()).isEqualTo("Yes");
        assertThat(caseDetailsTab.getTenancyLicenceDetails().getTypeOfTenancyLicence())
            .isEqualTo("Other");
        assertThat(caseDetailsTab.getTenancyLicenceDetails().getTenancyLicenceDate())
            .isEqualTo("16 April 2024");
        assertThat(caseDetailsTab.getNoticeDetails().getNoticeDate())
            .isEqualTo("11 May 2026, 5:02:00PM");
        assertThat(caseDetailsTab.getTenancyLicenceDetails().getHasCopyOfTenancyLicence())
            .isEqualTo("No");
        assertThat(caseDetailsTab.getTenancyLicenceDetails().getReasonsForNoTenancyLicenceDocuments())
            .isEqualTo("Reasons");
        assertThat(caseDetailsTab.getNoticeDetails().getNoticeMethod()).isEqualTo("By email");
        assertThat(caseDetailsTab.getNoticeDetails().getNoticeServed()).isEqualTo("Yes");
        assertThat(caseDetailsTab.getNoticeDetails().getNoticeDate()).isEqualTo("11 May 2026, 5:02:00PM");
        assertThat(caseDetailsTab.getApplicationsDetails().getPlanToMakeGeneralApplication()).isEqualTo("Yes");
        assertThat(caseDetailsTab.getActionsTakenDetails().getPreactionProtocolFollowed()).isEqualTo("No");
        assertThat(caseDetailsTab.getActionsTakenDetails().getPreActionProtocolIncompleteExplanation())
            .isEqualTo("preaction explanation");
        assertThat(caseDetailsTab.getActionsTakenDetails().getMediationAttempted()).isEqualTo("Yes");
        assertThat(caseDetailsTab.getActionsTakenDetails().getSettlementAttempted()).isEqualTo("Yes");
        assertThat(caseDetailsTab.getMortgageOneDetails().getNameKnown()).isEqualTo("Yes");
        assertThat(caseDetailsTab.getMortgageOneDetails().getName())
            .isEqualTo("underlessee name");
        assertThat(caseDetailsTab.getMortgageOneDetails().getAddressKnown())
            .isEqualTo("Yes");
        assertThat(caseDetailsTab.getMortgageOneDetails().getAddress())
            .isEqualTo(underlesseeAddress);
        assertThat(caseDetailsTab.getMortgageDetails()).hasSize(1);
        assertThat(caseDetailsTab.getMortgageDetails().getFirst().getValue().getNameKnown()).isEqualTo("No");
        assertThat(caseDetailsTab.getMortgageDetails().getFirst().getValue().getName()).isNull();
        assertThat(caseDetailsTab.getMortgageDetails().getFirst().getValue().getAddressKnown()).isEqualTo("No");
        assertThat(caseDetailsTab.getMortgageDetails().getFirst().getValue().getAddress()).isNull();
        assertThat(caseDetailsTab.getSuspensionOfRightToBuyDetails().getHousingAct())
            .isEqualTo(SuspensionOfRightToBuyHousingAct.SECTION_6A_2.getLabel());
        assertThat(caseDetailsTab.getSuspensionOfRightToBuyDetails().getReasons())
            .isEqualTo("suspension reason");
        assertThat(caseDetailsTab.getDemotionOfTenancyDetails().getHousingAct())
            .isEqualTo(DemotionOfTenancyHousingAct.SECTION_6A_2.getLabel());
        assertThat(caseDetailsTab.getDemotionOfTenancyDetails().getReasons())
            .isEqualTo("demotion reason");
        assertThat(caseDetailsTab.getClaimantAddress()).isEqualTo(claimantAddress);
        assertThat(caseDetailsTab.getClaimantContactDetails().getEmailAddress()).isEqualTo("claimant@email.com");
        assertThat(caseDetailsTab.getClaimantContactDetails().getPhoneNumberProvided()).isEqualTo("Yes");
        assertThat(caseDetailsTab.getClaimantContactDetails().getPhoneNumber()).isEqualTo("phone number");
        assertThat(caseDetailsTab.getClaimantCircumstances().getClaimantCircumstancesGiven()).isEqualTo("Yes");
        assertThat(caseDetailsTab.getClaimantCircumstances().getClaimantCircumstancesDetails())
            .isEqualTo("claimant circumstances");
        assertThat(caseDetailsTab.getDefendantCircumstanceDetails().getDefendantCircumstancesGiven())
            .isEqualTo("Yes");
        assertThat(caseDetailsTab.getDefendantCircumstanceDetails().getDefendantCircumstances())
            .isEqualTo("defendant circumstances");
        assertThat(caseDetailsTab.getAntisocialAndConductDetails()).isNull();
        assertThat(caseDetailsTab.getClaimantRegistrationAndLicensingDetails()).isNull();
        assertThat(caseDetailsTab.getProhibitedConductStandardContractDetails()).isNull();
        assertThat(caseDetailsTab.getRequiredDocumentsDetails()).isNull();
    }

    @Test
    void shouldSetCaseDetailsTabFieldsWithNoData() {
        PCSCase pcsCase = PCSCase.builder()
            .legislativeCountry(LegislativeCountry.ENGLAND)
            .build();

        // When
        CaseDetailsTab caseDetailsTab = caseDetailsTabView.buildCaseDetailsTab(pcsCase);

        assertThat(caseDetailsTab.getPropertyAddress()).isNull();
        assertThat(caseDetailsTab.getGroundsForPossessionDetails().getGrounds()).isEqualTo(noAnswer);
        assertThat(caseDetailsTab.getReasonsForPossessionDetails()).isNull();
        assertThat(caseDetailsTab.getDateClaimSubmitted()).isEqualTo(noAnswer);
        assertThat(caseDetailsTab.getClaimantInformation()).isNull();
        assertThat(caseDetailsTab.getDefendantInformationDetails()).isNull();
        assertThat(caseDetailsTab.getAdditionalDefendants()).isNull();
        assertThat(caseDetailsTab.getRentArrearsDetails()).isNull();
        assertThat(caseDetailsTab.getTenancyLicenceDetails().getTypeOfTenancyLicence())
            .isEqualTo(noAnswer);
        assertThat(caseDetailsTab.getTenancyLicenceDetails().getTenancyLicenceDate())
            .isEqualTo(noAnswer);
        assertThat(caseDetailsTab.getNoticeDetails().getNoticeDate())
            .isEqualTo(noAnswer);
        assertThat(caseDetailsTab.getTenancyLicenceDetails().getHasCopyOfTenancyLicence())
            .isEqualTo(noAnswer);
        assertThat(caseDetailsTab.getTenancyLicenceDetails().getReasonsForNoTenancyLicenceDocuments())
            .isNull();
        assertThat(caseDetailsTab.getNoticeDetails().getNoticeMethod()).isEqualTo(noAnswer);
        assertThat(caseDetailsTab.getNoticeDetails().getNoticeServed()).isEqualTo(noAnswer);
        assertThat(caseDetailsTab.getApplicationsDetails().getPlanToMakeGeneralApplication()).isEqualTo(noAnswer);
        assertThat(caseDetailsTab.getActionsTakenDetails().getPreactionProtocolFollowed()).isEqualTo(noAnswer);
        assertThat(caseDetailsTab.getActionsTakenDetails().getPreActionProtocolIncompleteExplanation())
            .isNull();
        assertThat(caseDetailsTab.getActionsTakenDetails().getMediationAttempted()).isEqualTo(noAnswer);
        assertThat(caseDetailsTab.getActionsTakenDetails().getSettlementAttempted()).isEqualTo(noAnswer);
        assertThat(caseDetailsTab.getMortgageDetails()).isNull();
        assertThat(caseDetailsTab.getSuspensionOfRightToBuyDetails()).isNull();
        assertThat(caseDetailsTab.getDemotionOfTenancyDetails()).isNull();
        assertThat(caseDetailsTab.getClaimantCircumstances()).isNull();
        assertThat(caseDetailsTab.getDefendantCircumstanceDetails()).isNull();
        assertThat(caseDetailsTab.getClaimantContactDetails()).isNull();
        assertThat(caseDetailsTab.getClaimantAddress()).isNull();
    }

    @Test
    void shouldHandleOneUnderlesseeOrMortgageParty() {
        AddressUK underlesseeAddress = AddressUK.builder().postCode("CV1 1DF").build();
        PCSCase pcsCase = PCSCase.builder()
            .allUnderlesseeOrMortgagees(List.of(
                listValue(
                    Party.builder()
                        .nameKnown(VerticalYesNo.YES)
                        .orgName("underlessee name")
                        .addressKnown(VerticalYesNo.YES)
                        .address(underlesseeAddress)
                        .build()
                )
            ))
            .build();

        // When
        CaseDetailsTab caseDetailsTab = caseDetailsTabView.buildCaseDetailsTab(pcsCase);

        // Then
        assertThat(caseDetailsTab.getMortgageDetails()).isNull();
        assertThat(caseDetailsTab.getMortgageOneDetails().getNameKnown()).isEqualTo("Yes");
        assertThat(caseDetailsTab.getMortgageOneDetails().getName())
            .isEqualTo("underlessee name");
        assertThat(caseDetailsTab.getMortgageOneDetails().getAddressKnown())
            .isEqualTo("Yes");
        assertThat(caseDetailsTab.getMortgageOneDetails().getAddress())
            .isEqualTo(underlesseeAddress);
    }

    @Test
    void shouldSetPlaceholderValuesIfOnlyAlternativesToPossessionIsSet() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .legislativeCountry(LegislativeCountry.ENGLAND)
            .alternativesToPossession(Set.of(DEMOTION_OF_TENANCY, SUSPENSION_OF_RIGHT_TO_BUY))
            .build();

        // When
        CaseDetailsTab caseDetailsTab = caseDetailsTabView.buildCaseDetailsTab(pcsCase);

        // Then
        assertThat(caseDetailsTab.getSuspensionOfRightToBuyDetails().getHousingAct())
            .isEqualTo(noAnswer);
        assertThat(caseDetailsTab.getSuspensionOfRightToBuyDetails().getReasons())
            .isEqualTo(noAnswer);
        assertThat(caseDetailsTab.getDemotionOfTenancyDetails().getHousingAct())
            .isEqualTo(noAnswer);
        assertThat(caseDetailsTab.getDemotionOfTenancyDetails().getReasons())
            .isEqualTo(noAnswer);
    }

    @Test
    void shouldSetOtherGroundsDescriptionAssuredTenancy() {
        PCSCase pcsCase = PCSCase.builder()
            .tenancyLicenceDetails(TenancyLicenceDetails.builder()
                                       .typeOfTenancyLicence(TenancyLicenceType.ASSURED_TENANCY)
                                       .tenancyLicenceDate(LocalDate.of(2024, 4, 16))
                                       .hasCopyOfTenancyLicence(VerticalYesNo.NO)
                                       .reasonsForNoTenancyLicenceDocuments("Reasons")
                                       .build())
            .claimGroundSummaries(List.of(
                listValue(ClaimGroundSummary.builder()
                              .label("Other")
                              .reason("Other reason")
                              .description("description")
                              .code(AssuredAdditionalOtherGround.OTHER.name())
                              .build())
            ))
            .build();

        // When
        CaseDetailsTab caseDetailsTab = caseDetailsTabView.buildCaseDetailsTab(pcsCase);

        assertThat(caseDetailsTab.getGroundsForPossessionDetails().getOtherGroundsDescription())
            .isEqualTo("description");
    }

    @Test
    void shouldSetOtherGroundsDescriptionOtherTenancy() {
        PCSCase pcsCase = PCSCase.builder()
            .tenancyLicenceDetails(TenancyLicenceDetails.builder()
                                       .typeOfTenancyLicence(TenancyLicenceType.OTHER)
                                       .tenancyLicenceDate(LocalDate.of(2024, 4, 16))
                                       .hasCopyOfTenancyLicence(VerticalYesNo.NO)
                                       .reasonsForNoTenancyLicenceDocuments("Reasons")
                                       .build())
            .claimGroundSummaries(List.of(
                listValue(ClaimGroundSummary.builder()
                              .label("Other")
                              .reason("Other reason")
                              .description("description")
                              .code(IntroductoryDemotedOrOtherGrounds.OTHER.name())
                              .build())
            ))
            .build();

        // When
        CaseDetailsTab caseDetailsTab = caseDetailsTabView.buildCaseDetailsTab(pcsCase);

        // Then
        assertThat(caseDetailsTab.getGroundsForPossessionDetails().getOtherGroundsDescription())
            .isEqualTo("description");
    }

    @Test
    void shouldUseClaimantContactPreferencesIfAllClaimantsIsNotSet() {
        // Given
        AddressUK claimantAddress = AddressUK.builder().postCode("L2 3RT").build();
        PCSCase pcsCase = PCSCase.builder()
            .claimantContactPreferences(
                ClaimantContactPreferences.builder()
                    .orgAddressFound(YesOrNo.YES)
                    .isCorrectClaimantContactAddress(VerticalYesNo.YES)
                    .organisationAddress(claimantAddress)
                    .isCorrectClaimantContactEmail(VerticalYesNo.YES)
                    .claimantContactEmail("claimant@email.com")
                    .claimantProvidePhoneNumber(VerticalYesNo.YES)
                    .claimantContactPhoneNumber("phone number")
                    .build()
            )
            .build();

        when(claimantInformationTabDetailsBuilder.createSummaryClaimantTabDetails(pcsCase)).thenReturn(
            ClaimantInformationTabDetails.builder()
                .claimantName("Claimant")
                .build()
        );

        // When
        CaseDetailsTab caseDetailsTab = caseDetailsTabView.buildCaseDetailsTab(pcsCase);

        // Then
        assertThat(caseDetailsTab.getClaimantAddress()).isEqualTo(claimantAddress);
        assertThat(caseDetailsTab.getClaimantContactDetails().getEmailAddress()).isEqualTo("claimant@email.com");
        assertThat(caseDetailsTab.getClaimantContactDetails().getPhoneNumber()).isEqualTo("phone number");
    }

    @Test
    void shouldUseOverwrittenAddressAndEmail() {
        // Given
        AddressUK claimantAddress = AddressUK.builder().postCode("L2 3RT").build();
        PCSCase pcsCase = PCSCase.builder()
            .claimantContactPreferences(
                ClaimantContactPreferences.builder()
                    .orgAddressFound(YesOrNo.YES)
                    .isCorrectClaimantContactAddress(VerticalYesNo.NO)
                    .overriddenClaimantContactAddress(claimantAddress)
                    .isCorrectClaimantContactEmail(VerticalYesNo.NO)
                    .overriddenClaimantContactEmail("claimant@email.com")
                    .claimantProvidePhoneNumber(VerticalYesNo.NO)
                    .claimantContactPhoneNumber("phone number")
                    .build()
            )
            .build();

        when(claimantInformationTabDetailsBuilder.createSummaryClaimantTabDetails(pcsCase)).thenReturn(
            ClaimantInformationTabDetails.builder()
                .claimantName("Claimant")
                .build()
        );

        // When
        CaseDetailsTab caseDetailsTab = caseDetailsTabView.buildCaseDetailsTab(pcsCase);

        // Then
        assertThat(caseDetailsTab.getClaimantAddress()).isEqualTo(claimantAddress);
        assertThat(caseDetailsTab.getClaimantContactDetails().getEmailAddress()).isEqualTo("claimant@email.com");
        assertThat(caseDetailsTab.getClaimantContactDetails().getPhoneNumberProvided()).isEqualTo("No");
    }

    @Test
    void shouldUsePlaceholderValuesIfClaimantDetailsIsNotSet() {
        PCSCase pcsCase = PCSCase.builder().build();

        when(claimantInformationTabDetailsBuilder.createSummaryClaimantTabDetails(pcsCase)).thenReturn(
            ClaimantInformationTabDetails.builder()
                .claimantName("Claimant")
                .build()
        );

        // When
        CaseDetailsTab caseDetailsTab = caseDetailsTabView.buildCaseDetailsTab(pcsCase);

        // Then
        assertThat(caseDetailsTab.getClaimantAddress().getAddressLine1()).isEqualTo(noAnswer);
        assertThat(caseDetailsTab.getClaimantAddress().getPostTown()).isEqualTo(noAnswer);
        assertThat(caseDetailsTab.getClaimantAddress().getCountry()).isEqualTo(noAnswer);
        assertThat(caseDetailsTab.getClaimantAddress().getPostCode()).isEqualTo(noAnswer);
        assertThat(caseDetailsTab.getClaimantContactDetails().getEmailAddress()).isEqualTo(noAnswer);
        assertThat(caseDetailsTab.getClaimantContactDetails().getPhoneNumberProvided()).isEqualTo(noAnswer);
        assertThat(caseDetailsTab.getClaimantContactDetails().getPhoneNumber()).isNull();
        assertThat(caseDetailsTab.getClaimantCircumstances().getClaimantCircumstancesGiven()).isEqualTo(noAnswer);
        assertThat(caseDetailsTab.getClaimantCircumstances().getClaimantCircumstancesDetails()).isNull();
    }

    @Test
    void shouldSetNoticeDetailsForFirstClassPost() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .noticeServed(YesOrNo.YES)
            .noticeServedDetails(NoticeServedDetails.builder()
                                     .noticeServiceMethod(NoticeServiceMethod.FIRST_CLASS_POST)
                                     .noticePostedDate(LocalDate.of(2026, 5, 11))
                                     .build())
            .build();

        // When
        CaseDetailsTab caseDetailsTab = caseDetailsTabView.buildCaseDetailsTab(pcsCase);

        // Then
        assertThat(caseDetailsTab.getNoticeDetails().getNoticeMethod())
            .isEqualTo(NoticeServiceMethod.FIRST_CLASS_POST.getLabel());
        assertThat(caseDetailsTab.getNoticeDetails().getNoticeServed()).isEqualTo("Yes");
        assertThat(caseDetailsTab.getNoticeDetails().getNoticeDate()).isEqualTo("11 May 2026");
    }

    @Test
    void shouldSetNoticeDetailsForPermittedPlace() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .noticeServed(YesOrNo.YES)
            .noticeServedDetails(NoticeServedDetails.builder()
                                     .noticeServiceMethod(NoticeServiceMethod.DELIVERED_PERMITTED_PLACE)
                                     .noticeDeliveredDate(LocalDate.of(2026, 5, 11))
                                     .build())
            .build();

        // When
        CaseDetailsTab caseDetailsTab = caseDetailsTabView.buildCaseDetailsTab(pcsCase);

        // Then
        assertThat(caseDetailsTab.getNoticeDetails().getNoticeMethod())
            .isEqualTo(NoticeServiceMethod.DELIVERED_PERMITTED_PLACE.getLabel());
        assertThat(caseDetailsTab.getNoticeDetails().getNoticeServed()).isEqualTo("Yes");
        assertThat(caseDetailsTab.getNoticeDetails().getNoticeDate()).isEqualTo("11 May 2026");
    }

    @Test
    void shouldSetNoticeDetailsForPersonallyHanded() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .noticeServed(YesOrNo.YES)
            .noticeServedDetails(NoticeServedDetails.builder()
                                     .noticeServiceMethod(NoticeServiceMethod.PERSONALLY_HANDED)
                                     .noticeHandedOverDateTime(LocalDateTime.of(2026, 5, 11, 9, 0, 0))
                                     .noticePersonName("Notice name")
                                     .build())
            .build();

        // When
        CaseDetailsTab caseDetailsTab = caseDetailsTabView.buildCaseDetailsTab(pcsCase);

        // Then
        assertThat(caseDetailsTab.getNoticeDetails().getNoticeMethod())
            .isEqualTo(NoticeServiceMethod.PERSONALLY_HANDED.getLabel());
        assertThat(caseDetailsTab.getNoticeDetails().getNoticeServed()).isEqualTo("Yes");
        assertThat(caseDetailsTab.getNoticeDetails().getNoticeDate()).isEqualTo("11 May 2026, 9:00:00AM");
        assertThat(caseDetailsTab.getNoticeDetails().getNoticePersonName()).isEqualTo("Notice name");
    }

    @Test
    void shouldSetNoticeDetailsForOtherElectronic() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .noticeServed(YesOrNo.YES)
            .noticeServedDetails(NoticeServedDetails.builder()
                                     .noticeServiceMethod(NoticeServiceMethod.OTHER_ELECTRONIC)
                                     .noticeOtherElectronicDateTime(LocalDateTime.of(2026, 5, 11, 9, 0, 0))
                                     .noticeOtherElectronicMethodExplanation("explanation")
                                     .build())
            .build();

        // When
        CaseDetailsTab caseDetailsTab = caseDetailsTabView.buildCaseDetailsTab(pcsCase);

        // Then
        assertThat(caseDetailsTab.getNoticeDetails().getNoticeMethod())
            .isEqualTo(NoticeServiceMethod.OTHER_ELECTRONIC.getLabel());
        assertThat(caseDetailsTab.getNoticeDetails().getNoticeServed()).isEqualTo("Yes");
        assertThat(caseDetailsTab.getNoticeDetails().getNoticeDate()).isEqualTo("11 May 2026, 9:00:00AM");
        assertThat(caseDetailsTab.getNoticeDetails().getNoticeOtherElectronicDetails())
            .isEqualTo("explanation");
    }

    @Test
    void shouldSetNoticeDetailsForOther() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .noticeServed(YesOrNo.YES)
            .noticeServedDetails(NoticeServedDetails.builder()
                                     .noticeServiceMethod(NoticeServiceMethod.OTHER)
                                     .noticeOtherDateTime(LocalDateTime.of(2026, 5, 11, 9, 0, 0))
                                     .build())
            .build();

        // When
        CaseDetailsTab caseDetailsTab = caseDetailsTabView.buildCaseDetailsTab(pcsCase);

        // Then
        assertThat(caseDetailsTab.getNoticeDetails().getNoticeMethod())
            .isEqualTo(NoticeServiceMethod.OTHER.getLabel());
        assertThat(caseDetailsTab.getNoticeDetails().getNoticeServed()).isEqualTo("Yes");
        assertThat(caseDetailsTab.getNoticeDetails().getNoticeDate()).isEqualTo("11 May 2026, 9:00:00AM");
    }

    @Test
    void shouldDisplaySubmittedDateInUkTimeWhenServerTimezoneIsUtc() {
        // Given
        TimeZone originalTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        PCSCase pcsCase = PCSCase.builder()
            .dateSubmitted(LocalDateTime.of(2026, 7, 11, 17, 2, 31))
            .build();

        try {
            // When
            CaseDetailsTab caseDetailsTab = caseDetailsTabView.buildCaseDetailsTab(pcsCase);

            // Then
            assertThat(caseDetailsTab.getDateClaimSubmitted()).isEqualTo("11 July 2026, 6:02:31PM");
        } finally {
            TimeZone.setDefault(originalTimeZone);
        }
    }

    @Test
    void shouldDisplaySubmittedDateInGmtOutsideBritishSummerTimeWhenServerTimezoneIsUtc() {
        // Given
        TimeZone originalTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        PCSCase pcsCase = PCSCase.builder()
            .dateSubmitted(LocalDateTime.of(2026, 1, 11, 17, 2, 31))
            .build();

        try {
            // When
            CaseDetailsTab caseDetailsTab = caseDetailsTabView.buildCaseDetailsTab(pcsCase);

            // Then
            assertThat(caseDetailsTab.getDateClaimSubmitted()).isEqualTo("11 January 2026, 5:02:31PM");
        } finally {
            TimeZone.setDefault(originalTimeZone);
        }
    }

    @Test
    void shouldSetCaseDetailsTabFieldsForWales() {
        AddressUK propertyAddress = AddressUK.builder().postCode("SW1A 1AA").build();
        AddressUK defendantAddress = AddressUK.builder().postCode("E1 1AA").build();
        AddressUK underlesseeAddress = AddressUK.builder().postCode("CV1 1DF").build();
        AddressUK claimantAddress = AddressUK.builder().postCode("L2 3RT").build();
        PCSCase pcsCase = PCSCase.builder()
            .legislativeCountry(LegislativeCountry.WALES)
            .claimantType(
                DynamicStringList.builder().value(
                        DynamicStringListElement.builder().code(COMMUNITY_LANDLORD.name()).build())
                    .build())
            .claimAgainstTrespassers(VerticalYesNo.YES)
            .propertyAddress(propertyAddress)
            .dateSubmitted(LocalDateTime.of(2026, 1, 11, 17, 2, 31))
            .claimGroundSummaries(List.of(
                listValue(ClaimGroundSummary.builder()
                              .label("Rent arrears (ground 10)")
                              .reason("Ground 10 reason")
                              .code("ABSOLUTE_GROUNDS")
                              .build()),
                listValue(ClaimGroundSummary.builder()
                              .label("Condition 1 of Section 84A of the Housing Act 1985")
                              .reason("Condition 1 reason")
                              .code("ABSOLUTE_GROUNDS")
                              .build())
            ))
            .additionalReasonsForPossession(AdditionalReasons.builder()
                                                .hasReasons(VerticalYesNo.YES)
                                                .reasons("Additional reasons")
                                                .build())
            .allClaimants(
                List.of(listValue(
                    Party.builder()
                        .orgName("Claimant")
                        .address(claimantAddress)
                        .emailAddress("claimant@email.com")
                        .phoneNumberProvided(VerticalYesNo.YES)
                        .phoneNumber("phone number")
                        .build()
                ))
            )
            .claimantCircumstances(
                ClaimantCircumstances.builder()
                    .claimantCircumstancesSelect(VerticalYesNo.YES)
                    .claimantCircumstancesDetails("claimant circumstances")
                    .build()
            )
            .allDefendants(List.of(
                listValue(Party.builder()
                              .nameKnown(VerticalYesNo.YES)
                              .firstName("Defendant")
                              .lastName("One")
                              .addressKnown(VerticalYesNo.YES)
                              .build()),
                listValue(Party.builder()
                              .nameKnown(VerticalYesNo.YES)
                              .firstName("Defendant")
                              .lastName("Two")
                              .addressKnown(VerticalYesNo.YES)
                              .address(defendantAddress)
                              .build()),
                listValue(Party.builder()
                              .nameKnown(VerticalYesNo.NO)
                              .addressKnown(VerticalYesNo.NO)
                              .build())
            ))
            .defendantCircumstances(
                DefendantCircumstances.builder()
                    .hasDefendantCircumstancesInfo(VerticalYesNo.YES)
                    .defendantCircumstancesInfo("defendant circumstances")
                    .build()
            )
            .rentDetails(RentDetails.builder()
                             .currentRent(new BigDecimal("100.00"))
                             .frequency(RentPaymentFrequency.OTHER)
                             .otherFrequency("Every 4 weeks")
                             .perDayCorrect(VerticalYesNo.NO)
                             .amendedDailyCharge(new BigDecimal("12.30"))
                             .build())
            .rentArrears(RentArrearsSection.builder()
                             .total(new BigDecimal("450.75"))
                             .build())
            .arrearsJudgmentWanted(VerticalYesNo.YES)
            .occupationLicenceDetailsWales(
                OccupationLicenceDetailsWales.builder()
                    .occupationLicenceTypeWales(OccupationLicenceTypeWales.OTHER)
                    .licenceStartDate(LocalDate.of(2024, 4, 16))
                    .build()
            )
            .walesNoticeDetails(
                WalesNoticeDetails.builder()
                    .noticeServed(YesOrNo.YES)
                    .typeOfNoticeServed("notice type")
                    .noticeStatement("notice statement")
                    .build()
            )
            .noticeServedDetails(NoticeServedDetails.builder()
                                     .noticeServiceMethod(NoticeServiceMethod.EMAIL)
                                     .noticeEmailSentDateTime(LocalDateTime.of(2026, 5, 11, 17, 2))
                                     .build())
            .preActionProtocolCompleted(VerticalYesNo.NO)
            .mediationAttempted(VerticalYesNo.YES)
            .settlementAttempted(VerticalYesNo.YES)
            .applicationWithClaim(VerticalYesNo.YES)
            .allUnderlesseeOrMortgagees(List.of(
                listValue(
                    Party.builder()
                        .nameKnown(VerticalYesNo.YES)
                        .orgName("underlessee name")
                        .addressKnown(VerticalYesNo.YES)
                        .address(underlesseeAddress)
                        .build()
                )
            ))
            .alternativesToPossession(Set.of(DEMOTION_OF_TENANCY, SUSPENSION_OF_RIGHT_TO_BUY))
            .isExemptLandlord(VerticalYesNo.NO)
            .showASBQuestionsPageWales(YesOrNo.YES)
            .asbQuestionsWales(
                ASBQuestionsDetailsWales.builder()
                    .antisocialBehaviour(VerticalYesNo.YES)
                    .antisocialBehaviourDetails("antisocial")
                    .illegalPurposesUse(VerticalYesNo.YES)
                    .illegalPurposesUseDetails("illegalPurposesUse")
                    .otherProhibitedConduct(VerticalYesNo.YES)
                    .otherProhibitedConductDetails("otherProhibitedConduct")
                    .build()
            )
            .prohibitedConductWalesClaim(VerticalYesNo.YES)
            .prohibitedConductWalesClaimDetails("prohibitedConductWalesClaim")
            .periodicContractTermsWales(
                PeriodicContractTermsWales.builder()
                    .agreedTermsOfPeriodicContract(VerticalYesNo.YES)
                    .detailsOfTerms("agreedTermsOfPeriodicContract")
                    .build()
            )
            .requiredDocumentsWales(
                WalesDocuments.builder()
                    .hasEnergyPerformanceCertificate(VerticalYesNo.NO)
                    .hasGasSafetyReport(VerticalYesNo.NO)
                    .hasElectricalInstallationConditionReport(VerticalYesNo.NO)
                    .noEpcReason("noEpcReason")
                    .noGasReportReason("noGasReportReason")
                    .noEicrReason("noEicrReason")
                    .build()
            )
            .build();

        when(groundsBuilder.getGrounds(pcsCase)).thenReturn(
            "Rent arrears (ground 10)\n"
                + "Antisocial behaviour: Condition 1 of Section 84A of the Housing Act 1985"
        );

        when(rentArrearsTabDetailsBuilder.buildDetailedRentArrearsTabDetails(pcsCase)).thenReturn(
            RentArrearsTabDetails.builder()
                .rentAmount("£100")
                .calculationFrequency("Every 4 weeks")
                .dailyRate("£12.30")
                .arrearsTotal("£450.75")
                .judgmentRequested("Yes")
                .build()
        );

        when(reasonsForPossessionTabDetailsBuilder.buildDetailsReasonsForPossession(pcsCase)).thenReturn(
            ReasonsForPossessionTabDetails.builder()
                .ground10("Ground 10 reason")
                .condition1OfSection84A("Condition 1 reason")
                .additionalReasonsForPossession("Additional reasons")
                .build()
        );

        when(claimantInformationTabDetailsBuilder.createSummaryClaimantTabDetails(pcsCase)).thenReturn(
            ClaimantInformationTabDetails.builder()
                .claimantName("Claimant")
                .build()
        );

        when(defendantInformationTabDetailsBuilder.buildDetailedDefendantDetails(pcsCase)).thenReturn(
            DefendantInformationTabDetails.builder()
                .nameKnown("Yes")
                .firstName("Defendant")
                .lastName("One")
                .addressKnown("Yes")
                .addressForService(propertyAddress)
                .build()
        );

        when(additionalDefendantInformationTabDetailsBuilder.buildDetailedAdditionalDefendantsDetails(pcsCase))
            .thenReturn(
                List.of(
                    listValue(
                        AdditionalDefendantInformationTabDetails.builder()
                            .nameKnown("Yes")
                            .firstName("Defendant")
                            .lastName("Two")
                            .addressKnown("Yes")
                            .addressForService(defendantAddress)
                            .build()
                    ),
                    listValue(
                        AdditionalDefendantInformationTabDetails.builder()
                            .nameKnown("No")
                            .addressKnown("No")
                            .build()
                    )
                )
            );

        // When
        CaseDetailsTab caseDetailsTab = caseDetailsTabView.buildCaseDetailsTab(pcsCase);

        // Then
        assertThat(caseDetailsTab.getPropertyAddress()).isEqualTo(propertyAddress);
        assertThat(caseDetailsTab.getGroundsForPossessionDetails().getGrounds())
            .isEqualTo(
                "Rent arrears (ground 10)\n"
                    + "Antisocial behaviour: Condition 1 of Section 84A of the Housing Act 1985"
            );
        assertThat(caseDetailsTab.getReasonsForPossessionDetails().getGround10())
            .isEqualTo("Ground 10 reason");
        assertThat(caseDetailsTab.getReasonsForPossessionDetails().getCondition1OfSection84A())
            .isEqualTo("Condition 1 reason");
        assertThat(caseDetailsTab.getReasonsForPossessionDetails().getAdditionalReasonsForPossession())
            .isEqualTo("Additional reasons");
        assertThat(caseDetailsTab.getDateClaimSubmitted()).isEqualTo("11 January 2026, 5:02:31PM");
        assertThat(caseDetailsTab.getClaimantInformation().getClaimantName()).isEqualTo("Claimant");
        assertThat(caseDetailsTab.getDefendantInformationDetails().getFirstName()).isEqualTo("Defendant");
        assertThat(caseDetailsTab.getDefendantInformationDetails().getLastName()).isEqualTo("One");
        assertThat(caseDetailsTab.getDefendantInformationDetails().getAddressForService()).isEqualTo(propertyAddress);
        assertThat(caseDetailsTab.getAdditionalDefendants()).hasSize(2);
        assertThat(caseDetailsTab.getAdditionalDefendants().getFirst().getValue().getFirstName())
            .isEqualTo("Defendant");
        assertThat(caseDetailsTab.getAdditionalDefendants().getFirst().getValue().getLastName()).isEqualTo("Two");
        assertThat(caseDetailsTab.getAdditionalDefendants().getFirst().getValue().getAddressForService())
            .isEqualTo(defendantAddress);
        assertThat(caseDetailsTab.getAdditionalDefendants().get(1).getValue().getNameKnown())
            .isEqualTo("No");
        assertThat(caseDetailsTab.getAdditionalDefendants().get(1).getValue().getAddressKnown())
            .isEqualTo("No");
        assertThat(caseDetailsTab.getRentArrearsDetails().getRentAmount()).isEqualTo("£100");
        assertThat(caseDetailsTab.getRentArrearsDetails().getCalculationFrequency()).isEqualTo("Every 4 weeks");
        assertThat(caseDetailsTab.getRentArrearsDetails().getDailyRate()).isEqualTo("£12.30");
        assertThat(caseDetailsTab.getRentArrearsDetails().getArrearsTotal()).isEqualTo("£450.75");
        assertThat(caseDetailsTab.getRentArrearsDetails().getJudgmentRequested()).isEqualTo("Yes");
        assertThat(caseDetailsTab.getOccupationContractLicenceDetails().getAgreementType())
            .isEqualTo("Other");
        assertThat(caseDetailsTab.getOccupationContractLicenceDetails().getAgreementStartDate())
            .isEqualTo("16 April 2024");
        assertThat(caseDetailsTab.getOccupationContractLicenceDetails().getDocumentsPlaceholder()).isEqualTo(noAnswer);
        assertThat(caseDetailsTab.getOccupationContractLicenceDetails().getDocuments()).isNull();
        assertThat(caseDetailsTab.getNoticeDetails().getNoticeDate())
            .isEqualTo("11 May 2026, 5:02:00PM");
        assertThat(caseDetailsTab.getNoticeDetails().getNoticeMethod()).isEqualTo("By email");
        assertThat(caseDetailsTab.getNoticeDetails().getNoticeServed()).isEqualTo("Yes");
        assertThat(caseDetailsTab.getNoticeDetails().getTypeOfNoticeServed()).isEqualTo("notice type");
        assertThat(caseDetailsTab.getNoticeDetails().getStatement()).isNull();
        assertThat(caseDetailsTab.getApplicationsDetails().getPlanToMakeGeneralApplication()).isEqualTo("Yes");
        assertThat(caseDetailsTab.getActionsTakenDetails().getPreactionProtocolFollowed()).isEqualTo("No");
        assertThat(caseDetailsTab.getActionsTakenDetails().getMediationAttempted()).isEqualTo("Yes");
        assertThat(caseDetailsTab.getActionsTakenDetails().getSettlementAttempted()).isEqualTo("Yes");
        assertThat(caseDetailsTab.getMortgageDetails()).isNull();
        assertThat(caseDetailsTab.getMortgageOneDetails().getNameKnown()).isEqualTo("Yes");
        assertThat(caseDetailsTab.getMortgageOneDetails().getName()).isEqualTo("underlessee name");
        assertThat(caseDetailsTab.getMortgageOneDetails().getAddressKnown()).isEqualTo("Yes");
        assertThat(caseDetailsTab.getMortgageOneDetails().getAddress()).isEqualTo(underlesseeAddress);
        assertThat(caseDetailsTab.getClaimantAddress()).isEqualTo(claimantAddress);
        assertThat(caseDetailsTab.getClaimantContactDetails().getEmailAddress()).isEqualTo("claimant@email.com");
        assertThat(caseDetailsTab.getClaimantContactDetails().getPhoneNumberProvided()).isEqualTo("Yes");
        assertThat(caseDetailsTab.getClaimantContactDetails().getPhoneNumber()).isEqualTo("phone number");
        assertThat(caseDetailsTab.getClaimantCircumstances().getClaimantCircumstancesGiven()).isEqualTo("Yes");
        assertThat(caseDetailsTab.getClaimantCircumstances().getClaimantCircumstancesDetails())
            .isEqualTo("claimant circumstances");
        assertThat(caseDetailsTab.getDefendantCircumstanceDetails().getDefendantCircumstancesGiven())
            .isEqualTo("Yes");
        assertThat(caseDetailsTab.getDefendantCircumstanceDetails().getDefendantCircumstances())
            .isEqualTo("defendant circumstances");
        assertThat(caseDetailsTab.getClaimantRegistrationAndLicensingDetails().getIsExemptLandlord())
            .isEqualTo("No");
        assertThat(caseDetailsTab.getAntisocialAndConductDetails().getAntiSocialBehaviour()).isEqualTo("Yes");
        assertThat(caseDetailsTab.getAntisocialAndConductDetails().getAntiSocialBehaviourDetails())
            .isEqualTo("antisocial");
        assertThat(caseDetailsTab.getAntisocialAndConductDetails().getPropertyUsedIllegally()).isEqualTo("Yes");
        assertThat(caseDetailsTab.getAntisocialAndConductDetails().getPropertyUsedIllegallyDetails())
            .isEqualTo("illegalPurposesUse");
        assertThat(caseDetailsTab.getAntisocialAndConductDetails().getOtherProhibitedConduct()).isEqualTo("Yes");
        assertThat(caseDetailsTab.getAntisocialAndConductDetails().getOtherProhibitedConductDetails())
            .isEqualTo("otherProhibitedConduct");
        assertThat(
            caseDetailsTab.getProhibitedConductStandardContractDetails().getSeekingProhibitedConductStandardContract()
        ).isEqualTo("Yes");
        assertThat(caseDetailsTab.getProhibitedConductStandardContractDetails().getWhyMakingClaim())
            .isEqualTo("prohibitedConductWalesClaim");
        assertThat(caseDetailsTab.getProhibitedConductStandardContractDetails().getAgreedTerms())
            .isEqualTo("Yes");
        assertThat(caseDetailsTab.getProhibitedConductStandardContractDetails().getTermDetails())
            .isEqualTo("agreedTermsOfPeriodicContract");
        assertThat(caseDetailsTab.getRequiredDocumentsDetails().getHasGasSafetyReport()).isEqualTo("No");
        assertThat(caseDetailsTab.getRequiredDocumentsDetails().getHasEnergyPerformanceCertificate()).isEqualTo("No");
        assertThat(caseDetailsTab.getRequiredDocumentsDetails().getHasElectricalInstallationConditionReport())
            .isEqualTo("No");
        assertThat(caseDetailsTab.getRequiredDocumentsDetails().getNoGasSafetyReportReason())
            .isEqualTo("noGasReportReason");
        assertThat(caseDetailsTab.getRequiredDocumentsDetails().getNoEnergyPerformanceCertificateReason())
            .isEqualTo("noEpcReason");
        assertThat(caseDetailsTab.getRequiredDocumentsDetails().getNoElectricalInstallationConditionReportReason())
            .isEqualTo("noEicrReason");
        assertThat(caseDetailsTab.getRequiredDocumentsDetails().getGasSafetyReports()).isNull();
        assertThat(caseDetailsTab.getRequiredDocumentsDetails().getEnergyPerformanceCertificates()).isNull();
        assertThat(caseDetailsTab.getRequiredDocumentsDetails().getElectricalInstallationReports()).isNull();
        assertThat(caseDetailsTab.getTenancyLicenceDetails()).isNull();
    }

    @Test
    void shouldSetCaseDetailsTabFieldsWithNoDataWales() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .legislativeCountry(LegislativeCountry.WALES)
            .build();

        // When
        CaseDetailsTab caseDetailsTab = caseDetailsTabView.buildCaseDetailsTab(pcsCase);

        // Then
        assertThat(caseDetailsTab.getOccupationContractLicenceDetails().getAgreementType()).isEqualTo(noAnswer);
        assertThat(caseDetailsTab.getOccupationContractLicenceDetails().getAgreementStartDate()).isEqualTo(noAnswer);
        assertThat(caseDetailsTab.getOccupationContractLicenceDetails().getDocumentsPlaceholder()).isEqualTo(noAnswer);
        assertThat(caseDetailsTab.getOccupationContractLicenceDetails().getDocuments()).isNull();

        assertThat(caseDetailsTab.getNoticeDetails().getNoticeDate()).isEqualTo(noAnswer);
        assertThat(caseDetailsTab.getNoticeDetails().getNoticeMethod()).isEqualTo(noAnswer);
        assertThat(caseDetailsTab.getNoticeDetails().getNoticeServed()).isEqualTo(noAnswer);
        assertThat(caseDetailsTab.getNoticeDetails().getNoticeDate()).isEqualTo(noAnswer);

        assertThat(caseDetailsTab.getAntisocialAndConductDetails()).isNull();
        assertThat(caseDetailsTab.getProhibitedConductStandardContractDetails()).isNull();
        assertThat(caseDetailsTab.getClaimantRegistrationAndLicensingDetails()).isNull();
    }

    @Test
    void shouldPlaceholderWhenShowASBQuestionsPageWalesIsYesWithNoOtherData() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .legislativeCountry(LegislativeCountry.WALES)
            .showASBQuestionsPageWales(YesOrNo.YES)
            .build();

        // When
        CaseDetailsTab caseDetailsTab = caseDetailsTabView.buildCaseDetailsTab(pcsCase);

        // Then
        assertThat(caseDetailsTab.getAntisocialAndConductDetails().getAntiSocialBehaviour()).isEqualTo(noAnswer);
        assertThat(caseDetailsTab.getAntisocialAndConductDetails().getAntiSocialBehaviourDetails()).isNull();
        assertThat(caseDetailsTab.getAntisocialAndConductDetails().getPropertyUsedIllegally()).isEqualTo(noAnswer);
        assertThat(caseDetailsTab.getAntisocialAndConductDetails().getPropertyUsedIllegallyDetails()).isNull();
        assertThat(caseDetailsTab.getAntisocialAndConductDetails().getOtherProhibitedConduct()).isEqualTo(noAnswer);
        assertThat(caseDetailsTab.getAntisocialAndConductDetails().getOtherProhibitedConductDetails()).isNull();
    }

    @Test
    void shouldNotSetDetailsValuesIfASBQuestionsDetailsWalesAreNo() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .legislativeCountry(LegislativeCountry.WALES)
            .showASBQuestionsPageWales(YesOrNo.YES)
            .asbQuestionsWales(
                ASBQuestionsDetailsWales.builder()
                    .antisocialBehaviour(VerticalYesNo.NO)
                    .antisocialBehaviourDetails("antisocial")
                    .illegalPurposesUse(VerticalYesNo.NO)
                    .illegalPurposesUseDetails("illegalPurposesUse")
                    .otherProhibitedConduct(VerticalYesNo.NO)
                    .otherProhibitedConductDetails("otherProhibitedConduct")
                    .build()
            )
            .build();

        // When
        CaseDetailsTab caseDetailsTab = caseDetailsTabView.buildCaseDetailsTab(pcsCase);

        // Then
        assertThat(caseDetailsTab.getAntisocialAndConductDetails().getAntiSocialBehaviour()).isEqualTo("No");
        assertThat(caseDetailsTab.getAntisocialAndConductDetails().getAntiSocialBehaviourDetails()).isNull();
        assertThat(caseDetailsTab.getAntisocialAndConductDetails().getPropertyUsedIllegally()).isEqualTo("No");
        assertThat(caseDetailsTab.getAntisocialAndConductDetails().getPropertyUsedIllegallyDetails()).isNull();
        assertThat(caseDetailsTab.getAntisocialAndConductDetails().getOtherProhibitedConduct()).isEqualTo("No");
        assertThat(caseDetailsTab.getAntisocialAndConductDetails().getOtherProhibitedConductDetails()).isNull();
    }

    @Test
    void shouldNotSetDetailedNoticeDetailsWhenNoticeServedIsNoEngland() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .noticeServed(YesOrNo.NO)
            .build();

        // When
        CaseDetailsTab caseDetailsTab = caseDetailsTabView.buildCaseDetailsTab(pcsCase);

        assertThat(caseDetailsTab.getNoticeDetails().getNoticeServed()).isEqualTo("No");
        assertThat(caseDetailsTab.getNoticeDetails().getNoticeDate()).isEqualTo(noAnswer);
        assertThat(caseDetailsTab.getNoticeDetails().getNoticeMethod()).isEqualTo(noAnswer);
    }

    @Test
    void shouldNotSetDetailedNoticeDetailsWhenNoticeServedIsNoWales() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .legislativeCountry(LegislativeCountry.WALES)
            .walesNoticeDetails(
                WalesNoticeDetails.builder()
                    .noticeServed(YesOrNo.NO)
                    .build()
            )
            .build();

        // When
        CaseDetailsTab caseDetailsTab = caseDetailsTabView.buildCaseDetailsTab(pcsCase);

        // Then
        assertThat(caseDetailsTab.getNoticeDetails().getNoticeServed()).isEqualTo("No");
        assertThat(caseDetailsTab.getNoticeDetails().getNoticeDate()).isEqualTo(noAnswer);
        assertThat(caseDetailsTab.getNoticeDetails().getNoticeMethod()).isEqualTo(noAnswer);
    }

    @Test
    void shouldShowDocumentsInRequiredDocumentsTabDetails() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .legislativeCountry(LegislativeCountry.WALES)
            .requiredDocumentsWales(
                WalesDocuments.builder()
                    .hasEnergyPerformanceCertificate(VerticalYesNo.YES)
                    .hasGasSafetyReport(VerticalYesNo.YES)
                    .hasElectricalInstallationConditionReport(VerticalYesNo.YES)
                    .noEpcReason("noEpcReason")
                    .noGasReportReason("noGasReportReason")
                    .noEicrReason("noEicrReason")
                    .gasSafetyReport(List.of(listValue(Document.builder().build())))
                    .energyPerformance((List.of(listValue(Document.builder().build()))))
                    .electricalInstallation((List.of(listValue(Document.builder().build()))))
                    .build()
            )
            .build();

        // When
        CaseDetailsTab caseDetailsTab = caseDetailsTabView.buildCaseDetailsTab(pcsCase);

        // Then
        assertThat(caseDetailsTab.getRequiredDocumentsDetails().getHasGasSafetyReport()).isEqualTo("Yes");
        assertThat(caseDetailsTab.getRequiredDocumentsDetails().getHasEnergyPerformanceCertificate()).isEqualTo("Yes");
        assertThat(caseDetailsTab.getRequiredDocumentsDetails().getHasElectricalInstallationConditionReport())
            .isEqualTo("Yes");
        assertThat(caseDetailsTab.getRequiredDocumentsDetails().getNoGasSafetyReportReason()).isNull();
        assertThat(caseDetailsTab.getRequiredDocumentsDetails().getNoEnergyPerformanceCertificateReason()).isNull();
        assertThat(caseDetailsTab.getRequiredDocumentsDetails().getNoElectricalInstallationConditionReportReason())
            .isNull();
        assertThat(caseDetailsTab.getRequiredDocumentsDetails().getGasSafetyReports()).hasSize(1);
        assertThat(caseDetailsTab.getRequiredDocumentsDetails().getEnergyPerformanceCertificates()).hasSize(1);
        assertThat(caseDetailsTab.getRequiredDocumentsDetails().getElectricalInstallationReports()).hasSize(1);
    }

    @Test
    void shouldSetNoticeStatementIfNoticeServedIsNoWales() {
        PCSCase pcsCase = PCSCase.builder()
            .legislativeCountry(LegislativeCountry.WALES)
            .walesNoticeDetails(
                WalesNoticeDetails.builder()
                    .noticeServed(YesOrNo.NO)
                    .typeOfNoticeServed("notice type")
                    .noticeStatement("notice statement")
                    .build()
            )
            .build();

        // When
        CaseDetailsTab caseDetailsTab = caseDetailsTabView.buildCaseDetailsTab(pcsCase);

        // Then
        assertThat(caseDetailsTab.getNoticeDetails().getNoticeServed()).isEqualTo("No");
        assertThat(caseDetailsTab.getNoticeDetails().getStatement()).isEqualTo("notice statement");
        assertThat(caseDetailsTab.getNoticeDetails().getTypeOfNoticeServed()).isNull();
    }

    private static <T> ListValue<T> listValue(T value) {
        return ListValue.<T>builder()
            .value(value)
            .build();
    }
}
