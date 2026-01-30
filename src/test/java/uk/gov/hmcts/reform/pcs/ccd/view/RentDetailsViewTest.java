package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentPaymentFrequency;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.TenancyLicenceEntity;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RentDetailsViewTest {

    @Mock
    private PCSCase pcsCase;
    @Mock
    private PcsCaseEntity pcsCaseEntity;

    private RentDetailsView underTest;

    @BeforeEach
    void setUp() {
        underTest = new RentDetailsView();
    }

    @Test
    void shouldNotSetAnythingIfNoTenancyLicence() {
        // Given
        when(pcsCaseEntity.getTenancyLicence()).thenReturn(null);

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        verifyNoInteractions(pcsCase);
    }

    @Test
    void shouldSetRentDetailsFields() {
        // Given
        TenancyLicenceEntity tenancyLicenceEntity = mock(TenancyLicenceEntity.class);
        when(pcsCaseEntity.getTenancyLicence()).thenReturn(tenancyLicenceEntity);

        BigDecimal rentAmount = new BigDecimal("10.23");
        RentPaymentFrequency rentPaymentFrequency = RentPaymentFrequency.FORTNIGHTLY;
        BigDecimal dailyRent = new BigDecimal("4.56");
        String otherRentFrequency = "other rent frequency";

        when(tenancyLicenceEntity.getRentAmount()).thenReturn(rentAmount);
        when(tenancyLicenceEntity.getRentFrequency()).thenReturn(rentPaymentFrequency);
        when(tenancyLicenceEntity.getOtherRentFrequency()).thenReturn(otherRentFrequency);
        when(tenancyLicenceEntity.getRentPerDay()).thenReturn(dailyRent);

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        ArgumentCaptor<RentDetails> rentDetailsCaptor
            = ArgumentCaptor.forClass(RentDetails.class);

        verify(pcsCase).setRentDetails(rentDetailsCaptor.capture());

        RentDetails rentDetails = rentDetailsCaptor.getValue();
        assertThat(rentDetails.getCurrentRent()).isEqualTo(rentAmount);
        assertThat(rentDetails.getFrequency()).isEqualTo(rentPaymentFrequency);
        assertThat(rentDetails.getOtherFrequency()).isEqualTo(otherRentFrequency);
        assertThat(rentDetails.getDailyCharge()).isEqualTo(dailyRent);
    }

}
