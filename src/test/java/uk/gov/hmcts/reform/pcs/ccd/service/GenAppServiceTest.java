package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.CitizenGenAppRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppState;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.GenAppRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GenAppServiceTest {

    @Mock
    private GenAppRepository genAppRepository;
    @Mock
    private PcsCaseEntity pcsCaseEntity;
    @Mock
    private PartyEntity applicantParty;

    private GenAppService underTest;

    @BeforeEach
    void setUp() {
        underTest = new GenAppService(genAppRepository);
    }

    @Test
    void shouldSaveCreatedEntityAndAddToPcsCaseEntity() {
        // Given
        CitizenGenAppRequest genAppRequest = CitizenGenAppRequest.builder()
            .applicationType(GenAppType.SOMETHING_ELSE)
            .build();

        PCSCase caseData = PCSCase.builder()
            .citizenGenAppRequest(genAppRequest)
            .build();

        // When
        GenAppEntity genAppEntity = underTest.createGenAppEntity(caseData, pcsCaseEntity, applicantParty);

        // Then
        verify(genAppRepository).save(genAppEntity);
        verify(pcsCaseEntity).addGenApp(genAppEntity);
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
        GenAppEntity genAppEntity = underTest.createGenAppEntity(caseData, pcsCaseEntity, applicantParty);

        // Then
        assertThat(genAppEntity.getType()).isEqualTo(genAppType);
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
        GenAppEntity genAppEntity = underTest.createGenAppEntity(caseData, pcsCaseEntity, applicantParty);

        // Then
        assertThat(genAppEntity.getState()).isEqualTo(GenAppState.SUBMITTED);
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
        GenAppEntity genAppEntity = underTest.createGenAppEntity(caseData, pcsCaseEntity, applicantParty);

        // Then
        assertThat(genAppEntity.getParty()).isEqualTo(applicantParty);
    }

    @ParameterizedTest
    @EnumSource(YesOrNo.class)
    void shouldSetWithin14DaysFlag(YesOrNo within14Days) {
        // Given
        CitizenGenAppRequest genAppRequest = CitizenGenAppRequest.builder()
            .within14Days(within14Days)
            .build();

        PCSCase caseData = PCSCase.builder()
            .citizenGenAppRequest(genAppRequest)
            .build();

        // When
        GenAppEntity genAppEntity = underTest.createGenAppEntity(caseData, pcsCaseEntity, applicantParty);

        // Then
        assertThat(genAppEntity.getWithin14Days()).isEqualTo(within14Days);
    }

    @Test
    void shouldSetHwfDetails() {
        // Given
        String expectedHwfReference = "hwf-1234";
        CitizenGenAppRequest genAppRequest = CitizenGenAppRequest.builder()
            .needHwf(YesOrNo.YES)
            .appliedForHwf(YesOrNo.YES)
            .hwfReference(expectedHwfReference)
            .build();

        PCSCase caseData = PCSCase.builder()
            .citizenGenAppRequest(genAppRequest)
            .build();

        // When
        GenAppEntity genAppEntity = underTest.createGenAppEntity(caseData, pcsCaseEntity, applicantParty);

        // Then
        assertThat(genAppEntity.getNeedHwf()).isEqualTo(YesOrNo.YES);
        assertThat(genAppEntity.getAppliedForHwf()).isEqualTo(YesOrNo.YES);
        assertThat(genAppEntity.getHelpWithFeesEntity().getHwfReference()).isEqualTo(expectedHwfReference);
    }

    @Test
    void shouldNotSetHwfReferenceIfAppliedForIsNo() {
        // Given
        CitizenGenAppRequest genAppRequest = CitizenGenAppRequest.builder()
            .needHwf(YesOrNo.YES)
            .appliedForHwf(YesOrNo.NO)
            .hwfReference("hwf-1234")
            .build();

        PCSCase caseData = PCSCase.builder()
            .citizenGenAppRequest(genAppRequest)
            .build();

        // When
        GenAppEntity genAppEntity = underTest.createGenAppEntity(caseData, pcsCaseEntity, applicantParty);

        // Then
        assertThat(genAppEntity.getNeedHwf()).isEqualTo(YesOrNo.YES);
        assertThat(genAppEntity.getAppliedForHwf()).isEqualTo(YesOrNo.NO);
        assertThat(genAppEntity.getHelpWithFeesEntity()).isNull();
    }

}
