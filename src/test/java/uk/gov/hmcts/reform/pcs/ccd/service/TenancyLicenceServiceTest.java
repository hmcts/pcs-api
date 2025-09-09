package uk.gov.hmcts.reform.pcs.ccd.service;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentPaymentFrequency;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicence;

class TenancyLicenceServiceTest {

    private final TenancyLicenceService tenancyLicenceService = new TenancyLicenceService();

    @Test
    void shouldSetTenancyLicence() {
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
