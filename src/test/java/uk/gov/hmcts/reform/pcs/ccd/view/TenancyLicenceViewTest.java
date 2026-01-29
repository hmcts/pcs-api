package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.TenancyLicenceEntity;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
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
    void shouldSetTenancyLicenceFields() {
        // Given
        TenancyLicenceEntity tenancyLicenceEntity = mock(TenancyLicenceEntity.class);
        when(pcsCaseEntity.getTenancyLicence()).thenReturn(tenancyLicenceEntity);

        TenancyLicenceType tenancyLicenceType = TenancyLicenceType.SECURE_TENANCY;
        String otherTypeDetails = "other type details";
        LocalDate tenancyStartDate = mock(LocalDate.class);

        when(tenancyLicenceEntity.getType()).thenReturn(tenancyLicenceType);
        when(tenancyLicenceEntity.getOtherTypeDetails()).thenReturn(otherTypeDetails);
        when(tenancyLicenceEntity.getStartDate()).thenReturn(tenancyStartDate);

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        ArgumentCaptor<TenancyLicenceDetails> tenancyLicenceDetailsCaptor
            = ArgumentCaptor.forClass(TenancyLicenceDetails.class);

        verify(pcsCase).setTenancyLicenceDetails(tenancyLicenceDetailsCaptor.capture());

        TenancyLicenceDetails tenancyLicenceDetails = tenancyLicenceDetailsCaptor.getValue();
        assertThat(tenancyLicenceDetails.getTypeOfTenancyLicence()).isEqualTo(tenancyLicenceType);
        assertThat(tenancyLicenceDetails.getDetailsOfOtherTypeOfTenancyLicence()).isEqualTo(otherTypeDetails);
        assertThat(tenancyLicenceDetails.getTenancyLicenceDate()).isEqualTo(tenancyStartDate);
    }

}
