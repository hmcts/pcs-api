package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServedDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentSection;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentPaymentFrequency;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicence;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.ThirdPartyPaymentSource;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.WalesHousingAct;
import uk.gov.hmcts.reform.pcs.ccd.domain.WalesNoticeDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotApplicable;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceDetailsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceTypeWales;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenancyLicenceServiceTest {

    private final TenancyLicenceService tenancyLicenceService = new TenancyLicenceService();

    @Mock
    private PCSCase pcsCaseMock;

    @Mock
    private NoticeServedDetails noticeServedDetails;

    @Test
    void shouldSetTenancyLicence() {
        LocalDate tenancyDate = LocalDate.of(2025, 8, 27);
        when(pcsCaseMock.getNoticeServedDetails()).thenReturn(noticeServedDetails);

        List<ListValue<Document>> uploadedDocs = Arrays.asList(
            ListValue.<Document>builder().id("1")
                .value(Document.builder()
                           .filename("tenancy_agreement.pdf")
                           .build())
                .build(),
            ListValue.<Document>builder().id("2")
                .value(Document.builder()
                           .filename("proof_of_id.png")
                           .build())
                .build()
        );

        // Test tenancy type field
        assertTenancyLicenceField(
            pcsCase -> when(pcsCase.getTypeOfTenancyLicence()).thenReturn(TenancyLicenceType.ASSURED_TENANCY),
            expected -> assertThat(expected.getTenancyLicenceType())
                .isEqualTo(TenancyLicenceType.ASSURED_TENANCY.getLabel()));

        // Test tenancy date field
        assertTenancyLicenceField(
            pcsCase -> when(pcsCase.getTenancyLicenceDate()).thenReturn(tenancyDate),
            expected -> assertThat(expected.getTenancyLicenceDate()).isEqualTo(tenancyDate));

        //Test supporting documents field
        assertTenancyLicenceField(
            pcsCase -> when(pcsCase.getTenancyLicenceDocuments()).thenReturn(uploadedDocs),
            expected -> {
                assertThat(expected.getSupportingDocuments()).hasSize(2);
                assertThat(expected.getSupportingDocuments())
                    .extracting(Document::getFilename)
                    .containsExactlyInAnyOrder("tenancy_agreement.pdf", "proof_of_id.png");
            }
        );

        // Test rent statement documents field
        List<ListValue<Document>> rentStatementDocs = Arrays.asList(
            ListValue.<Document>builder().id("10")
                .value(Document.builder().filename("rent_statement_jan.pdf").build()).build(),
            ListValue.<Document>builder().id("11")
                .value(Document.builder().filename("rent_statement_feb.pdf").build()).build()
        );
        assertTenancyLicenceField(
            pcsCase -> when(pcsCase.getRentStatementDocuments()).thenReturn(rentStatementDocs),
            expected -> {
                assertThat(expected.getRentStatementDocuments()).hasSize(2);
                assertThat(expected.getRentStatementDocuments())
                    .extracting(Document::getFilename)
                    .containsExactlyInAnyOrder("rent_statement_jan.pdf", "rent_statement_feb.pdf");
            }
        );

        // Test notice documents field
        List<ListValue<Document>> noticeDocs = Arrays.asList(
            ListValue.<Document>builder().id("20")
                .value(Document.builder().filename("notice_served.pdf").build()).build(),
            ListValue.<Document>builder().id("21")
                .value(Document.builder().filename("certificate_service.pdf").build()).build()
        );
        assertTenancyLicenceField(
            pcsCase -> {
                when(pcsCase.getNoticeServedDetails()).thenReturn(noticeServedDetails);
                when(noticeServedDetails.getNoticeDocuments()).thenReturn(noticeDocs);
            },
            expected -> {
                assertThat(expected.getNoticeDocuments()).hasSize(2);
                assertThat(expected.getNoticeDocuments())
                    .extracting(Document::getFilename)
                    .containsExactlyInAnyOrder("notice_served.pdf", "certificate_service.pdf");
            }
        );

        // Test notice_served field updates
        assertTenancyLicenceField(
                pcsCase -> when(pcsCase.getNoticeServed()).thenReturn(YesOrNo.YES),
                expected -> assertThat(expected.getNoticeServed()).isTrue());
        assertTenancyLicenceField(
                pcsCase -> when(pcsCase.getNoticeServed()).thenReturn(YesOrNo.NO),
                expected -> assertThat(expected.getNoticeServed()).isFalse());

        // Test rent amount field
        assertTenancyLicenceField(
                pcsCase -> when(pcsCase.getRentSection()).thenReturn(RentSection.builder()
                        .currentRent("120000") // value in pence
                        .build()),
                expected -> assertThat(expected.getRentAmount())
                        .isEqualTo(new BigDecimal("1200.00")));// value in pounds

        // Test rent payment frequency field
        assertTenancyLicenceField(
                pcsCase -> when(pcsCase.getRentSection()).thenReturn(RentSection.builder()
                        .rentFrequency(RentPaymentFrequency.MONTHLY)
                        .build()),
                expected -> assertThat(expected.getRentPaymentFrequency()).isEqualTo(RentPaymentFrequency.MONTHLY));

        // Test other rent frequency field
        assertTenancyLicenceField(
                pcsCase -> when(pcsCase.getRentSection()).thenReturn(RentSection.builder()
                        .otherRentFrequency("Bi-weekly")
                        .build()),
                expected -> assertThat(expected.getOtherRentFrequency()).isEqualTo("Bi-weekly"));

        // Test daily rent charge amount field
        assertTenancyLicenceField(
                pcsCase -> when(pcsCase.getRentSection()).thenReturn(RentSection.builder()
                        .dailyRentCharge("4000")
                        .build()),
                expected -> assertThat(expected.getDailyRentChargeAmount()).isEqualTo(new BigDecimal("40.00")));

        // Test total rent arrears field
        assertTenancyLicenceField(
                pcsCase -> when(pcsCase.getTotalRentArrears()).thenReturn("150000"), // value in pence
                expected -> assertThat(expected.getTotalRentArrears())
                        .isEqualTo(new BigDecimal("1500.00"))); // value in pounds

        // Test third party payment sources field
        assertTenancyLicenceField(
                pcsCase -> when(pcsCase.getThirdPartyPaymentSources()).thenReturn(
                        Arrays.asList(ThirdPartyPaymentSource.UNIVERSAL_CREDIT,
                                ThirdPartyPaymentSource.HOUSING_BENEFIT)),
                expected -> {
                    assertThat(expected.getThirdPartyPaymentSources()).hasSize(2);
                    assertThat(expected.getThirdPartyPaymentSources())
                            .containsExactlyInAnyOrder(ThirdPartyPaymentSource.UNIVERSAL_CREDIT,
                                    ThirdPartyPaymentSource.HOUSING_BENEFIT);
                });

        // Test third party payment source other field
        assertTenancyLicenceField(
                pcsCase -> when(pcsCase.getThirdPartyPaymentSourceOther()).thenReturn("Custom payment method"),
                expected -> assertThat(expected.getThirdPartyPaymentSourceOther()).isEqualTo("Custom payment method"));

        // Test arrearsJudgmentWanted field updates
        assertTenancyLicenceField(
                pcsCase -> when(pcsCase.getArrearsJudgmentWanted()).thenReturn(VerticalYesNo.YES),
                expected -> assertThat(expected.getArrearsJudgmentWanted()).isTrue());
        assertTenancyLicenceField(
                pcsCase -> when(pcsCase.getArrearsJudgmentWanted()).thenReturn(VerticalYesNo.NO),
                expected -> assertThat(expected.getArrearsJudgmentWanted()).isFalse());
    }

    private void assertTenancyLicenceField(java.util.function.Consumer<PCSCase> setupMock,
            java.util.function.Consumer<TenancyLicence> assertions) {
        setupMock.accept(pcsCaseMock);
        TenancyLicence actual = tenancyLicenceService.buildTenancyLicence(pcsCaseMock);
        assertions.accept(actual);
    }

    @ParameterizedTest(name = "amended={0}, calculated={1}, daily={2} -> expected={3}")
    @MethodSource("dailyRentChargeScenarios")
    void shouldPreferDailyRentCharge(String amendedDailyRent, String calculatedDailyRent, String dailyRent, String expectedAmount) {
        when(pcsCaseMock.getNoticeServedDetails()).thenReturn(noticeServedDetails);
        when(pcsCaseMock.getRentSection()).thenReturn(RentSection.builder()
                .amendedDailyRentCharge(amendedDailyRent)
                .calculatedDailyRentCharge(calculatedDailyRent)
                .dailyRentCharge(dailyRent)
                .currentRent("120000")
                .rentFrequency(RentPaymentFrequency.MONTHLY)
                .build());

        TenancyLicence result = tenancyLicenceService.buildTenancyLicence(pcsCaseMock);
        assertThat(result.getDailyRentChargeAmount()).isEqualTo(new BigDecimal(expectedAmount));
    }

    @Test
    void shouldHandleNullTotalRentArrears() {
        // Given
        when(pcsCaseMock.getNoticeServedDetails()).thenReturn(noticeServedDetails);
        when(pcsCaseMock.getTotalRentArrears()).thenReturn(null);
        // When
        TenancyLicence result = tenancyLicenceService.buildTenancyLicence(pcsCaseMock);
        // Then
        assertThat(result.getTotalRentArrears()).isNull();
    }

    @Test
    void shouldHandleEmptyThirdPartyPaymentSources() {
        // Given
        when(pcsCaseMock.getNoticeServedDetails()).thenReturn(noticeServedDetails);
        when(pcsCaseMock.getThirdPartyPaymentSources()).thenReturn(Collections.emptyList());
        // When
        TenancyLicence result = tenancyLicenceService.buildTenancyLicence(pcsCaseMock);
        // Then
        assertThat(result.getThirdPartyPaymentSources()).isEmpty();
    }

    @Test
    void shouldHandleNullThirdPartyPaymentSources() {
        // Given
        when(pcsCaseMock.getNoticeServedDetails()).thenReturn(noticeServedDetails);
        when(pcsCaseMock.getThirdPartyPaymentSources()).thenReturn(null);
        // When
        TenancyLicence result = tenancyLicenceService.buildTenancyLicence(pcsCaseMock);
        // Then
        assertThat(result.getThirdPartyPaymentSources()).isNull();
    }

    @Test
    void shouldHandleNullThirdPartyPaymentSourceOther() {
        // Given
        when(pcsCaseMock.getNoticeServedDetails()).thenReturn(noticeServedDetails);
        when(pcsCaseMock.getThirdPartyPaymentSourceOther()).thenReturn(null);
        // When
        TenancyLicence result = tenancyLicenceService.buildTenancyLicence(pcsCaseMock);
        // Then
        assertThat(result.getThirdPartyPaymentSourceOther()).isNull();
    }

    @Test
    void shouldHandleEmptyThirdPartyPaymentSourceOther() {
        // Given
        when(pcsCaseMock.getNoticeServedDetails()).thenReturn(noticeServedDetails);
        when(pcsCaseMock.getThirdPartyPaymentSourceOther()).thenReturn("");
        // When
        TenancyLicence result = tenancyLicenceService.buildTenancyLicence(pcsCaseMock);
        // Then
        assertThat(result.getThirdPartyPaymentSourceOther()).isEqualTo("");
    }

    @Test
    void shouldHandleNullRentStatementDocuments() {
        // Given
        when(pcsCaseMock.getNoticeServedDetails()).thenReturn(noticeServedDetails);
        when(pcsCaseMock.getRentStatementDocuments()).thenReturn(null);
        // When
        TenancyLicence result = tenancyLicenceService.buildTenancyLicence(pcsCaseMock);
        // Then
        assertThat(result.getRentStatementDocuments()).isEmpty();
    }

    @Test
    void shouldHandleEmptyRentStatementDocuments() {
        // Given
        when(pcsCaseMock.getNoticeServedDetails()).thenReturn(noticeServedDetails);
        when(pcsCaseMock.getRentStatementDocuments()).thenReturn(Collections.emptyList());
        // When
        TenancyLicence result = tenancyLicenceService.buildTenancyLicence(pcsCaseMock);
        // Then
        assertThat(result.getRentStatementDocuments()).isEmpty();
    }

    @Test
    void shouldHandleNullNoticeDocuments() {
        // Given
        when(pcsCaseMock.getNoticeServedDetails()).thenReturn(noticeServedDetails);
        when(noticeServedDetails.getNoticeDocuments()).thenReturn(null);
        // When
        TenancyLicence result = tenancyLicenceService.buildTenancyLicence(pcsCaseMock);
        // Then
        assertThat(result.getNoticeDocuments()).isEmpty();
    }

    @Test
    void shouldHandleEmptyNoticeDocuments() {
        // Given
        when(pcsCaseMock.getNoticeServedDetails()).thenReturn(noticeServedDetails);
        when(noticeServedDetails.getNoticeDocuments()).thenReturn(Collections.emptyList());
        // When
        TenancyLicence result = tenancyLicenceService.buildTenancyLicence(pcsCaseMock);
        // Then
        assertThat(result.getNoticeDocuments()).isEmpty();
    }

    @Test
    void shouldMapWalesHousingActDetailsWhenPresent() {
        // Given
        LocalDate appointmentDate = LocalDate.of(2024, 3, 15);
        WalesHousingAct walesHousingAct = WalesHousingAct.builder()
            .registered(YesNoNotApplicable.YES)
            .registrationNumber("REG123456")
            .licensed(YesNoNotApplicable.YES)
            .licenceNumber("LIC789012")
            .licensedAgentAppointed(YesNoNotApplicable.YES)
            .agentFirstName("John")
            .agentLastName("Smith")
            .agentLicenceNumber("AGENT345678")
            .agentAppointmentDate(appointmentDate)
            .build();

        when(pcsCaseMock.getNoticeServedDetails()).thenReturn(noticeServedDetails);
        when(pcsCaseMock.getWalesHousingAct()).thenReturn(walesHousingAct);

        // When
        TenancyLicence result = tenancyLicenceService.buildTenancyLicence(pcsCaseMock);

        // Then
        assertThat(result.getWalesRegistered()).isEqualTo(YesNoNotApplicable.YES);
        assertThat(result.getWalesRegistrationNumber()).isEqualTo("REG123456");
        assertThat(result.getWalesLicensed()).isEqualTo(YesNoNotApplicable.YES);
        assertThat(result.getWalesLicenceNumber()).isEqualTo("LIC789012");
        assertThat(result.getWalesLicensedAgentAppointed()).isEqualTo(YesNoNotApplicable.YES);
        assertThat(result.getWalesAgentFirstName()).isEqualTo("John");
        assertThat(result.getWalesAgentLastName()).isEqualTo("Smith");
        assertThat(result.getWalesAgentLicenceNumber()).isEqualTo("AGENT345678");
        assertThat(result.getWalesAgentAppointmentDate()).isEqualTo(appointmentDate);
    }

    @Test
    void shouldHandleNullWalesHousingActDetails() {
        // Given
        when(pcsCaseMock.getNoticeServedDetails()).thenReturn(noticeServedDetails);
        when(pcsCaseMock.getWalesHousingAct()).thenReturn(null);

        // When
        TenancyLicence result = tenancyLicenceService.buildTenancyLicence(pcsCaseMock);

        // Then
        assertThat(result.getWalesRegistered()).isNull();
        assertThat(result.getWalesRegistrationNumber()).isNull();
        assertThat(result.getWalesLicensed()).isNull();
        assertThat(result.getWalesLicenceNumber()).isNull();
        assertThat(result.getWalesLicensedAgentAppointed()).isNull();
        assertThat(result.getWalesAgentFirstName()).isNull();
        assertThat(result.getWalesAgentLastName()).isNull();
        assertThat(result.getWalesAgentLicenceNumber()).isNull();
        assertThat(result.getWalesAgentAppointmentDate()).isNull();
    }

    @Test
    void shouldHandleWalesHousingActDetailsWithNotApplicableValues() {
        // Given
        WalesHousingAct walesHousingAct = WalesHousingAct.builder()
            .registered(YesNoNotApplicable.NOT_APPLICABLE)
            .licensed(YesNoNotApplicable.NO)
            .licensedAgentAppointed(YesNoNotApplicable.NOT_APPLICABLE)
            .build();

        when(pcsCaseMock.getNoticeServedDetails()).thenReturn(noticeServedDetails);
        when(pcsCaseMock.getWalesHousingAct()).thenReturn(walesHousingAct);

        // When
        TenancyLicence result = tenancyLicenceService.buildTenancyLicence(pcsCaseMock);

        // Then
        assertThat(result.getWalesRegistered()).isEqualTo(YesNoNotApplicable.NOT_APPLICABLE);
        assertThat(result.getWalesLicensed()).isEqualTo(YesNoNotApplicable.NO);
        assertThat(result.getWalesLicensedAgentAppointed()).isEqualTo(YesNoNotApplicable.NOT_APPLICABLE);
    }

    @Test
    void shouldMapWalesNoticeFieldsWhenPresent() {
        // Given
        String typeOfNoticeServed = "Some notice type";

        WalesNoticeDetails walesNoticeDetails = WalesNoticeDetails.builder()
            .noticeServed(YesOrNo.YES)
            .typeOfNoticeServed(typeOfNoticeServed)
            .build();
        when(pcsCaseMock.getNoticeServedDetails()).thenReturn(noticeServedDetails);
        when(pcsCaseMock.getWalesNoticeDetails()).thenReturn(walesNoticeDetails);

        // When
        TenancyLicence result = tenancyLicenceService.buildTenancyLicence(pcsCaseMock);

        // Then
        assertThat(result.getWalesNoticeServed()).isTrue();
        assertThat(result.getWalesTypeOfNoticeServed()).isEqualTo(typeOfNoticeServed);
    }

    @Test
    void shouldMapWalesOccupationContractDetailsToTenancyLicence() {
        // Given - Wales case with occupation contract details
        LocalDate licenseStartDate = LocalDate.of(2024, 1, 15);
        List<ListValue<Document>> walesDocuments = Arrays.asList(
            ListValue.<Document>builder().id("1")
                .value(Document.builder()
                    .filename("occupation_contract.pdf")
                    .url("http://doc.com/occupation_contract.pdf")
                    .build())
                .build(),
            ListValue.<Document>builder().id("2")
                .value(Document.builder()
                    .filename("additional_doc.pdf")
                    .url("http://doc.com/additional_doc.pdf")
                    .build())
                .build()
        );

        OccupationLicenceDetailsWales walesDetails = OccupationLicenceDetailsWales.builder()
            .occupationLicenceTypeWales(OccupationLicenceTypeWales.SECURE_CONTRACT)
            .otherLicenceTypeDetails("Custom contract details")
            .licenceStartDate(licenseStartDate)
            .licenceDocuments(walesDocuments)
            .build();

        PCSCase pcsCase = PCSCase.builder()
            .occupationLicenceDetailsWales(walesDetails)
            .noticeServedDetails(noticeServedDetails)
            .build();

        // When
        TenancyLicence result = tenancyLicenceService.buildTenancyLicence(pcsCase);

        // Then - Wales fields populated
        assertThat(result.getOccupationLicenceTypeWales()).isEqualTo(OccupationLicenceTypeWales.SECURE_CONTRACT);
        assertThat(result.getWalesOtherLicenceTypeDetails()).isEqualTo("Custom contract details");
        assertThat(result.getWalesLicenceStartDate()).isEqualTo(licenseStartDate);
        assertThat(result.getWalesLicenceDocuments()).hasSize(2);
        assertThat(result.getWalesLicenceDocuments().get(0).getFilename()).isEqualTo("occupation_contract.pdf");
        assertThat(result.getWalesLicenceDocuments().get(1).getFilename()).isEqualTo("additional_doc.pdf");

        // England fields should be null or empty
        assertThat(result.getTenancyLicenceType()).isNull();
        assertThat(result.getTenancyLicenceDate()).isNull();
        assertThat(result.getSupportingDocuments()).isEmpty();
    }

    @Test
    void shouldMapWalesOccupationContractDetailsWithStandardContract() {
        // Given - Wales case with standard contract
        LocalDate licenseStartDate = LocalDate.of(2023, 6, 1);

        OccupationLicenceDetailsWales walesDetails = OccupationLicenceDetailsWales.builder()
            .occupationLicenceTypeWales(OccupationLicenceTypeWales.STANDARD_CONTRACT)
            .licenceStartDate(licenseStartDate)
            .licenceDocuments(null)
            .build();

        PCSCase pcsCase = PCSCase.builder()
            .occupationLicenceDetailsWales(walesDetails)
            .noticeServedDetails(noticeServedDetails)
            .build();

        // When
        TenancyLicence result = tenancyLicenceService.buildTenancyLicence(pcsCase);

        // Then
        assertThat(result.getOccupationLicenceTypeWales()).isEqualTo(OccupationLicenceTypeWales.STANDARD_CONTRACT);
        assertThat(result.getWalesLicenceStartDate()).isEqualTo(licenseStartDate);
        assertThat(result.getWalesOtherLicenceTypeDetails()).isNull();
        assertThat(result.getWalesLicenceDocuments()).isEmpty();
    }

    @Test
    void shouldHandleNullOccupationLicenceDetailsWales() {
        // Given - Case with no Wales occupation contract details
        PCSCase pcsCase = PCSCase.builder()
            .occupationLicenceDetailsWales(null)
            .noticeServedDetails(noticeServedDetails)
            .build();

        // When
        TenancyLicence result = tenancyLicenceService.buildTenancyLicence(pcsCase);

        // Then - Wales fields should be null
        assertThat(result.getOccupationLicenceTypeWales()).isNull();
        assertThat(result.getWalesOtherLicenceTypeDetails()).isNull();
        assertThat(result.getWalesLicenceStartDate()).isNull();
        assertThat(result.getWalesLicenceDocuments()).isNull();
    }

    @Test
    void shouldMapEnglandTenancyDetailsWithoutWalesData() {
        // Given - England case (no Wales data)
        LocalDate tenancyDate = LocalDate.of(2024, 2, 20);
        List<ListValue<Document>> englandDocs = List.of(
            ListValue.<Document>builder().id("1")
                .value(Document.builder()
                    .filename("tenancy_agreement.pdf")
                    .url("http://doc.com/tenancy_agreement.pdf")
                    .build())
                .build()
        );

        when(pcsCaseMock.getNoticeServedDetails()).thenReturn(noticeServedDetails);
        when(pcsCaseMock.getTypeOfTenancyLicence()).thenReturn(TenancyLicenceType.ASSURED_TENANCY);
        when(pcsCaseMock.getTenancyLicenceDate()).thenReturn(tenancyDate);
        when(pcsCaseMock.getTenancyLicenceDocuments()).thenReturn(englandDocs);
        when(pcsCaseMock.getOccupationLicenceDetailsWales()).thenReturn(null);

        // When
        TenancyLicence result = tenancyLicenceService.buildTenancyLicence(pcsCaseMock);

        // Then - England fields populated
        assertThat(result.getTenancyLicenceType()).isEqualTo("Assured tenancy");
        assertThat(result.getTenancyLicenceDate()).isEqualTo(tenancyDate);
        assertThat(result.getSupportingDocuments()).hasSize(1);

        // Wales occupation contract fields should be null
        assertThat(result.getOccupationLicenceTypeWales()).isNull();
        assertThat(result.getWalesLicenceStartDate()).isNull();
        assertThat(result.getWalesLicenceDocuments()).isNull();
    }

    static Stream<Arguments> dailyRentChargeScenarios() {
        return Stream.of(
                Arguments.of("5000", "4000", "3500", "50.00"),
                Arguments.of(null, "4000", "3500", "40.00"),
                Arguments.of(null, null, "3500", "35.00")
        );
    }
}
