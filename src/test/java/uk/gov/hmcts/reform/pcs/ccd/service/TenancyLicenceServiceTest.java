package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentPaymentFrequency;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicence;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.ThirdPartyPaymentSource;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenancyLicenceServiceTest {

    private final TenancyLicenceService tenancyLicenceService = new TenancyLicenceService();

    @Mock
    private PCSCase pcsCase;

    @Test
    void shouldSetTenancyLicence() {
        LocalDate tenancyDate = LocalDate.of(2025, 8, 27);

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
                    .extracting(d -> d.getFilename())
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
                    .extracting(d -> d.getFilename())
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
            pcsCase -> when(pcsCase.getNoticeDocuments()).thenReturn(noticeDocs),
            expected -> {
                assertThat(expected.getNoticeDocuments()).hasSize(2);
                assertThat(expected.getNoticeDocuments())
                    .extracting(d -> d.getFilename())
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
                pcsCase -> when(pcsCase.getCurrentRent()).thenReturn("120000"), // value in pence
                expected -> assertThat(expected.getRentAmount())
                        .isEqualTo(new BigDecimal("1200.00")));// value in pounds

        // Test rent payment frequency field
        assertTenancyLicenceField(
                pcsCase -> when(pcsCase.getRentFrequency()).thenReturn(RentPaymentFrequency.MONTHLY),
                expected -> assertThat(expected.getRentPaymentFrequency()).isEqualTo(RentPaymentFrequency.MONTHLY));

        // Test other rent frequency field
        assertTenancyLicenceField(
                pcsCase -> when(pcsCase.getOtherRentFrequency()).thenReturn("Bi-weekly"),
                expected -> assertThat(expected.getOtherRentFrequency()).isEqualTo("Bi-weekly"));

        // Test daily rent charge amount field
        assertTenancyLicenceField(
                pcsCase -> when(pcsCase.getDailyRentChargeAmount()).thenReturn("4000"),
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
                pcsCase -> when(pcsCase.getArrearsJudgmentWanted()).thenReturn(YesOrNo.YES),
                expected -> assertThat(expected.getArrearsJudgmentWanted()).isTrue());
        assertTenancyLicenceField(
                pcsCase -> when(pcsCase.getArrearsJudgmentWanted()).thenReturn(YesOrNo.NO),
                expected -> assertThat(expected.getArrearsJudgmentWanted()).isFalse());
    }

    private void assertTenancyLicenceField(java.util.function.Consumer<PCSCase> setupMock,
            java.util.function.Consumer<TenancyLicence> assertions) {
        setupMock.accept(pcsCase);
        TenancyLicence actual = tenancyLicenceService.buildTenancyLicence(pcsCase);
        assertions.accept(actual);
    }

    @Test
    void shouldUseAmendedDailyRentAmountWhenAvailable() {
        // Given
        when(pcsCase.getAmendedDailyRentChargeAmount()).thenReturn("5000");
        when(pcsCase.getCalculatedDailyRentChargeAmount()).thenReturn("4000");
        when(pcsCase.getDailyRentChargeAmount()).thenReturn("3500");
        when(pcsCase.getCurrentRent()).thenReturn("120000");
        when(pcsCase.getRentFrequency()).thenReturn(RentPaymentFrequency.MONTHLY);
        // When
        TenancyLicence result = tenancyLicenceService.buildTenancyLicence(pcsCase);
        // Then
        assertThat(result.getDailyRentChargeAmount()).isEqualTo(new BigDecimal("50.00"));
    }

    @Test
    void shouldUseCalculatedDailyRentAmountWhenAmendedNotAvailable() {
        // Given
        when(pcsCase.getAmendedDailyRentChargeAmount()).thenReturn(null);
        when(pcsCase.getCalculatedDailyRentChargeAmount()).thenReturn("4000");
        when(pcsCase.getDailyRentChargeAmount()).thenReturn("3500");
        when(pcsCase.getCurrentRent()).thenReturn("120000");
        when(pcsCase.getRentFrequency()).thenReturn(RentPaymentFrequency.MONTHLY);
        // When
        TenancyLicence result = tenancyLicenceService.buildTenancyLicence(pcsCase);
        // Then
        assertThat(result.getDailyRentChargeAmount()).isEqualTo(new BigDecimal("40.00"));
    }

    @Test
    void shouldUseDailyRentChargeAmountWhenOthersNotAvailable() {
        // Given
        when(pcsCase.getAmendedDailyRentChargeAmount()).thenReturn(null);
        when(pcsCase.getCalculatedDailyRentChargeAmount()).thenReturn(null);
        when(pcsCase.getDailyRentChargeAmount()).thenReturn("3500");
        when(pcsCase.getCurrentRent()).thenReturn("120000");
        when(pcsCase.getRentFrequency()).thenReturn(RentPaymentFrequency.MONTHLY);
        // When
        TenancyLicence result = tenancyLicenceService.buildTenancyLicence(pcsCase);
        // Then
        assertThat(result.getDailyRentChargeAmount()).isEqualTo(new BigDecimal("35.00"));
    }

    @Test
    void shouldHandleNullTotalRentArrears() {
        // Given
        when(pcsCase.getTotalRentArrears()).thenReturn(null);
        // When
        TenancyLicence result = tenancyLicenceService.buildTenancyLicence(pcsCase);
        // Then
        assertThat(result.getTotalRentArrears()).isNull();
    }

    @Test
    void shouldHandleEmptyThirdPartyPaymentSources() {
        // Given
        when(pcsCase.getThirdPartyPaymentSources()).thenReturn(Collections.emptyList());
        // When
        TenancyLicence result = tenancyLicenceService.buildTenancyLicence(pcsCase);
        // Then
        assertThat(result.getThirdPartyPaymentSources()).isEmpty();
    }

    @Test
    void shouldHandleNullThirdPartyPaymentSources() {
        // Given
        when(pcsCase.getThirdPartyPaymentSources()).thenReturn(null);
        // When
        TenancyLicence result = tenancyLicenceService.buildTenancyLicence(pcsCase);
        // Then
        assertThat(result.getThirdPartyPaymentSources()).isNull();
    }

    @Test
    void shouldHandleNullThirdPartyPaymentSourceOther() {
        // Given
        when(pcsCase.getThirdPartyPaymentSourceOther()).thenReturn(null);
        // When
        TenancyLicence result = tenancyLicenceService.buildTenancyLicence(pcsCase);
        // Then
        assertThat(result.getThirdPartyPaymentSourceOther()).isNull();
    }

    @Test
    void shouldHandleEmptyThirdPartyPaymentSourceOther() {
        // Given
        when(pcsCase.getThirdPartyPaymentSourceOther()).thenReturn("");
        // When
        TenancyLicence result = tenancyLicenceService.buildTenancyLicence(pcsCase);
        // Then
        assertThat(result.getThirdPartyPaymentSourceOther()).isEqualTo("");
    }

    @Test
    void shouldHandleNullRentStatementDocuments() {
        // Given
        when(pcsCase.getRentStatementDocuments()).thenReturn(null);
        // When
        TenancyLicence result = tenancyLicenceService.buildTenancyLicence(pcsCase);
        // Then
        assertThat(result.getRentStatementDocuments()).isEmpty();
    }

    @Test
    void shouldHandleEmptyRentStatementDocuments() {
        // Given
        when(pcsCase.getRentStatementDocuments()).thenReturn(Collections.emptyList());
        // When
        TenancyLicence result = tenancyLicenceService.buildTenancyLicence(pcsCase);
        // Then
        assertThat(result.getRentStatementDocuments()).isEmpty();
    }

    @Test
    void shouldHandleNullNoticeDocuments() {
        // Given
        when(pcsCase.getNoticeDocuments()).thenReturn(null);
        // When
        TenancyLicence result = tenancyLicenceService.buildTenancyLicence(pcsCase);
        // Then
        assertThat(result.getNoticeDocuments()).isEmpty();
    }

    @Test
    void shouldHandleEmptyNoticeDocuments() {
        // Given
        when(pcsCase.getNoticeDocuments()).thenReturn(Collections.emptyList());
        // When
        TenancyLicence result = tenancyLicenceService.buildTenancyLicence(pcsCase);
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

        when(pcsCase.getWalesHousingAct()).thenReturn(walesHousingAct);

        // When
        TenancyLicence result = tenancyLicenceService.buildTenancyLicence(pcsCase);

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
        when(pcsCase.getWalesHousingAct()).thenReturn(null);

        // When
        TenancyLicence result = tenancyLicenceService.buildTenancyLicence(pcsCase);

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

        when(pcsCase.getWalesHousingAct()).thenReturn(walesHousingAct);

        // When
        TenancyLicence result = tenancyLicenceService.buildTenancyLicence(pcsCase);

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
        when(pcsCase.getWalesNoticeDetails()).thenReturn(walesNoticeDetails);

        // When
        TenancyLicence result = tenancyLicenceService.buildTenancyLicence(pcsCase);

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
            .licenseType(OccupationLicenceTypeWales.SECURE_CONTRACT)
            .otherLicenseTypeDetails("Custom contract details")
            .licenseStartDate(licenseStartDate)
            .licenseDocuments(walesDocuments)
            .build();

        PCSCase pcsCase = PCSCase.builder()
            .occupationLicenceDetailsWales(walesDetails)
            .build();

        // When
        TenancyLicence result = tenancyLicenceService.buildTenancyLicence(pcsCase);

        // Then - Wales fields populated
        assertThat(result.getWalesOccupationLicenceType()).isEqualTo("Secure contract");
        assertThat(result.getWalesOtherLicenseTypeDetails()).isEqualTo("Custom contract details");
        assertThat(result.getWalesLicenseStartDate()).isEqualTo(licenseStartDate);
        assertThat(result.getWalesLicenseDocuments()).hasSize(2);
        assertThat(result.getWalesLicenseDocuments().get(0).getFilename()).isEqualTo("occupation_contract.pdf");
        assertThat(result.getWalesLicenseDocuments().get(1).getFilename()).isEqualTo("additional_doc.pdf");

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
            .licenseType(OccupationLicenceTypeWales.STANDARD_CONTRACT)
            .licenseStartDate(licenseStartDate)
            .licenseDocuments(null)
            .build();

        PCSCase pcsCase = PCSCase.builder()
            .occupationLicenceDetailsWales(walesDetails)
            .build();

        // When
        TenancyLicence result = tenancyLicenceService.buildTenancyLicence(pcsCase);

        // Then
        assertThat(result.getWalesOccupationLicenceType()).isEqualTo("Standard contract");
        assertThat(result.getWalesLicenseStartDate()).isEqualTo(licenseStartDate);
        assertThat(result.getWalesOtherLicenseTypeDetails()).isNull();
        assertThat(result.getWalesLicenseDocuments()).isEmpty();
    }

    @Test
    void shouldHandleNullOccupationLicenceDetailsWales() {
        // Given - Case with no Wales occupation contract details
        PCSCase pcsCase = PCSCase.builder()
            .occupationLicenceDetailsWales(null)
            .build();

        // When
        TenancyLicence result = tenancyLicenceService.buildTenancyLicence(pcsCase);

        // Then - Wales fields should be null
        assertThat(result.getWalesOccupationLicenceType()).isNull();
        assertThat(result.getWalesOtherLicenseTypeDetails()).isNull();
        assertThat(result.getWalesLicenseStartDate()).isNull();
        assertThat(result.getWalesLicenseDocuments()).isNull();
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

        when(pcsCase.getTypeOfTenancyLicence()).thenReturn(TenancyLicenceType.ASSURED_TENANCY);
        when(pcsCase.getTenancyLicenceDate()).thenReturn(tenancyDate);
        when(pcsCase.getTenancyLicenceDocuments()).thenReturn(englandDocs);
        when(pcsCase.getOccupationLicenceDetailsWales()).thenReturn(null);

        // When
        TenancyLicence result = tenancyLicenceService.buildTenancyLicence(pcsCase);

        // Then - England fields populated
        assertThat(result.getTenancyLicenceType()).isEqualTo("Assured tenancy");
        assertThat(result.getTenancyLicenceDate()).isEqualTo(tenancyDate);
        assertThat(result.getSupportingDocuments()).hasSize(1);

        // Wales occupation contract fields should be null
        assertThat(result.getWalesOccupationLicenceType()).isNull();
        assertThat(result.getWalesLicenseStartDate()).isNull();
        assertThat(result.getWalesLicenseDocuments()).isNull();
    }
}
