package uk.gov.hmcts.reform.pcs.ccd.service.genapp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.CitizenGenAppRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppState;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.GenAppRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenAppServiceTest {

    @Mock
    private GenAppRepository genAppRepository;
    @Mock
    private PcsCaseEntity pcsCaseEntity;
    @Mock
    private PartyEntity applicantParty;
    @Captor
    private ArgumentCaptor<GenAppEntity> genAppEntityCaptor;

    private GenAppService underTest;

    @BeforeEach
    void setUp() {
        underTest = new GenAppService(genAppRepository);
    }

    @Test
    void shouldReturnSavedEntity() {
        // Given
        CitizenGenAppRequest genAppRequest = CitizenGenAppRequest.builder()
            .applicationType(GenAppType.SOMETHING_ELSE)
            .build();

        PCSCase caseData = PCSCase.builder()
            .citizenGenAppRequest(genAppRequest)
            .build();

        GenAppEntity savedGenAppEntity = mock(GenAppEntity.class);
        when(genAppRepository.save(isA(GenAppEntity.class))).thenReturn(savedGenAppEntity);

        // When
        GenAppEntity returnedGenAppEntity = underTest.createGenAppEntity(caseData, pcsCaseEntity, applicantParty);

        // Then
        verify(genAppRepository).save(any(GenAppEntity.class));
        assertThat(returnedGenAppEntity).isSameAs(savedGenAppEntity);
    }

    @ParameterizedTest
    @EnumSource(value = GenAppType.class)
    void shouldSetGeneralApplicationType(GenAppType genAppType) {
        // Given
        CitizenGenAppRequest genAppRequest = CitizenGenAppRequest.builder()
            .applicationType(genAppType)
            .build();

        PCSCase caseData = PCSCase.builder()
            .citizenGenAppRequest(genAppRequest)
            .build();

        // When
        underTest.createGenAppEntity(caseData, pcsCaseEntity, applicantParty);

        // Then
        verify(genAppRepository).save(genAppEntityCaptor.capture());
        assertThat(genAppEntityCaptor.getValue().getType()).isEqualTo(genAppType);
    }

    @Test
    void shouldSetInitialState() {
        // Given
        CitizenGenAppRequest genAppRequest = CitizenGenAppRequest.builder()
            .build();

        PCSCase caseData = PCSCase.builder()
            .citizenGenAppRequest(genAppRequest)
            .build();

        // When
        underTest.createGenAppEntity(caseData, pcsCaseEntity, applicantParty);

        // Then
        verify(genAppRepository).save(genAppEntityCaptor.capture());
        assertThat(genAppEntityCaptor.getValue().getState()).isEqualTo(GenAppState.SUBMITTED);
    }

    @Test
    void shouldSetApplicantParty() {
        // Given
        CitizenGenAppRequest genAppRequest = CitizenGenAppRequest.builder()
            .applicationType(GenAppType.SUSPEND)
            .build();

        PCSCase caseData = PCSCase.builder()
            .citizenGenAppRequest(genAppRequest)
            .build();

        // When
        underTest.createGenAppEntity(caseData, pcsCaseEntity, applicantParty);

        // Then
        verify(genAppRepository).save(genAppEntityCaptor.capture());
        assertThat(genAppEntityCaptor.getValue().getParty()).isEqualTo(applicantParty);
    }

    @ParameterizedTest
    @EnumSource(VerticalYesNo.class)
    void shouldSetWithin14DaysFlag(VerticalYesNo within14Days) {
        // Given
        CitizenGenAppRequest genAppRequest = CitizenGenAppRequest.builder()
            .within14Days(within14Days)
            .build();

        PCSCase caseData = PCSCase.builder()
            .citizenGenAppRequest(genAppRequest)
            .build();

        // When
        underTest.createGenAppEntity(caseData, pcsCaseEntity, applicantParty);

        // Then
        verify(genAppRepository).save(genAppEntityCaptor.capture());
        assertThat(genAppEntityCaptor.getValue().getWithin14Days()).isEqualTo(within14Days);
    }

    @Test
    void shouldSetHwfDetails() {
        // Given
        String expectedHwfReference = "hwf-1234";
        CitizenGenAppRequest genAppRequest = CitizenGenAppRequest.builder()
            .needHwf(VerticalYesNo.YES)
            .appliedForHwf(VerticalYesNo.YES)
            .hwfReference(expectedHwfReference)
            .build();

        PCSCase caseData = PCSCase.builder()
            .citizenGenAppRequest(genAppRequest)
            .build();

        // When
        underTest.createGenAppEntity(caseData, pcsCaseEntity, applicantParty);

        // Then
        verify(genAppRepository).save(genAppEntityCaptor.capture());
        GenAppEntity genAppEntity = genAppEntityCaptor.getValue();

        assertThat(genAppEntity.getNeedHwf()).isEqualTo(VerticalYesNo.YES);
        assertThat(genAppEntity.getAppliedForHwf()).isEqualTo(VerticalYesNo.YES);
        assertThat(genAppEntity.getHelpWithFeesEntity().getHwfReference()).isEqualTo(expectedHwfReference);
    }

    @Test
    void shouldNotSetHwfReferenceIfAppliedForIsNo() {
        // Given
        CitizenGenAppRequest genAppRequest = CitizenGenAppRequest.builder()
            .needHwf(VerticalYesNo.YES)
            .appliedForHwf(VerticalYesNo.NO)
            .hwfReference("hwf-1234")
            .build();

        PCSCase caseData = PCSCase.builder()
            .citizenGenAppRequest(genAppRequest)
            .build();

        // When
        underTest.createGenAppEntity(caseData, pcsCaseEntity, applicantParty);

        // Then
        verify(genAppRepository).save(genAppEntityCaptor.capture());
        GenAppEntity genAppEntity = genAppEntityCaptor.getValue();

        assertThat(genAppEntity.getNeedHwf()).isEqualTo(VerticalYesNo.YES);
        assertThat(genAppEntity.getAppliedForHwf()).isEqualTo(VerticalYesNo.NO);
        assertThat(genAppEntity.getHelpWithFeesEntity()).isNull();
    }

}
