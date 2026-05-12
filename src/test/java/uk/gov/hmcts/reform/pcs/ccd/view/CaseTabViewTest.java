package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
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
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.ClaimantTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.DefendantTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared.RentArrearsTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.summary.SummaryTab;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceDetailsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceTypeWales;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CaseTabViewTest {

    private CaseTabView underTest;

    @BeforeEach
    void setUp() {
        underTest = new CaseTabView();
    }

    @Test
    void shouldSetClaimantDetailsInCasePartiesTab() {
        // Given
        String name = "claimant";
        AddressUK address = AddressUK.builder().build();
        String telephoneNumber = "telephone number";
        String emailAddress = "email@test.com";
        Party claimant = Party.builder()
            .orgName(name)
            .address(address)
            .phoneNumber(telephoneNumber)
            .emailAddress(emailAddress)
            .build();
        List<ListValue<Party>> claimants = List.of(
            ListValue.<Party>builder()
                .value(claimant)
                .build()
        );

        PCSCase pcsCase = PCSCase.builder()
            .allClaimants(claimants)
            .build();

        // When
        underTest.setCaseTabFields(pcsCase);

        // Then
        assertThat(pcsCase.getCasePartiesTab()).isNotNull();
        ClaimantTabDetails claimantTabDetails = pcsCase.getCasePartiesTab().getClaimantDetails();
        assertThat(claimantTabDetails).isNotNull();
        assertThat(claimantTabDetails.getName()).isEqualTo(name);
        assertThat(claimantTabDetails.getServiceAddress()).isEqualTo(address);
        assertThat(claimantTabDetails.getEmailAddress()).isEqualTo(emailAddress);
        assertThat(claimantTabDetails.getTelephoneNumber()).isEqualTo(telephoneNumber);
    }

    @Test
    void shouldSetDefendantOneDetailsInCasePartiesTab() {
        // Given
        String firstName = "defendant";
        String lastName = "one";
        AddressUK address = AddressUK.builder().build();
        Party defendant = Party.builder()
            .firstName(firstName)
            .lastName(lastName)
            .nameKnown(VerticalYesNo.YES)
            .address(address)
            .build();

        ListValue<Party> defendantListValue = ListValue.<Party>builder()
            .value(defendant)
            .build();

        List<ListValue<Party>> defendants = new ArrayList<>();
        defendants.add(defendantListValue);

        PCSCase pcsCase = PCSCase.builder()
            .allDefendants(defendants)
            .build();

        // When
        underTest.setCaseTabFields(pcsCase);

        // Then
        assertThat(pcsCase.getCasePartiesTab()).isNotNull();
        DefendantTabDetails defendant1TabDetails = pcsCase.getCasePartiesTab().getDefendantOneDetails();
        List<ListValue<DefendantTabDetails>> additionalDefendantsTabDetails =
            pcsCase.getCasePartiesTab().getDefendantsDetails();

        assertThat(defendant1TabDetails.getFirstName()).isEqualTo(firstName);
        assertThat(defendant1TabDetails.getLastName()).isEqualTo(lastName);
        assertThat(defendant1TabDetails.getServiceAddress()).isEqualTo(address);
        assertThat(additionalDefendantsTabDetails).isNull();
    }

    @Test
    void shouldSetMultipleDefendantDetailsInCasePartiesTab() {
        // Given
        String defendant1FirstName = "defendant1";
        String defendant1LastName = "one";
        AddressUK address1 = AddressUK.builder().build();
        Party defendant1 = Party.builder()
            .firstName(defendant1FirstName)
            .lastName(defendant1LastName)
            .nameKnown(VerticalYesNo.YES)
            .address(address1)
            .build();

        String defendant2FirstName = "defendant2";
        String defendant2LastName = "two";
        AddressUK address2 = AddressUK.builder().build();
        Party defendant2 = Party.builder()
            .firstName(defendant2FirstName)
            .lastName(defendant2LastName)
            .nameKnown(VerticalYesNo.YES)
            .address(address2)
            .build();

        ListValue<Party> defendant1ListValue = ListValue.<Party>builder()
            .value(defendant1)
            .build();

        ListValue<Party> defendant2ListValue = ListValue.<Party>builder()
            .value(defendant2)
            .build();

        List<ListValue<Party>> defendants = new ArrayList<>();
        defendants.add(defendant1ListValue);
        defendants.add(defendant2ListValue);

        PCSCase pcsCase = PCSCase.builder()
            .allDefendants(defendants)
            .build();

        // When
        underTest.setCaseTabFields(pcsCase);

        // Then
        assertThat(pcsCase.getCasePartiesTab()).isNotNull();
        DefendantTabDetails defendant1TabDetails = pcsCase.getCasePartiesTab().getDefendantOneDetails();
        List<ListValue<DefendantTabDetails>> additionalDefendantsTabDetails =
            pcsCase.getCasePartiesTab().getDefendantsDetails();

        assertThat(defendant1TabDetails.getFirstName()).isEqualTo(defendant1FirstName);
        assertThat(defendant1TabDetails.getLastName()).isEqualTo(defendant1LastName);
        assertThat(defendant1TabDetails.getServiceAddress()).isEqualTo(address1);
        assertThat(additionalDefendantsTabDetails).isNotNull();
        assertThat(additionalDefendantsTabDetails.size()).isEqualTo(1);

        DefendantTabDetails defendant2TabDetails = additionalDefendantsTabDetails.getFirst().getValue();
        assertThat(defendant2TabDetails.getFirstName()).isEqualTo(defendant2FirstName);
        assertThat(defendant2TabDetails.getLastName()).isEqualTo(defendant2LastName);
        assertThat(defendant2TabDetails.getServiceAddress()).isEqualTo(address2);
    }

    @Test
    void shouldSetDefaultDefendantNameInCasePartiesTabIfNameNotKnown() {
        // Given
        String firstName = "defendant";
        String lastName = "One";
        AddressUK address = AddressUK.builder().build();
        Party defendant = Party.builder()
            .firstName(firstName)
            .lastName(lastName)
            .nameKnown(VerticalYesNo.NO)
            .address(address)
            .build();

        ListValue<Party> defendantListValue = ListValue.<Party>builder()
            .value(defendant)
            .build();

        List<ListValue<Party>> defendants = new ArrayList<>();
        defendants.add(defendantListValue);

        PCSCase pcsCase = PCSCase.builder()
            .allDefendants(defendants)
            .build();

        // When
        underTest.setCaseTabFields(pcsCase);

        // Then
        assertThat(pcsCase.getCasePartiesTab()).isNotNull();
        DefendantTabDetails defendant1TabDetails = pcsCase.getCasePartiesTab().getDefendantOneDetails();
        List<ListValue<DefendantTabDetails>> additionalDefendantsTabDetails =
            pcsCase.getCasePartiesTab().getDefendantsDetails();

        assertThat(defendant1TabDetails.getFirstName()).isEqualTo("Person unknown");
        assertThat(defendant1TabDetails.getLastName()).isEqualTo("Person unknown");
        assertThat(defendant1TabDetails.getServiceAddress()).isEqualTo(address);
        assertThat(additionalDefendantsTabDetails).isNull();
    }

    @Test
    void shouldSetDefaultDefendantAddressToPropertyAddressIfNotKnown() {
        // Given
        String firstName = "defendant";
        String lastName = "One";
        AddressUK address = AddressUK.builder().build();
        Party defendant = Party.builder()
            .firstName(firstName)
            .lastName(lastName)
            .nameKnown(VerticalYesNo.YES)
            .address(null)
            .build();

        ListValue<Party> defendantListValue = ListValue.<Party>builder()
            .value(defendant)
            .build();

        List<ListValue<Party>> defendants = new ArrayList<>();
        defendants.add(defendantListValue);

        PCSCase pcsCase = PCSCase.builder()
            .allDefendants(defendants)
            .propertyAddress(address)
            .build();

        // When
        underTest.setCaseTabFields(pcsCase);

        // Then
        assertThat(pcsCase.getCasePartiesTab()).isNotNull();
        DefendantTabDetails defendant1TabDetails = pcsCase.getCasePartiesTab().getDefendantOneDetails();
        List<ListValue<DefendantTabDetails>> additionalDefendantsTabDetails =
            pcsCase.getCasePartiesTab().getDefendantsDetails();

        assertThat(defendant1TabDetails.getFirstName()).isEqualTo(firstName);
        assertThat(defendant1TabDetails.getLastName()).isEqualTo(lastName);
        assertThat(defendant1TabDetails.getServiceAddress()).isEqualTo(address);
        assertThat(additionalDefendantsTabDetails).isNull();
    }

    @Test
    void shouldNotSetCasePartiesTabWithNoData() {
        // Given
        PCSCase pcsCase = PCSCase.builder().build();

        // When
        underTest.setCaseTabFields(pcsCase);

        // Then
        assertThat(pcsCase.getCasePartiesTab()).isNotNull();
        assertThat(pcsCase.getCasePartiesTab().getClaimantDetails()).isNull();
        assertThat(pcsCase.getCasePartiesTab().getDefendantOneDetails()).isNull();
        assertThat(pcsCase.getCasePartiesTab().getDefendantsDetails()).isNull();
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
                listValue(ClaimGroundSummary.builder().label("Ground 1").build()),
                listValue(ClaimGroundSummary.builder().label("Ground 2").build())
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
        underTest.setCaseTabFields(pcsCase);

        // Then
        SummaryTab summaryTab = pcsCase.getSummaryTab();
        assertThat(summaryTab.getRepossessedPropertyAddress()).isEqualTo(propertyAddress);
        assertThat(summaryTab.getGroundsForPossession().getGrounds()).isEqualTo("Ground 1, Ground 2");
        assertThat(summaryTab.getClaimSubmittedDate()).isEqualTo("11 May 2026, 5:02:31PM");
        assertThat(summaryTab.getReasonsForPossession().getGroundReasons()).isEqualTo("Yes");
        assertThat(summaryTab.getReasonsForPossession().getAdditionalReasonsForPossession())
            .isEqualTo("Additional reasons");
        assertThat(summaryTab.getPossessionReasonsSubmittedDate()).isEqualTo("11 May 2026, 5:02:31PM");
        assertThat(summaryTab.getClaimantDetails().getClaimantName()).isEqualTo("Fallback claimant");
        assertThat(summaryTab.getDefendantDetails().getFirstName()).isEqualTo("Defendant");
        assertThat(summaryTab.getDefendantDetails().getLastName()).isEqualTo("One");
        assertThat(summaryTab.getDefendantDetails().getAddressForService()).isEqualTo(propertyAddress);
        assertThat(summaryTab.getAdditionalDefendants()).hasSize(1);
        assertThat(summaryTab.getAdditionalDefendants().getFirst().getValue().getFirstName()).isEqualTo("Defendant");
        assertThat(summaryTab.getAdditionalDefendants().getFirst().getValue().getLastName()).isEqualTo("Two");
        assertThat(summaryTab.getAdditionalDefendants().getFirst().getValue().getAddressForService())
            .isEqualTo(defendantAddress);
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
    void shouldSetSummaryClaimantNameFromOverriddenName() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .claimantInformation(ClaimantInformation.builder()
                                      .isClaimantNameCorrect(VerticalYesNo.NO)
                                      .overriddenClaimantName("Overridden claimant")
                                      .build())
            .build();

        // When
        underTest.setCaseTabFields(pcsCase);

        // Then
        assertThat(pcsCase.getSummaryTab().getClaimantDetails().getClaimantName()).isEqualTo("Overridden claimant");
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
        underTest.setCaseTabFields(pcsCase);

        // Then
        assertThat(pcsCase.getSummaryTab().getClaimantDetails().getClaimantName())
            .isEqualTo("Claimant information name");
    }

    @Test
    void shouldSetSummaryClaimantNameFromAllClaimants() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .allClaimants(List.of(listValue(Party.builder().orgName("Claimant party").build())))
            .build();

        // When
        underTest.setCaseTabFields(pcsCase);

        // Then
        assertThat(pcsCase.getSummaryTab().getClaimantDetails().getClaimantName()).isEqualTo("Claimant party");
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
        underTest.setCaseTabFields(pcsCase);

        // Then
        SummaryTab summaryTab = pcsCase.getSummaryTab();
        assertThat(summaryTab.getGroundsForPossession().getGrounds()).isNull();
        assertThat(summaryTab.getReasonsForPossession()).isNull();
        assertThat(summaryTab.getPossessionReasonsSubmittedDate()).isNull();
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
        underTest.setCaseTabFields(pcsCase);

        // Then
        RentArrearsTabDetails rentArrearsDetails = pcsCase.getSummaryTab().getRentArrearsDetails();
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
        underTest.setCaseTabFields(pcsCase);

        // Then
        assertThat(pcsCase.getSummaryTab().getRentArrearsDetails().getDailyRate()).isEqualTo("£2.34");
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
        underTest.setCaseTabFields(pcsCase);

        // Then
        assertThat(pcsCase.getSummaryTab().getRentArrearsDetails().getDailyRate()).isEqualTo("£3.40");
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
        underTest.setCaseTabFields(pcsCase);

        // Then
        assertThat(pcsCase.getSummaryTab().getTenancyDetails().getAgreementType()).isEqualTo("Assured tenancy");
        assertThat(pcsCase.getSummaryTab().getTenancyDetails().getAgreementStartDate()).isNull();
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
        underTest.setCaseTabFields(pcsCase);

        // Then
        assertThat(pcsCase.getSummaryTab().getTenancyDetails().getAgreementType()).isEqualTo("Secure contract");
        assertThat(pcsCase.getSummaryTab().getTenancyDetails().getAgreementStartDate()).isEqualTo("12/05/2025");
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
        underTest.setCaseTabFields(pcsCase);

        // Then
        assertThat(pcsCase.getSummaryTab().getTenancyDetails().getAgreementType()).isEqualTo("Other Welsh licence");
        assertThat(pcsCase.getSummaryTab().getTenancyDetails().getAgreementStartDate()).isNull();
    }

    private static <T> ListValue<T> listValue(T value) {
        return ListValue.<T>builder()
            .value(value)
            .build();
    }
}
