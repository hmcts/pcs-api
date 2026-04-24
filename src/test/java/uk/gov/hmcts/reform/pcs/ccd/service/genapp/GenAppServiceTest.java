package uk.gov.hmcts.reform.pcs.ccd.service.genapp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.CitizenGenAppRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppState;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.GenAppRepository;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
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

    @ParameterizedTest
    @MethodSource("otherPartiesAgreedScenarios")
    void shouldSetOtherPartiesAgreedDetails(CitizenGenAppRequest genAppRequest,
                                            VerticalYesNo expectedOtherPartiesAgreed,
                                            VerticalYesNo expectedWithoutNotice,
                                            String expectedWithoutNoticeReason) {
        // Given
        PCSCase caseData = PCSCase.builder()
            .citizenGenAppRequest(genAppRequest)
            .build();

        // When
        underTest.createGenAppEntity(caseData, pcsCaseEntity, applicantParty);

        // Then
        verify(genAppRepository).save(genAppEntityCaptor.capture());
        GenAppEntity genAppEntity = genAppEntityCaptor.getValue();

        assertThat(genAppEntity.getOtherPartiesAgreed()).isEqualTo(expectedOtherPartiesAgreed);
        assertThat(genAppEntity.getWithoutNotice()).isEqualTo(expectedWithoutNotice);
        assertThat(genAppEntity.getWithoutNoticeReason()).isEqualTo(expectedWithoutNoticeReason);
    }

    @ParameterizedTest
    @EnumSource(value = LanguageUsed.class)
    void shouldSetLanguageUsed(LanguageUsed languageUsed) {
        // Given
        CitizenGenAppRequest genAppRequest = CitizenGenAppRequest.builder()
                .languageUsed(languageUsed)
                .build();

        PCSCase caseData = PCSCase.builder()
                .citizenGenAppRequest(genAppRequest)
                .build();

        // When
        underTest.createGenAppEntity(caseData, pcsCaseEntity, applicantParty);

        // Then
        verify(genAppRepository).save(genAppEntityCaptor.capture());
        assertThat(genAppEntityCaptor.getValue().getLanguageUsed()).isEqualTo(languageUsed);
    }

    private static Stream<Arguments> otherPartiesAgreedScenarios() {
        return Stream.of(
            argumentSet("Parties agreed", CitizenGenAppRequest.builder()
                            .otherPartiesAgreed(VerticalYesNo.YES)
                            .withoutNotice(VerticalYesNo.YES)         // Should be ignored
                            .withoutNoticeReason("some reason") // Should be ignored
                            .build(),
                        VerticalYesNo.YES,    // Expected otherPartiesAgreed
                        null,           // Expected withoutNotice
                        null            // Expected withoutNoticeReason
            ),
            argumentSet("Parties not agreed, without notice", CitizenGenAppRequest.builder()
                            .otherPartiesAgreed(VerticalYesNo.NO)
                            .withoutNotice(VerticalYesNo.YES)
                            .withoutNoticeReason("some reason")
                            .build(),
                        VerticalYesNo.NO,     // Expected otherPartiesAgreed
                        VerticalYesNo.YES,    // Expected withoutNotice
                        "some reason"   // Expected withoutNoticeReason
            ),
            argumentSet("Parties not agreed, not without notice", CitizenGenAppRequest.builder()
                            .otherPartiesAgreed(VerticalYesNo.NO)
                            .withoutNotice(VerticalYesNo.NO)
                            .withoutNoticeReason("some reason") // Should be ignored
                            .build(),
                        VerticalYesNo.NO,     // Expected otherPartiesAgreed
                        VerticalYesNo.NO,     // Expected withoutNotice
                        null            // Expected withoutNoticeReason
            )
        );
    }

}
