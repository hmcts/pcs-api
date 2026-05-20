package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
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
import uk.gov.hmcts.reform.pcs.ccd.view.builder.AdditionalDefendantInformationTabDetailsBuilder;
import uk.gov.hmcts.reform.pcs.ccd.view.builder.ClaimantInformationTabDetailsBuilder;
import uk.gov.hmcts.reform.pcs.ccd.view.builder.DefendantInformationTabDetailsBuilder;
import uk.gov.hmcts.reform.pcs.ccd.view.builder.GroundsBuilder;
import uk.gov.hmcts.reform.pcs.ccd.view.builder.ReasonsForPossessionTabDetailsBuilder;
import uk.gov.hmcts.reform.pcs.ccd.view.builder.RentArrearsTabDetailsBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CaseSummaryTabViewTest {

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

    private CaseSummaryTabView underTest;

    @BeforeEach
    void setUp() {
        underTest = new CaseSummaryTabView(
            groundsBuilder,
            rentArrearsTabDetailsBuilder,
            reasonsForPossessionTabDetailsBuilder,
            claimantInformationTabDetailsBuilder,
            defendantInformationTabDetailsBuilder,
            additionalDefendantInformationTabDetailsBuilder
        );
    }

    @Test
    void shouldSetSummaryTabFields() {
        // Given
        AddressUK propertyAddress = AddressUK.builder().postCode("SW1A 1AA").build();
        AddressUK defendantAddress = AddressUK.builder().postCode("E1 1AA").build();
        PCSCase pcsCase = PCSCase.builder()
            .propertyAddress(propertyAddress)
            .dateSubmitted(LocalDateTime.of(2026, 5, 11, 17, 2, 31))
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
        assertThat(summaryTab.getDateClaimSubmitted()).isEqualTo("11 May 2026, 5:02:31PM");
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
        assertThat(summaryTab.getNoticeDetails().getNoticeServedDate()).isEqualTo("11/05/2026");
    }

    @Test
    void shouldDisplaySubmittedDateInGmtWhenBritishSummerTimeApplies() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .dateSubmitted(LocalDateTime.of(2026, 7, 11, 17, 2, 31))
            .build();

        // When
        SummaryTab summaryTab = underTest.buildSummaryTab(pcsCase);

        // Then
        assertThat(summaryTab.getDateClaimSubmitted()).isEqualTo("11 July 2026, 5:02:31PM");
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
    }

    @Test
    void shouldSetTenancyDetailsFromWalesOccupationLicenceTypeLabel() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .occupationLicenceDetailsWales(OccupationLicenceDetailsWales.builder()
                                               .occupationLicenceTypeWales(OccupationLicenceTypeWales.SECURE_CONTRACT)
                                               .licenceStartDate(LocalDate.of(2025, 5, 12))
                                               .build())
            .build();

        // When
        SummaryTab summaryTab = underTest.buildSummaryTab(pcsCase);

        // Then
        assertThat(summaryTab.getTenancyDetails().getAgreementType()).isEqualTo("Secure contract");
        assertThat(summaryTab.getTenancyDetails().getAgreementStartDate()).isEqualTo("12/05/2025");
    }

    @Test
    void shouldSetTenancyDetailsFromWalesOtherOccupationLicenceType() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .occupationLicenceDetailsWales(OccupationLicenceDetailsWales.builder()
                                               .occupationLicenceTypeWales(OccupationLicenceTypeWales.OTHER)
                                               .otherLicenceTypeDetails("Other Welsh licence")
                                               .build())
            .build();

        // When
        SummaryTab summaryTab = underTest.buildSummaryTab(pcsCase);

        // Then
        assertThat(summaryTab.getTenancyDetails().getAgreementType()).isEqualTo("Other Welsh licence");
        assertThat(summaryTab.getTenancyDetails().getAgreementStartDate()).isNull();
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
