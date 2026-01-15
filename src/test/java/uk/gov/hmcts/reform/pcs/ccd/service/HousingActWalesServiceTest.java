package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.WalesHousingAct;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotApplicable;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.HousingActWalesEntity;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mock.Strictness.LENIENT;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HousingActWalesServiceTest {

    @Mock
    private PCSCase pcsCase;
    @Mock(strictness = LENIENT)
    private WalesHousingAct walesHousingAct;

    private HousingActWalesService underTest;

    @BeforeEach
    void setUp() {
        when(pcsCase.getWalesHousingAct()).thenReturn(walesHousingAct);

        underTest = new HousingActWalesService();
    }

    @Test
    void shouldSetRegistrationNumberWhenRegistered() {
        // Given
        String registrationNumber = "some registration number";
        when(walesHousingAct.getRegistered()).thenReturn(YesNoNotApplicable.YES);
        when(walesHousingAct.getRegistrationNumber()).thenReturn(registrationNumber);

        // When
        HousingActWalesEntity housingActWalesEntity = underTest.createHousingActWalesEntity(pcsCase);

        // Then
        assertThat(housingActWalesEntity.getRegistered()).isEqualTo(YesNoNotApplicable.YES);
        assertThat(housingActWalesEntity.getRegistrationNumber()).isEqualTo(registrationNumber);
    }

    @ParameterizedTest
    @EnumSource(value = YesNoNotApplicable.class, names = {"NO", "NOT_APPLICABLE"})
    void shouldNotSetRegistrationNumberWhenNotRegistered(YesNoNotApplicable registeredFlag) {
        // Given
        String registrationNumber = "some registration number";
        when(walesHousingAct.getRegistered()).thenReturn(registeredFlag);
        when(walesHousingAct.getRegistrationNumber()).thenReturn(registrationNumber);

        // When
        HousingActWalesEntity housingActWalesEntity = underTest.createHousingActWalesEntity(pcsCase);

        // Then
        assertThat(housingActWalesEntity.getRegistered()).isEqualTo(registeredFlag);
        assertThat(housingActWalesEntity.getRegistrationNumber()).isNull();
    }

    @Test
    void shouldSetLicenceNumberWhenLicensed() {
        // Given
        String licenceNumber = "some licence number";
        when(walesHousingAct.getLicensed()).thenReturn(YesNoNotApplicable.YES);
        when(walesHousingAct.getLicenceNumber()).thenReturn(licenceNumber);

        // When
        HousingActWalesEntity housingActWalesEntity = underTest.createHousingActWalesEntity(pcsCase);

        // Then
        assertThat(housingActWalesEntity.getLicensed()).isEqualTo(YesNoNotApplicable.YES);
        assertThat(housingActWalesEntity.getLicenceNumber()).isEqualTo(licenceNumber);
    }

    @ParameterizedTest
    @EnumSource(value = YesNoNotApplicable.class, names = {"NO", "NOT_APPLICABLE"})
    void shouldNotSetLicenceNumberWhenNotLicensed(YesNoNotApplicable licensedFlag) {
        // Given
        String licenceNumber = "some licence number";
        when(walesHousingAct.getLicensed()).thenReturn(licensedFlag);
        when(walesHousingAct.getLicenceNumber()).thenReturn(licenceNumber);

        // When
        HousingActWalesEntity housingActWalesEntity = underTest.createHousingActWalesEntity(pcsCase);

        // Then
        assertThat(housingActWalesEntity.getLicensed()).isEqualTo(licensedFlag);
        assertThat(housingActWalesEntity.getLicenceNumber()).isNull();
    }

    @Test
    void shouldSetAgentDetailsWhenAppointed() {
        // Given
        String agentFirstName = "agent first name";
        String agentLastName = "agent last name";
        String agentLicenceNumber = "agent licence number";
        LocalDate agentAppointedDate = mock(LocalDate.class);

        when(walesHousingAct.getLicensedAgentAppointed()).thenReturn(YesNoNotApplicable.YES);
        when(walesHousingAct.getAgentFirstName()).thenReturn(agentFirstName);
        when(walesHousingAct.getAgentLastName()).thenReturn(agentLastName);
        when(walesHousingAct.getAgentLicenceNumber()).thenReturn(agentLicenceNumber);
        when(walesHousingAct.getAgentAppointmentDate()).thenReturn(agentAppointedDate);

        // When
        HousingActWalesEntity housingActWalesEntity = underTest.createHousingActWalesEntity(pcsCase);

        // Then
        assertThat(housingActWalesEntity.getAgentAppointed()).isEqualTo(YesNoNotApplicable.YES);
        assertThat(housingActWalesEntity.getAgentFirstName()).isEqualTo(agentFirstName);
        assertThat(housingActWalesEntity.getAgentLastName()).isEqualTo(agentLastName);
        assertThat(housingActWalesEntity.getAgentLicenceNumber()).isEqualTo(agentLicenceNumber);
        assertThat(housingActWalesEntity.getAgentAppointmentDate()).isEqualTo(agentAppointedDate);
    }

    @ParameterizedTest
    @EnumSource(value = YesNoNotApplicable.class, names = {"NO", "NOT_APPLICABLE"})
    void shouldNotSetAgentDetailsWhenAppointed(YesNoNotApplicable agentAppointedFlag) {
        // Given
        String agentFirstName = "agent first name";
        String agentLastName = "agent last name";
        String agentLicenceNumber = "agent licence number";
        LocalDate agentAppointedDate = mock(LocalDate.class);

        when(walesHousingAct.getLicensedAgentAppointed()).thenReturn(agentAppointedFlag);
        when(walesHousingAct.getAgentFirstName()).thenReturn(agentFirstName);
        when(walesHousingAct.getAgentLastName()).thenReturn(agentLastName);
        when(walesHousingAct.getAgentLicenceNumber()).thenReturn(agentLicenceNumber);
        when(walesHousingAct.getAgentAppointmentDate()).thenReturn(agentAppointedDate);

        // When
        HousingActWalesEntity housingActWalesEntity = underTest.createHousingActWalesEntity(pcsCase);

        // Then
        assertThat(housingActWalesEntity.getAgentAppointed()).isEqualTo(agentAppointedFlag);
        assertThat(housingActWalesEntity.getAgentFirstName()).isNull();
        assertThat(housingActWalesEntity.getAgentLastName()).isNull();
        assertThat(housingActWalesEntity.getAgentLicenceNumber()).isNull();
        assertThat(housingActWalesEntity.getAgentAppointmentDate()).isNull();
    }

}
