package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.CombinedLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceDetailsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceTypeWales;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.TenancyLicenceEntity;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenancyLicenceViewTest {

    @Mock
    private PCSCase pcsCase;
    @Mock
    private PcsCaseEntity pcsCaseEntity;

    private TenancyLicenceView underTest;

    @BeforeEach
    void setUp() {
        underTest = new TenancyLicenceView();
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
    void shouldSetTenancyLicenceFieldsForNonWales() {
        // Given
        TenancyLicenceEntity tenancyLicenceEntity = mock(TenancyLicenceEntity.class);
        when(pcsCaseEntity.getTenancyLicence()).thenReturn(tenancyLicenceEntity);

        String otherTypeDetails = "other type details";
        LocalDate tenancyStartDate = mock(LocalDate.class);

        when(tenancyLicenceEntity.getType()).thenReturn(CombinedLicenceType.SECURE_TENANCY);
        when(tenancyLicenceEntity.getOtherTypeDetails()).thenReturn(otherTypeDetails);
        when(tenancyLicenceEntity.getStartDate()).thenReturn(tenancyStartDate);

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        ArgumentCaptor<TenancyLicenceDetails> tenancyLicenceDetailsCaptor
            = ArgumentCaptor.forClass(TenancyLicenceDetails.class);

        verify(pcsCase).setTenancyLicenceDetails(tenancyLicenceDetailsCaptor.capture());
        verify(pcsCase, never()).setOccupationLicenceDetailsWales(any());

        TenancyLicenceDetails tenancyLicenceDetails = tenancyLicenceDetailsCaptor.getValue();
        assertThat(tenancyLicenceDetails.getTypeOfTenancyLicence()).isEqualTo(TenancyLicenceType.SECURE_TENANCY);
        assertThat(tenancyLicenceDetails.getDetailsOfOtherTypeOfTenancyLicence()).isEqualTo(otherTypeDetails);
        assertThat(tenancyLicenceDetails.getTenancyLicenceDate()).isEqualTo(tenancyStartDate);
    }

    @Test
    void shouldSetTenancyLicenceFieldsForWales() {
        // Given
        TenancyLicenceEntity tenancyLicenceEntity = mock(TenancyLicenceEntity.class);
        when(pcsCaseEntity.getTenancyLicence()).thenReturn(tenancyLicenceEntity);
        when(pcsCase.getLegislativeCountry()).thenReturn(LegislativeCountry.WALES);

        String otherTypeDetails = "other type details";
        LocalDate tenancyStartDate = mock(LocalDate.class);

        when(tenancyLicenceEntity.getType()).thenReturn(CombinedLicenceType.SECURE_CONTRACT);
        when(tenancyLicenceEntity.getOtherTypeDetails()).thenReturn(otherTypeDetails);
        when(tenancyLicenceEntity.getStartDate()).thenReturn(tenancyStartDate);

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        ArgumentCaptor<OccupationLicenceDetailsWales> occupationLicenceDetailsCaptor
            = ArgumentCaptor.forClass(OccupationLicenceDetailsWales.class);

        verify(pcsCase).setOccupationLicenceDetailsWales(occupationLicenceDetailsCaptor.capture());
        verify(pcsCase, never()).setTenancyLicenceDetails(any());

        OccupationLicenceDetailsWales occupationLicenceDetails = occupationLicenceDetailsCaptor.getValue();
        assertThat(occupationLicenceDetails.getOccupationLicenceTypeWales())
            .isEqualTo(OccupationLicenceTypeWales.SECURE_CONTRACT);
        assertThat(occupationLicenceDetails.getOtherLicenceTypeDetails()).isEqualTo(otherTypeDetails);
        assertThat(occupationLicenceDetails.getLicenceStartDate()).isEqualTo(tenancyStartDate);
    }

}
