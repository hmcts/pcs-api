package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared.AdditionalDefendantInformationTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared.ClaimantInformationTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared.DefendantInformationTabDetails;
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
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
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

    @InjectMocks
    private CaseSummaryTabView underTest;

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

        when(groundsBuilder.getGrounds(pcsCase)).thenReturn(
            "Rent arrears (ground 10)\n"
            + "Antisocial behaviour: Condition 1 of Section 84A of the Housing Act 1985"
        );

        when(rentArrearsTabDetailsBuilder.buildRentArrearsTabDetails(pcsCase)).thenReturn(
            RentArrearsTabDetails.builder()
                .rentAmount("£100")
                .calculationFrequency("Every 4 weeks")
                .dailyRate("£12.30")
                .arrearsTotal("£450.75")
                .judgmentRequested("Yes")
                .build()
        );

        when(reasonsForPossessionTabDetailsBuilder.buildSummaryReasonsForPossession(pcsCase)).thenReturn(
            ReasonsForPossessionTabDetails.builder()
                .ground10("Ground 10 reason")
                .condition1OfSection84A("Condition 1 reason")
                .additionalReasonsForPossession("Additional reasons")
                .build()
        );

        when(claimantInformationTabDetailsBuilder.createSummaryClaimantTabDetails(pcsCase)).thenReturn(
            ClaimantInformationTabDetails.builder()
                .claimantName("Fallback claimant")
                .build()
        );

        when(defendantInformationTabDetailsBuilder.buildSummaryDefendantOneDetails(pcsCase)).thenReturn(
            DefendantInformationTabDetails.builder()
                .firstName("Defendant")
                .lastName("One")
                .addressForService(propertyAddress)
                .build()
        );

        when(additionalDefendantInformationTabDetailsBuilder.buildSummaryAdditionalDefendantsDetails(pcsCase))
            .thenReturn(
                List.of(
                    listValue(
                        AdditionalDefendantInformationTabDetails.builder()
                            .firstName("Defendant")
                            .lastName("Two")
                            .addressForService(defendantAddress)
                            .build()
                    ),
                    listValue(
                        AdditionalDefendantInformationTabDetails.builder()
                            .firstName(CaseTabView.NAME_UNKNOWN)
                            .lastName(CaseTabView.NAME_UNKNOWN)
                            .addressForService(propertyAddress)
                            .build()
                    )
                )
            );

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

        when(groundsBuilder.getGrounds(pcsCase)).thenReturn(null);
        when(rentArrearsTabDetailsBuilder.buildRentArrearsTabDetails(pcsCase)).thenReturn(null);
        when(reasonsForPossessionTabDetailsBuilder.buildSummaryReasonsForPossession(pcsCase)).thenReturn(null);
        when(claimantInformationTabDetailsBuilder.createSummaryClaimantTabDetails(pcsCase)).thenReturn(null);
        when(defendantInformationTabDetailsBuilder.buildSummaryDefendantOneDetails(pcsCase)).thenReturn(null);
        when(additionalDefendantInformationTabDetailsBuilder.buildSummaryAdditionalDefendantsDetails(pcsCase))
            .thenReturn(null);

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
    void shouldNotSetEmptySummarySections() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .allDefendants(List.of(listValue(Party.builder()
                                                .nameKnown(VerticalYesNo.NO)
                                                .addressKnown(VerticalYesNo.NO)
                                                .build())))
            .build();

        when(groundsBuilder.getGrounds(pcsCase)).thenReturn(null);
        when(rentArrearsTabDetailsBuilder.buildRentArrearsTabDetails(pcsCase)).thenReturn(null);
        when(reasonsForPossessionTabDetailsBuilder.buildSummaryReasonsForPossession(pcsCase)).thenReturn(null);
        when(claimantInformationTabDetailsBuilder.createSummaryClaimantTabDetails(pcsCase)).thenReturn(null);
        when(defendantInformationTabDetailsBuilder.buildSummaryDefendantOneDetails(pcsCase)).thenReturn(null);
        when(additionalDefendantInformationTabDetailsBuilder.buildSummaryAdditionalDefendantsDetails(pcsCase))
            .thenReturn(null);

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
        assertThat(summaryTab.getOccupationContractOrLicenceDetails()).isNull();
        assertThat(summaryTab.getNoticeDetails()).isNull();
    }

    @Test
    void shouldSetTenancyDetailsFromTenancyLicenceTypeLabel() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .tenancyLicenceDetails(TenancyLicenceDetails.builder()
                                       .typeOfTenancyLicence(TenancyLicenceType.ASSURED_TENANCY)
                                       .build())
            .build();

        when(groundsBuilder.getGrounds(pcsCase)).thenReturn(null);
        when(rentArrearsTabDetailsBuilder.buildRentArrearsTabDetails(pcsCase)).thenReturn(null);
        when(reasonsForPossessionTabDetailsBuilder.buildSummaryReasonsForPossession(pcsCase)).thenReturn(null);
        when(claimantInformationTabDetailsBuilder.createSummaryClaimantTabDetails(pcsCase)).thenReturn(null);
        when(defendantInformationTabDetailsBuilder.buildSummaryDefendantOneDetails(pcsCase)).thenReturn(null);
        when(additionalDefendantInformationTabDetailsBuilder.buildSummaryAdditionalDefendantsDetails(pcsCase))
            .thenReturn(null);

        // When
        SummaryTab summaryTab = underTest.buildSummaryTab(pcsCase);

        // Then
        assertThat(summaryTab.getTenancyDetails().getAgreementType()).isEqualTo("Assured tenancy");
        assertThat(summaryTab.getTenancyDetails().getAgreementStartDate()).isNull();
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
            .legislativeCountry(LegislativeCountry.WALES)
            .build();

        // When
        SummaryTab summaryTab = underTest.buildSummaryTab(pcsCase);

        // Then
        assertThat(summaryTab.getTenancyDetails()).isNull();
        assertThat(summaryTab.getOccupationContractOrLicenceDetails()).isNull();
    }

    @ParameterizedTest
    @MethodSource("walesOccupationLicenceTypeLabelScenarios")
    void shouldSetOccupationContractOrLicenceDetailsFromWalesOccupationLicenceTypeLabel(
        TenancyLicenceDetails tenancyLicenceDetails,
        OccupationLicenceTypeWales occupationLicenceType,
        String expectedAgreementType
    ) {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .tenancyLicenceDetails(tenancyLicenceDetails)
            .occupationLicenceDetailsWales(OccupationLicenceDetailsWales.builder()
                                               .occupationLicenceTypeWales(occupationLicenceType)
                                               .licenceStartDate(LocalDate.of(2025, 5, 12))
                                               .build())
            .legislativeCountry(LegislativeCountry.WALES)
            .build();

        when(groundsBuilder.getGrounds(pcsCase)).thenReturn(null);
        when(rentArrearsTabDetailsBuilder.buildRentArrearsTabDetails(pcsCase)).thenReturn(null);
        when(reasonsForPossessionTabDetailsBuilder.buildSummaryReasonsForPossession(pcsCase)).thenReturn(null);
        when(claimantInformationTabDetailsBuilder.createSummaryClaimantTabDetails(pcsCase)).thenReturn(null);
        when(defendantInformationTabDetailsBuilder.buildSummaryDefendantOneDetails(pcsCase)).thenReturn(null);
        when(additionalDefendantInformationTabDetailsBuilder.buildSummaryAdditionalDefendantsDetails(pcsCase))
            .thenReturn(null);

        // When
        SummaryTab summaryTab = underTest.buildSummaryTab(pcsCase);

        // Then
        assertThat(summaryTab.getTenancyDetails()).isNull();
        assertThat(summaryTab.getOccupationContractOrLicenceDetails().getAgreementType())
            .isEqualTo(expectedAgreementType);
        assertThat(summaryTab.getOccupationContractOrLicenceDetails().getAgreementStartDate()).isEqualTo("12/05/2025");
    }

    @Test
    void shouldSetOccupationContractOrLicenceDetailsFromWalesOtherOccupationLicenceType() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .occupationLicenceDetailsWales(OccupationLicenceDetailsWales.builder()
                                               .occupationLicenceTypeWales(OccupationLicenceTypeWales.OTHER)
                                               .otherLicenceTypeDetails("Other Welsh licence")
                                               .build())
            .legislativeCountry(LegislativeCountry.WALES)
            .build();

        when(groundsBuilder.getGrounds(pcsCase)).thenReturn(null);
        when(rentArrearsTabDetailsBuilder.buildRentArrearsTabDetails(pcsCase)).thenReturn(null);
        when(reasonsForPossessionTabDetailsBuilder.buildSummaryReasonsForPossession(pcsCase)).thenReturn(null);
        when(claimantInformationTabDetailsBuilder.createSummaryClaimantTabDetails(pcsCase)).thenReturn(null);
        when(defendantInformationTabDetailsBuilder.buildSummaryDefendantOneDetails(pcsCase)).thenReturn(null);
        when(additionalDefendantInformationTabDetailsBuilder.buildSummaryAdditionalDefendantsDetails(pcsCase))
            .thenReturn(null);

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
