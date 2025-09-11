package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentPaymentFrequency;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicence;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TenancyLicenceServiceTest {

    private final TenancyLicenceService tenancyLicenceService = new TenancyLicenceService();

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
    }

    private void assertTenancyLicenceField(java.util.function.Consumer<PCSCase> setupMock,
            java.util.function.Consumer<TenancyLicence> assertions) {
        PCSCase pcsCase = mock(PCSCase.class);
        setupMock.accept(pcsCase);
        TenancyLicence actual = tenancyLicenceService.buildTenancyLicence(pcsCase);
        assertions.accept(actual);
    }

    @Test
    void shouldUseAmendedDailyRentAmountWhenAvailable() {
        // Given
        PCSCase pcsCase = mock(PCSCase.class);
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
        PCSCase pcsCase = mock(PCSCase.class);
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
        PCSCase pcsCase = mock(PCSCase.class);
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
}
