package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.WalesHousingAct;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotApplicable;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.HousingActWalesEntity;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HousingActWalesViewTest {

    @Mock
    private PCSCase pcsCase;
    @Mock
    private PcsCaseEntity pcsCaseEntity;
    @Mock
    private ClaimEntity mainClaimEntity;

    private HousingActWalesView underTest;

    @BeforeEach
    void setUp() {
        underTest = new HousingActWalesView();
    }

    @Test
    void shouldNotSetAnythingIfNoMainClaim() {
        // Given
        when(pcsCaseEntity.getClaims()).thenReturn(List.of());

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        verifyNoInteractions(pcsCase);
    }

    @Test
    void shouldNotSetAnythingIfNoHousingActWales() {
        // Given
        when(pcsCaseEntity.getClaims()).thenReturn(List.of(mainClaimEntity));
        when(mainClaimEntity.getHousingActWales()).thenReturn(null);

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        verifyNoInteractions(pcsCase);
    }

    @Test
    void shouldSetHousingActWalesFields() {
        // Given
        HousingActWalesEntity housingActWalesEntity = mock(HousingActWalesEntity.class);

        LocalDate agentAppointmentDate = mock(LocalDate.class);

        when(pcsCaseEntity.getClaims()).thenReturn(List.of(mainClaimEntity));
        when(mainClaimEntity.getHousingActWales()).thenReturn(housingActWalesEntity);

        when(housingActWalesEntity.getRegistered()).thenReturn(YesNoNotApplicable.YES);
        when(housingActWalesEntity.getRegistrationNumber()).thenReturn("registration number");
        when(housingActWalesEntity.getLicensed()).thenReturn(YesNoNotApplicable.YES);
        when(housingActWalesEntity.getLicenceNumber()).thenReturn("licence number");
        when(housingActWalesEntity.getAgentAppointed()).thenReturn(YesNoNotApplicable.YES);
        when(housingActWalesEntity.getAgentFirstName()).thenReturn("agent first name");
        when(housingActWalesEntity.getAgentLastName()).thenReturn("agent last name");
        when(housingActWalesEntity.getAgentAppointmentDate()).thenReturn(agentAppointmentDate);

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        ArgumentCaptor<WalesHousingAct> walesHousingActCaptor
            = ArgumentCaptor.forClass(WalesHousingAct.class);

        verify(pcsCase).setWalesHousingAct(walesHousingActCaptor.capture());

        WalesHousingAct walesHousingAct = walesHousingActCaptor.getValue();
        assertThat(walesHousingAct.getRegistered()).isEqualTo(YesNoNotApplicable.YES);
        assertThat(walesHousingAct.getRegistrationNumber()).isEqualTo("registration number");
        assertThat(walesHousingAct.getLicensed()).isEqualTo(YesNoNotApplicable.YES);
        assertThat(walesHousingAct.getLicenceNumber()).isEqualTo("licence number");
        assertThat(walesHousingAct.getLicensedAgentAppointed()).isEqualTo(YesNoNotApplicable.YES);
        assertThat(walesHousingAct.getAgentFirstName()).isEqualTo("agent first name");
        assertThat(walesHousingAct.getAgentLastName()).isEqualTo("agent last name");
        assertThat(walesHousingAct.getAgentAppointmentDate()).isEqualTo(agentAppointmentDate);
    }

}
