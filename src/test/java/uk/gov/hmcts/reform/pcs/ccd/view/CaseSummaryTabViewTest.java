package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalReasons;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantInformation;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServedDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsSection;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentPaymentFrequency;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.ClaimGroundSummary;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared.RentArrearsTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared.ReasonsForPossessionTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.summary.SummaryTab;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceDetailsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceTypeWales;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class CaseSummaryTabViewTest {

    private CaseSummaryTabView underTest;

    @BeforeEach
    void setUp() {
        underTest = new CaseSummaryTabView();
    }

    @Test
    void shouldSetSummaryTabFields() {
        // Given
        AddressUK propertyAddress = AddressUK.builder().postCode("SW1A 1AA").build();
        AddressUK defendantAddress = AddressUK.builder().postCode("E1 1AA").build();
        PCSCase pcsCase = PCSCase.builder()
            .propertyAddress(propertyAddress)
            .dateSubmitted(LocalDateTime.of(2026, 1, 11, 17, 2, 31))
            .claimGroundSummaries(List.of(
                listValue(ClaimGroundSummary.builder()
                              .label("Rent arrears (ground 10)")
                              .reason("Ground 10 reason")
                              .build()),
                listValue(ClaimGroundSummary.builder()
                              .label("Condition 1 of Section 84A of the Housing Act 1985")
                              .reason("Condition 1 reason")
                              .build())
            ))
            .additionalReasonsForPossession(AdditionalReasons.builder()
                                                .hasReasons(VerticalYesNo.YES)
                                                .reasons("Additional reasons")
                                                .build())
            .claimantInformation(ClaimantInformation.builder()
                                      .orgNameFound(YesOrNo.NO)
                                      .fallbackClaimantName("Fallback claimant")
                                      .build())
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
                                       .detailsOfOtherTypeOfTenancyLicence("Licence details")
                                       .tenancyLicenceDate(LocalDate.of(2024, 4, 16))
                                       .build())
            .noticeServedDetails(NoticeServedDetails.builder()
                                     .noticeEmailSentDateTime(LocalDateTime.of(2026, 5, 11, 17, 2))
                                     .build())
            .build();

        // When
        SummaryTab summaryTab = underTest.buildSummaryTab(pcsCase);

        // Then
        assertThat(summaryTab.getRepossessedPropertyAddress()).isEqualTo(propertyAddress);
        assertThat(summaryTab.getGroundsForPossession().getGrounds())
            .isEqualTo(
                "Rent arrears (ground 10)\n"
                    + "Antisocial behaviour: Condition 1 of Section 84A of the Housing Act 1985"
            );
        assertThat(summaryTab.getReasonsForPossession().getGround10()).isEqualTo("Ground 10 reason");
        assertThat(summaryTab.getReasonsForPossession().getCondition1OfSection84A())
            .isEqualTo("Condition 1 reason");
        assertThat(summaryTab.getReasonsForPossession().getAdditionalReasonsForPossession())
            .isEqualTo("Additional reasons");
        assertThat(summaryTab.getDateClaimSubmitted()).isEqualTo("11 January 2026, 5:02:31PM");
        assertThat(summaryTab.getClaimantDetails().getClaimantName()).isEqualTo("Fallback claimant");
        assertThat(summaryTab.getDefendantDetails().getFirstName()).isEqualTo("Defendant");
        assertThat(summaryTab.getDefendantDetails().getLastName()).isEqualTo("One");
        assertThat(summaryTab.getDefendantDetails().getAddressForService()).isEqualTo(propertyAddress);
        assertThat(summaryTab.getAdditionalDefendants()).hasSize(2);
        assertThat(summaryTab.getAdditionalDefendants().getFirst().getValue().getFirstName()).isEqualTo("Defendant");
        assertThat(summaryTab.getAdditionalDefendants().getFirst().getValue().getLastName()).isEqualTo("Two");
        assertThat(summaryTab.getAdditionalDefendants().getFirst().getValue().getAddressForService())
            .isEqualTo(defendantAddress);
        assertThat(summaryTab.getAdditionalDefendants().get(1).getValue().getFirstName())
            .isEqualTo(CaseTabView.NAME_UNKNOWN);
        assertThat(summaryTab.getAdditionalDefendants().get(1).getValue().getLastName())
            .isEqualTo(CaseTabView.NAME_UNKNOWN);
        assertThat(summaryTab.getAdditionalDefendants().get(1).getValue().getAddressForService())
            .isEqualTo(propertyAddress);
        assertThat(summaryTab.getRentArrearsDetails().getRentAmount()).isEqualTo("£100");
        assertThat(summaryTab.getRentArrearsDetails().getCalculationFrequency()).isEqualTo("Every 4 weeks");
        assertThat(summaryTab.getRentArrearsDetails().getDailyRate()).isEqualTo("£12.30");
        assertThat(summaryTab.getRentArrearsDetails().getArrearsTotal()).isEqualTo("£450.75");
        assertThat(summaryTab.getRentArrearsDetails().getJudgmentRequested()).isEqualTo("Yes");
        assertThat(summaryTab.getTenancyDetails().getAgreementType()).isEqualTo("Licence details");
        assertThat(summaryTab.getTenancyDetails().getAgreementStartDate()).isEqualTo("16/04/2024");
        assertThat(summaryTab.getOccupationContractOrLicenceDetails()).isNull();
        assertThat(summaryTab.getNoticeDetails().getNoticeServedDate()).isEqualTo("11/05/2026");
    }

    @Test
    void shouldDisplaySubmittedDateInUkTimeWhenServerTimezoneIsUk() {
        // Given
        TimeZone originalTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/London"));
        PCSCase pcsCase = PCSCase.builder()
            .dateSubmitted(LocalDateTime.of(2026, 7, 11, 17, 2, 31))
            .build();

        try {
            // When
            SummaryTab summaryTab = underTest.buildSummaryTab(pcsCase);

            // Then
            assertThat(summaryTab.getDateClaimSubmitted()).isEqualTo("11 July 2026, 5:02:31PM");
        } finally {
            TimeZone.setDefault(originalTimeZone);
        }
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
            SummaryTab summaryTab = underTest.buildSummaryTab(pcsCase);

            // Then
            assertThat(summaryTab.getDateClaimSubmitted()).isEqualTo("11 July 2026, 6:02:31PM");
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
            SummaryTab summaryTab = underTest.buildSummaryTab(pcsCase);

            // Then
            assertThat(summaryTab.getDateClaimSubmitted()).isEqualTo("11 January 2026, 5:02:31PM");
        } finally {
            TimeZone.setDefault(originalTimeZone);
        }
    }

    @Test
    void shouldSetSummaryClaimantNameFromOverriddenName() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .claimantInformation(ClaimantInformation.builder()
                                      .isClaimantNameCorrect(VerticalYesNo.NO)
                                      .overriddenClaimantName("Overridden claimant")
                                      .build())
            .build();

        // When
        SummaryTab summaryTab = underTest.buildSummaryTab(pcsCase);

        // Then
        assertThat(summaryTab.getClaimantDetails().getClaimantName()).isEqualTo("Overridden claimant");
    }

    @Test
    void shouldSetSummaryClaimantNameFromClaimantInformationName() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .claimantInformation(ClaimantInformation.builder()
                                      .claimantName("Claimant information name")
                                      .build())
            .build();

        // When
        SummaryTab summaryTab = underTest.buildSummaryTab(pcsCase);

        // Then
        assertThat(summaryTab.getClaimantDetails().getClaimantName()).isEqualTo("Claimant information name");
    }

    @Test
    void shouldSetSummaryClaimantNameFromAllClaimants() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .allClaimants(List.of(listValue(Party.builder().orgName("Claimant party").build())))
            .build();

        // When
        SummaryTab summaryTab = underTest.buildSummaryTab(pcsCase);

        // Then
        assertThat(summaryTab.getClaimantDetails().getClaimantName()).isEqualTo("Claimant party");
    }

    @Test
    void shouldNotSetEmptySummarySections() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .allDefendants(List.of(listValue(Party.builder()
                                                .nameKnown(VerticalYesNo.NO)
                                                .addressKnown(VerticalYesNo.NO)
                                                .build())))
            .build();

        // When
        SummaryTab summaryTab = underTest.buildSummaryTab(pcsCase);

        // Then
        assertThat(summaryTab.getGroundsForPossession().getGrounds()).isNull();
        assertThat(summaryTab.getReasonsForPossession()).isNull();
        assertThat(summaryTab.getDateClaimSubmitted()).isNull();
        assertThat(summaryTab.getClaimantDetails()).isNull();
        assertThat(summaryTab.getDefendantDetails()).isNull();
        assertThat(summaryTab.getAdditionalDefendants()).isNull();
        assertThat(summaryTab.getRentArrearsDetails()).isNull();
        assertThat(summaryTab.getTenancyDetails()).isNull();
        assertThat(summaryTab.getNoticeDetails()).isNull();
    }

    @Test
    void shouldSetUnknownDefendantNameWhenNameNotKnownButAddressKnown() {
        // Given
        AddressUK address = AddressUK.builder().postCode("SW1A 1AA").build();
        PCSCase pcsCase = PCSCase.builder()
            .allDefendants(List.of(listValue(Party.builder()
                                                .nameKnown(VerticalYesNo.NO)
                                                .addressKnown(VerticalYesNo.YES)
                                                .address(address)
                                                .build())))
            .build();

        // When
        SummaryTab summaryTab = underTest.buildSummaryTab(pcsCase);

        // Then
        assertThat(summaryTab.getDefendantDetails().getFirstName()).isEqualTo(CaseTabView.NAME_UNKNOWN);
        assertThat(summaryTab.getDefendantDetails().getLastName()).isEqualTo(CaseTabView.NAME_UNKNOWN);
        assertThat(summaryTab.getDefendantDetails().getAddressForService()).isEqualTo(address);
    }

    @Test
    void shouldDefaultDefendantAddressForServiceToPropertyAddressWhenAddressNotKnown() {
        // Given
        AddressUK propertyAddress = AddressUK.builder().postCode("SW1A 1AA").build();
        PCSCase pcsCase = PCSCase.builder()
            .propertyAddress(propertyAddress)
            .allDefendants(List.of(listValue(Party.builder()
                                                .nameKnown(VerticalYesNo.NO)
                                                .addressKnown(VerticalYesNo.NO)
                                                .build())))
            .build();

        // When
        SummaryTab summaryTab = underTest.buildSummaryTab(pcsCase);

        // Then
        assertThat(summaryTab.getDefendantDetails().getFirstName()).isEqualTo(CaseTabView.NAME_UNKNOWN);
        assertThat(summaryTab.getDefendantDetails().getLastName()).isEqualTo(CaseTabView.NAME_UNKNOWN);
        assertThat(summaryTab.getDefendantDetails().getAddressForService()).isEqualTo(propertyAddress);
    }

    @Test
    void shouldSetUnknownAdditionalDefendantNameWhenNameNotKnownButAddressKnown() {
        // Given
        AddressUK address = AddressUK.builder().postCode("SW1A 1AA").build();
        PCSCase pcsCase = PCSCase.builder()
            .allDefendants(List.of(
                listValue(Party.builder()
                              .nameKnown(VerticalYesNo.YES)
                              .firstName("Defendant")
                              .lastName("One")
                              .build()),
                listValue(Party.builder()
                              .nameKnown(VerticalYesNo.NO)
                              .addressKnown(VerticalYesNo.YES)
                              .address(address)
                              .build())
            ))
            .build();

        // When
        SummaryTab summaryTab = underTest.buildSummaryTab(pcsCase);

        // Then
        assertThat(summaryTab.getAdditionalDefendants()).hasSize(1);
        assertThat(summaryTab.getAdditionalDefendants().getFirst().getValue().getFirstName())
            .isEqualTo(CaseTabView.NAME_UNKNOWN);
        assertThat(summaryTab.getAdditionalDefendants().getFirst().getValue().getLastName())
            .isEqualTo(CaseTabView.NAME_UNKNOWN);
        assertThat(summaryTab.getAdditionalDefendants().getFirst().getValue().getAddressForService())
            .isEqualTo(address);
    }

    @Test
    void shouldDefaultAdditionalDefendantAddressForServiceToPropertyAddressWhenAddressNotKnown() {
        // Given
        AddressUK propertyAddress = AddressUK.builder().postCode("SW1A 1AA").build();
        PCSCase pcsCase = PCSCase.builder()
            .propertyAddress(propertyAddress)
            .allDefendants(List.of(
                listValue(Party.builder()
                              .nameKnown(VerticalYesNo.YES)
                              .firstName("Defendant")
                              .lastName("One")
                              .build()),
                listValue(Party.builder()
                              .nameKnown(VerticalYesNo.NO)
                              .addressKnown(VerticalYesNo.NO)
                              .build())
            ))
            .build();

        // When
        SummaryTab summaryTab = underTest.buildSummaryTab(pcsCase);

        // Then
        assertThat(summaryTab.getAdditionalDefendants()).hasSize(1);
        assertThat(summaryTab.getAdditionalDefendants().getFirst().getValue().getFirstName())
            .isEqualTo(CaseTabView.NAME_UNKNOWN);
        assertThat(summaryTab.getAdditionalDefendants().getFirst().getValue().getLastName())
            .isEqualTo(CaseTabView.NAME_UNKNOWN);
        assertThat(summaryTab.getAdditionalDefendants().getFirst().getValue().getAddressForService())
            .isEqualTo(propertyAddress);
    }

    @Test
    void shouldSetEachGroundReasonInSummaryTab() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .claimGroundSummaries(List.of(
                listValue(ClaimGroundSummary.builder()
                              .label("Antisocial behaviour")
                              .reason("Antisocial reason")
                              .build()),
                listValue(ClaimGroundSummary.builder()
                              .label("Premium paid in connection with mutual exchange (ground 6)")
                              .reason("Premium reason")
                              .build()),
                listValue(ClaimGroundSummary.builder()
                              .label("Offence during a riot (ground 2ZA)")
                              .reason("Riot reason")
                              .build()),
                listValue(ClaimGroundSummary.builder()
                              .label("Condition 1 of Section 84A of the Housing Act 1985")
                              .reason("Condition 1 reason")
                              .build()),
                listValue(ClaimGroundSummary.builder()
                              .label("Landlord’s works (ground 10)")
                              .reason("Works reason")
                              .build()),
                listValue(ClaimGroundSummary.builder()
                              .label("Housing association special circumstances accommodation (ground 14)")
                              .reason("Housing reason")
                              .build())
            ))
            .build();

        // When
        ReasonsForPossessionTabDetails reasons = underTest.buildSummaryTab(pcsCase).getReasonsForPossession();

        // Then
        assertThat(reasons.getAntisocialBehaviour()).isEqualTo("Antisocial reason");
        assertThat(reasons.getGround6()).isEqualTo("Premium reason");
        assertThat(reasons.getGround2ZA()).isEqualTo("Riot reason");
        assertThat(reasons.getCondition1OfSection84A()).isEqualTo("Condition 1 reason");
        assertThat(reasons.getGround10()).isEqualTo("Works reason");
        assertThat(reasons.getGround14()).isEqualTo("Housing reason");
    }

    @Test
    void shouldGroupSection84AConditionsUnderAntisocialBehaviourInGrounds() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .claimGroundSummaries(List.of(
                groundSummary("Antisocial behaviour", "Antisocial reason"),
                groundSummary("Nuisance, annoyance, illegal or immoral use of the property (ground 2)",
                              "Nuisance reason"),
                groundSummary("Condition 3 of Section 84A of the Housing Act 1985", "Condition 3 reason"),
                groundSummary("Condition 1 of Section 84A of the Housing Act 1985", "Condition 1 reason"),
                groundSummary("Condition 5 of Section 84A of the Housing Act 1985", "Condition 5 reason"),
                groundSummary("Condition 2 of Section 84A of the Housing Act 1985", "Condition 2 reason"),
                groundSummary("Condition 4 of Section 84A of the Housing Act 1985", "Condition 4 reason"),
                groundSummary("Landlord’s works (ground 10)", "Works reason")
            ))
            .build();

        // When
        SummaryTab summaryTab = underTest.buildSummaryTab(pcsCase);

        // Then
        assertThat(summaryTab.getGroundsForPossession().getGrounds()).isEqualTo(String.join("\n",
            "Antisocial behaviour: " + String.join(", ",
                "Condition 1 of Section 84A of the Housing Act 1985",
                "Condition 2 of Section 84A of the Housing Act 1985",
                "Condition 3 of Section 84A of the Housing Act 1985",
                "Condition 4 of Section 84A of the Housing Act 1985",
                "Condition 5 of Section 84A of the Housing Act 1985"
            ),
            "Nuisance, annoyance, illegal or immoral use of the property (ground 2)",
            "Landlord’s works (ground 10)"
        ));
    }

    @Test
    void shouldGroupSection84AConditionsUnderAntisocialBehaviourWhenParentGroundIsMissing() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .claimGroundSummaries(List.of(
                groundSummary("Condition 2 of Section 84A of the Housing Act 1985", "Condition 2 reason"),
                groundSummary("Condition 1 of Section 84A of the Housing Act 1985", "Condition 1 reason"),
                groundSummary("Landlord’s works (ground 10)", "Works reason")
            ))
            .build();

        // When
        SummaryTab summaryTab = underTest.buildSummaryTab(pcsCase);

        // Then
        assertThat(summaryTab.getGroundsForPossession().getGrounds()).isEqualTo(String.join("\n",
            "Antisocial behaviour: " + String.join(", ",
                "Condition 1 of Section 84A of the Housing Act 1985",
                "Condition 2 of Section 84A of the Housing Act 1985"
            ),
            "Landlord’s works (ground 10)"
        ));
    }

    @Test
    void shouldGroupEstateManagementGroundsUnderSection160InGrounds() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .claimGroundSummaries(List.of(
                groundSummary("Estate management grounds (section 160)", null),
                groundSummary("Notice given under a landlord’s break clause (section 199)", "Section 199 reason"),
                groundSummary("Other estate management reasons (ground I)", "Ground I reason"),
                groundSummary("Redevelopment schemes (ground B)", "Ground B reason"),
                groundSummary("Building works (ground A)", "Ground A reason"),
                groundSummary("Reserve successors (ground G)", "Ground G reason")
            ))
            .build();

        // When
        SummaryTab summaryTab = underTest.buildSummaryTab(pcsCase);

        // Then
        assertThat(summaryTab.getGroundsForPossession().getGrounds()).isEqualTo(String.join("\n",
            "Estate management grounds (section 160): " + String.join(", ",
                "Building works (ground A)",
                "Redevelopment schemes (ground B)",
                "Reserve successors (ground G)",
                "Other estate management reasons (ground I)"
            ),
            "Notice given under a landlord’s break clause (section 199)"
        ));
    }

    @Test
    void shouldGroupEstateManagementGroundsUnderSection160WhenParentGroundIsMissing() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .claimGroundSummaries(List.of(
                groundSummary("Redevelopment schemes (ground B)", "Ground B reason"),
                groundSummary("Building works (ground A)", "Ground A reason")
            ))
            .build();

        // When
        SummaryTab summaryTab = underTest.buildSummaryTab(pcsCase);

        // Then
        assertThat(summaryTab.getGroundsForPossession().getGrounds()).isEqualTo(
            "Estate management grounds (section 160): " + String.join(", ",
                "Building works (ground A)",
                "Redevelopment schemes (ground B)"
            )
        );
    }

    @Test
    void shouldSetEachGroundNumberReasonInSummaryTab() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .claimGroundSummaries(List.of(
                groundSummary("Ground label (ground 1)", "Reason 1"),
                groundSummary("Ground label (ground 2)", "Reason 2"),
                groundSummary("Ground label (ground 2A)", "Reason 2A"),
                groundSummary("Ground label (ground 2ZA)", "Reason 2ZA"),
                groundSummary("Ground label (ground 3)", "Reason 3"),
                groundSummary("Ground label (ground 4)", "Reason 4"),
                groundSummary("Ground label (ground 5)", "Reason 5"),
                groundSummary("Ground label (ground 6)", "Reason 6"),
                groundSummary("Ground label (ground 7)", "Reason 7"),
                groundSummary("Ground label (ground 7A)", "Reason 7A"),
                groundSummary("Ground label (ground 7B)", "Reason 7B"),
                groundSummary("Ground label (ground 8)", "Reason 8"),
                groundSummary("Ground label (ground 9)", "Reason 9"),
                groundSummary("Ground label (ground 10)", "Reason 10"),
                groundSummary("Ground label (ground 10A)", "Reason 10A"),
                groundSummary("Ground label (ground 11)", "Reason 11"),
                groundSummary("Ground label (ground 12)", "Reason 12"),
                groundSummary("Ground label (ground 13)", "Reason 13"),
                groundSummary("Ground label (ground 14)", "Reason 14"),
                groundSummary("Ground label (ground 14A)", "Reason 14A"),
                groundSummary("Ground label (ground 14ZA)", "Reason 14ZA"),
                groundSummary("Ground label (ground 15)", "Reason 15"),
                groundSummary("Ground label (ground 15A)", "Reason 15A"),
                groundSummary("Ground label (ground 16)", "Reason 16"),
                groundSummary("Ground label (ground 17)", "Reason 17"),
                groundSummary("Ground label (ground A)", "Reason A"),
                groundSummary("Ground label (ground B)", "Reason B"),
                groundSummary("Ground label (ground C)", "Reason C"),
                groundSummary("Ground label (ground D)", "Reason D"),
                groundSummary("Ground label (ground E)", "Reason E"),
                groundSummary("Ground label (ground F)", "Reason F"),
                groundSummary("Ground label (ground G)", "Reason G"),
                groundSummary("Ground label (ground H)", "Reason H"),
                groundSummary("Ground label (ground I)", "Reason I"),
                groundSummary("Ground label (ground Z)", "Unmapped reason")
            ))
            .build();

        // When
        ReasonsForPossessionTabDetails reasons = underTest.buildSummaryTab(pcsCase).getReasonsForPossession();

        // Then
        assertThat(reasons.getGround1()).isEqualTo("Reason 1");
        assertThat(reasons.getGround2()).isEqualTo("Reason 2");
        assertThat(reasons.getGround2A()).isEqualTo("Reason 2A");
        assertThat(reasons.getGround2ZA()).isEqualTo("Reason 2ZA");
        assertThat(reasons.getGround3()).isEqualTo("Reason 3");
        assertThat(reasons.getGround4()).isEqualTo("Reason 4");
        assertThat(reasons.getGround5()).isEqualTo("Reason 5");
        assertThat(reasons.getGround6()).isEqualTo("Reason 6");
        assertThat(reasons.getGround7()).isEqualTo("Reason 7");
        assertThat(reasons.getGround7A()).isEqualTo("Reason 7A");
        assertThat(reasons.getGround7B()).isEqualTo("Reason 7B");
        assertThat(reasons.getGround8()).isEqualTo("Reason 8");
        assertThat(reasons.getGround9()).isEqualTo("Reason 9");
        assertThat(reasons.getGround10()).isEqualTo("Reason 10");
        assertThat(reasons.getGround10A()).isEqualTo("Reason 10A");
        assertThat(reasons.getGround11()).isEqualTo("Reason 11");
        assertThat(reasons.getGround12()).isEqualTo("Reason 12");
        assertThat(reasons.getGround13()).isEqualTo("Reason 13");
        assertThat(reasons.getGround14()).isEqualTo("Reason 14");
        assertThat(reasons.getGround14A()).isEqualTo("Reason 14A");
        assertThat(reasons.getGround14ZA()).isEqualTo("Reason 14ZA");
        assertThat(reasons.getGround15()).isEqualTo("Reason 15");
        assertThat(reasons.getGround15A()).isEqualTo("Reason 15A");
        assertThat(reasons.getGround16()).isEqualTo("Reason 16");
        assertThat(reasons.getGround17()).isEqualTo("Reason 17");
        assertThat(reasons.getGroundA()).isEqualTo("Reason A");
        assertThat(reasons.getGroundB()).isEqualTo("Reason B");
        assertThat(reasons.getGroundC()).isEqualTo("Reason C");
        assertThat(reasons.getGroundD()).isEqualTo("Reason D");
        assertThat(reasons.getGroundE()).isEqualTo("Reason E");
        assertThat(reasons.getGroundF()).isEqualTo("Reason F");
        assertThat(reasons.getGroundG()).isEqualTo("Reason G");
        assertThat(reasons.getGroundH()).isEqualTo("Reason H");
        assertThat(reasons.getGroundI()).isEqualTo("Reason I");
    }

    @Test
    void shouldSetEachSectionReasonInSummaryTab() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .claimGroundSummaries(List.of(
                groundSummary("Ground label (section 157)", "Reason 157"),
                groundSummary("Ground label (section 170)", "Reason 170"),
                groundSummary("Ground label (section 178)", "Reason 178"),
                groundSummary("Ground label (section 181)", "Reason 181"),
                groundSummary("Ground label (section 186)", "Reason 186"),
                groundSummary("Ground label (section 187)", "Reason 187"),
                groundSummary("Ground label (section 191)", "Reason 191"),
                groundSummary("Ground label (section 199)", "Reason 199"),
                groundSummary("Ground label (section 999)", "Unmapped reason")
            ))
            .build();

        // When
        ReasonsForPossessionTabDetails reasons = underTest.buildSummaryTab(pcsCase).getReasonsForPossession();

        // Then
        assertThat(reasons.getSection157()).isEqualTo("Reason 157");
        assertThat(reasons.getSection170()).isEqualTo("Reason 170");
        assertThat(reasons.getSection178()).isEqualTo("Reason 178");
        assertThat(reasons.getSection181()).isEqualTo("Reason 181");
        assertThat(reasons.getSection186()).isEqualTo("Reason 186");
        assertThat(reasons.getSection187()).isEqualTo("Reason 187");
        assertThat(reasons.getSection191()).isEqualTo("Reason 191");
        assertThat(reasons.getSection199()).isEqualTo("Reason 199");
    }

    @Test
    void shouldSetNonNumberedGroundReasonsInSummaryTab() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .claimGroundSummaries(List.of(
                groundSummary("Condition 1 of Section 84A of the Housing Act 1985", "Condition 1 reason"),
                groundSummary("Condition 2 of Section 84A of the Housing Act 1985", "Condition 2 reason"),
                groundSummary("Condition 3 of Section 84A of the Housing Act 1985", "Condition 3 reason"),
                groundSummary("Condition 4 of Section 84A of the Housing Act 1985", "Condition 4 reason"),
                groundSummary("Condition 5 of Section 84A of the Housing Act 1985", "Condition 5 reason"),
                groundSummary("Antisocial behaviour", "Antisocial reason"),
                groundSummary("Breach of the tenancy", "Breach reason"),
                groundSummary("Absolute grounds", "Absolute reason"),
                groundSummary("Other", "Other reason"),
                groundSummary("Other grounds", "Other grounds reason"),
                groundSummary("No grounds", "No grounds reason"),
                groundSummary("Converted contract paragraph 25B(2) of Schedule 12", "Paragraph reason"),
                groundSummary("Unmapped label", "Unmapped reason")
            ))
            .build();

        // When
        ReasonsForPossessionTabDetails reasons = underTest.buildSummaryTab(pcsCase).getReasonsForPossession();

        // Then
        assertThat(reasons.getCondition1OfSection84A()).isEqualTo("Condition 1 reason");
        assertThat(reasons.getCondition2OfSection84A()).isEqualTo("Condition 2 reason");
        assertThat(reasons.getCondition3OfSection84A()).isEqualTo("Condition 3 reason");
        assertThat(reasons.getCondition4OfSection84A()).isEqualTo("Condition 4 reason");
        assertThat(reasons.getCondition5OfSection84A()).isEqualTo("Condition 5 reason");
        assertThat(reasons.getAntisocialBehaviour()).isEqualTo("Antisocial reason");
        assertThat(reasons.getBreachOfTheTenancy()).isEqualTo("Breach reason");
        assertThat(reasons.getAbsoluteGrounds()).isEqualTo("Absolute reason");
        assertThat(reasons.getOtherGrounds()).isEqualTo("Other grounds reason");
        assertThat(reasons.getNoGrounds()).isEqualTo("No grounds reason");
        assertThat(reasons.getParagraph25B2Schedule12()).isEqualTo("Paragraph reason");
    }

    @Test
    void shouldSetRentArrearsDetailsFromStandardFrequencyAndDailyCharge() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .rentDetails(RentDetails.builder()
                             .frequency(RentPaymentFrequency.WEEKLY)
                             .dailyCharge(new BigDecimal("1.50"))
                             .build())
            .build();

        // When
        RentArrearsTabDetails rentArrearsDetails = underTest.buildSummaryTab(pcsCase).getRentArrearsDetails();

        // Then
        assertThat(rentArrearsDetails.getCalculationFrequency()).isEqualTo("Weekly");
        assertThat(rentArrearsDetails.getDailyRate()).isEqualTo("£1.50");
    }

    @Test
    void shouldSetRentArrearsDetailsFromFormattedCalculatedDailyCharge() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .rentDetails(RentDetails.builder()
                             .formattedCalculatedDailyCharge("£2.34")
                             .build())
            .build();

        // When
        SummaryTab summaryTab = underTest.buildSummaryTab(pcsCase);

        // Then
        assertThat(summaryTab.getRentArrearsDetails().getDailyRate()).isEqualTo("£2.34");
    }

    @Test
    void shouldSetRentArrearsDetailsFromCalculatedDailyCharge() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .rentDetails(RentDetails.builder()
                             .calculatedDailyCharge(new BigDecimal("3.40"))
                             .build())
            .build();

        // When
        SummaryTab summaryTab = underTest.buildSummaryTab(pcsCase);

        // Then
        assertThat(summaryTab.getRentArrearsDetails().getDailyRate()).isEqualTo("£3.40");
    }

    @Test
    void shouldSetTenancyDetailsFromTenancyLicenceTypeLabel() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .tenancyLicenceDetails(TenancyLicenceDetails.builder()
                                       .typeOfTenancyLicence(TenancyLicenceType.ASSURED_TENANCY)
                                       .build())
            .build();

        // When
        SummaryTab summaryTab = underTest.buildSummaryTab(pcsCase);

        // Then
        assertThat(summaryTab.getTenancyDetails().getAgreementType()).isEqualTo("Assured tenancy");
        assertThat(summaryTab.getTenancyDetails().getAgreementStartDate()).isNull();
        assertThat(summaryTab.getOccupationContractOrLicenceDetails()).isNull();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("unavailableTenancyDetailsScenarios")
    void shouldNotSetTenancyDetailsWhenNeitherEnglandNorWalesTenancyTypeIsAvailable(
        String scenario,
        TenancyLicenceDetails tenancyLicenceDetails,
        OccupationLicenceDetailsWales occupationLicenceDetailsWales
    ) {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .tenancyLicenceDetails(tenancyLicenceDetails)
            .occupationLicenceDetailsWales(occupationLicenceDetailsWales)
            .build();

        // When
        SummaryTab summaryTab = underTest.buildSummaryTab(pcsCase);

        // Then
        assertThat(summaryTab.getTenancyDetails()).isNull();
        assertThat(summaryTab.getOccupationContractOrLicenceDetails()).isNull();
    }

    @ParameterizedTest
    @MethodSource("walesOccupationLicenceTypeLabelScenarios")
    void shouldSetTenancyDetailsFromWalesOccupationLicenceTypeLabelWhenEnglandTenancyDetailsAreUnavailable(
        TenancyLicenceDetails tenancyLicenceDetails,
        OccupationLicenceTypeWales occupationLicenceType,
        String expectedAgreementType
    ) {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .legislativeCountry(LegislativeCountry.WALES)
            .tenancyLicenceDetails(tenancyLicenceDetails)
            .occupationLicenceDetailsWales(OccupationLicenceDetailsWales.builder()
                                               .occupationLicenceTypeWales(occupationLicenceType)
                                               .licenceStartDate(LocalDate.of(2025, 5, 12))
                                               .build())
            .build();

        // When
        SummaryTab summaryTab = underTest.buildSummaryTab(pcsCase);

        // Then
        assertThat(summaryTab.getTenancyDetails()).isNull();
        assertThat(summaryTab.getOccupationContractOrLicenceDetails().getAgreementType())
            .isEqualTo(expectedAgreementType);
        assertThat(summaryTab.getOccupationContractOrLicenceDetails().getAgreementStartDate()).isEqualTo("12/05/2025");
    }

    @Test
    void shouldSetTenancyDetailsFromWalesOtherOccupationLicenceType() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .legislativeCountry(LegislativeCountry.WALES)
            .occupationLicenceDetailsWales(OccupationLicenceDetailsWales.builder()
                                               .occupationLicenceTypeWales(OccupationLicenceTypeWales.OTHER)
                                               .otherLicenceTypeDetails("Other Welsh licence")
                                               .build())
            .build();

        // When
        SummaryTab summaryTab = underTest.buildSummaryTab(pcsCase);

        // Then
        assertThat(summaryTab.getTenancyDetails()).isNull();
        assertThat(summaryTab.getOccupationContractOrLicenceDetails().getAgreementType())
            .isEqualTo("Other Welsh licence");
        assertThat(summaryTab.getOccupationContractOrLicenceDetails().getAgreementStartDate()).isNull();
    }

    private static Stream<Arguments> unavailableTenancyDetailsScenarios() {
        return Stream.of(
            Arguments.of(
                "no England tenancy details and no Wales occupation licence details",
                null,
                null
            ),
            Arguments.of(
                "England tenancy details do not have a type and no Wales occupation licence details",
                TenancyLicenceDetails.builder().build(),
                null
            ),
            Arguments.of(
                "no England tenancy details and Wales occupation licence details do not have a type",
                null,
                OccupationLicenceDetailsWales.builder().build()
            ),
            Arguments.of(
                "England tenancy details and Wales occupation licence details do not have a type",
                TenancyLicenceDetails.builder().build(),
                OccupationLicenceDetailsWales.builder().build()
            )
        );
    }

    private static Stream<Arguments> walesOccupationLicenceTypeLabelScenarios() {
        return Stream.of(
            Arguments.of(
                null,
                OccupationLicenceTypeWales.SECURE_CONTRACT,
                "Secure contract"
            ),
            Arguments.of(
                TenancyLicenceDetails.builder().build(),
                OccupationLicenceTypeWales.STANDARD_CONTRACT,
                "Standard contract"
            )
        );
    }

    private static <T> ListValue<T> listValue(T value) {
        return ListValue.<T>builder()
            .value(value)
            .build();
    }

    private static ListValue<ClaimGroundSummary> groundSummary(String label, String reason) {
        return listValue(ClaimGroundSummary.builder()
                             .label(label)
                             .reason(reason)
                             .build());
    }
}
