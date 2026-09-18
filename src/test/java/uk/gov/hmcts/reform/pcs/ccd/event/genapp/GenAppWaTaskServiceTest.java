package uk.gov.hmcts.reform.pcs.ccd.event.genapp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.camunda.CamundaService;
import uk.gov.hmcts.reform.pcs.camunda.TaskType;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentNameService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.service.workallocation.TaskDescriptionService;
import uk.gov.hmcts.reform.pcs.ccd.service.workallocation.TranslationWAService;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenAppWaTaskServiceTest {

    private static final long CASE_REFERENCE = 1234L;

    @Mock
    private TaskDescriptionService taskDescriptionService;
    @Mock
    private CamundaService camundaService;
    @Mock
    private PartyService partyService;
    @Mock
    private TranslationWAService translationWAService;
    @Mock
    private DocumentNameService documentNameService;

    @Captor
    private ArgumentCaptor<List<DocumentEntity>> documentsCaptor;

    private GenAppWaTaskService underTest;

    @BeforeEach
    void setUp() {
        underTest = new GenAppWaTaskService(taskDescriptionService, camundaService, partyService,
                                            translationWAService, documentNameService);
    }

    @ParameterizedTest
    @MethodSource("genAppTypeToTaskTypeScenarios")
    void shouldCreateTaskForGenApp(GenAppType genAppType, TaskType expectedTaskType) {
        // Given
        GenAppEntity genAppEntity = GenAppEntity.builder()
            .type(genAppType)
            .build();

        String expectedDescription = "some task description for " + genAppType;
        when(taskDescriptionService.createReviewGenAppDescription(CASE_REFERENCE, genAppEntity))
            .thenReturn(expectedDescription);

        // When
        underTest.createReviewGenAppTask(CASE_REFERENCE, genAppEntity);

        // Then
        verify(camundaService).createTask(CASE_REFERENCE, expectedTaskType, expectedDescription);
    }

    private static Stream<Arguments> genAppTypeToTaskTypeScenarios() {
        return Stream.of(
            Arguments.arguments(GenAppType.ADJOURN, TaskType.REVIEW_ADJOURN_GEN_APP),
            Arguments.arguments(GenAppType.SET_ASIDE, TaskType.REVIEW_SET_ASIDE_GEN_APP),
            Arguments.arguments(GenAppType.SOMETHING_ELSE, TaskType.REVIEW_GEN_APP)
        );
    }

    @Test
    void shouldCreateTranslationTaskWhenDefendantWelshDocumentsExist() {
        // Given
        ClaimEntity mainClaim = ClaimEntity.builder().id(UUID.randomUUID()).build();
        PartyEntity party = PartyEntity.builder().id(UUID.randomUUID()).build();
        PcsCaseEntity genAppPcsCase = PcsCaseEntity.builder()
            .caseReference(1234567890123456L)
            .claims(List.of(mainClaim))
            .build();

        DocumentEntity activeDocument = DocumentEntity.builder().fileName("evidence.pdf").build();
        DocumentEntity removedDocument = DocumentEntity.builder().removed(true).build();

        GenAppEntity genAppEntity = GenAppEntity.builder()
            .party(party)
            .pcsCase(genAppPcsCase)
            .languageUsed(LanguageUsed.WELSH)
            .documents(List.of(activeDocument, removedDocument))
            .build();

        when(partyService.getPartyRole(party)).thenReturn(PartyRole.DEFENDANT);
        when(translationWAService.isTranslationRequired(LanguageUsed.WELSH)).thenReturn(true);
        when(documentNameService.expectedGenAppFilename(genAppEntity, mainClaim))
            .thenReturn("General Application GA1 - Defendant 1");

        // When
        underTest.createTranslationTaskForGenApp(genAppEntity);

        // Then
        verify(translationWAService).createTranslateDefendantSubmittedDocumentTask(
            eq(genAppPcsCase), eq(party), documentsCaptor.capture());
        assertThat(documentsCaptor.getValue())
            .extracting(DocumentEntity::getFileName)
            .containsExactly("General Application GA1 - Defendant 1", "evidence.pdf");
    }

    @Test
    void shouldNotCreateTranslationTaskWhenApplicantIsNeitherClaimantNorDefendant() {
        // Given
        PartyEntity party = PartyEntity.builder().id(UUID.randomUUID()).build();
        GenAppEntity genAppEntity = GenAppEntity.builder()
            .party(party)
            .languageUsed(LanguageUsed.WELSH)
            .build();

        when(partyService.getPartyRole(party)).thenReturn(PartyRole.UNDERLESSEE_OR_MORTGAGEE);
        when(translationWAService.isTranslationRequired(LanguageUsed.WELSH)).thenReturn(true);

        // When
        underTest.createTranslationTaskForGenApp(genAppEntity);

        // Then
        verify(translationWAService, never()).createTranslateDefendantSubmittedDocumentTask(any(), any(), any());
        verify(translationWAService, never()).createTranslateClaimantSubmittedDocumentTask(anyLong(), any());
    }

    @Test
    void shouldNotCreateTranslationTaskWhenLanguageIsEnglish() {
        // Given
        PartyEntity party = PartyEntity.builder().id(UUID.randomUUID()).build();
        GenAppEntity genAppEntity = GenAppEntity.builder()
            .party(party)
            .languageUsed(LanguageUsed.ENGLISH)
            .build();

        // When
        underTest.createTranslationTaskForGenApp(genAppEntity);

        // Then
        verify(translationWAService, never()).createTranslateDefendantSubmittedDocumentTask(any(), any(), any());
        verify(translationWAService, never()).createTranslateClaimantSubmittedDocumentTask(anyLong(), any());
    }

    @Test
    void shouldCreateTranslationTaskWhenClaimantWelshDocumentsExist() {
        // Given
        ClaimEntity mainClaim = ClaimEntity.builder().id(UUID.randomUUID()).build();
        PartyEntity party = PartyEntity.builder().id(UUID.randomUUID()).build();
        PcsCaseEntity genAppPcsCase = PcsCaseEntity.builder()
            .caseReference(1234567890123456L)
            .claims(List.of(mainClaim))
            .build();

        DocumentEntity activeDocument = DocumentEntity.builder().fileName("evidence.pdf").build();
        DocumentEntity removedDocument = DocumentEntity.builder().removed(true).build();

        GenAppEntity genAppEntity = GenAppEntity.builder()
            .party(party)
            .pcsCase(genAppPcsCase)
            .languageUsed(LanguageUsed.WELSH)
            .documents(List.of(activeDocument, removedDocument))
            .build();

        when(partyService.getPartyRole(party)).thenReturn(PartyRole.CLAIMANT);
        when(translationWAService.isTranslationRequired(LanguageUsed.WELSH)).thenReturn(true);
        when(documentNameService.expectedGenAppFilename(genAppEntity, mainClaim))
            .thenReturn("General Application GA1 - Claimant 1");

        // When
        underTest.createTranslationTaskForGenApp(genAppEntity);

        // Then
        verify(translationWAService).createTranslateClaimantSubmittedDocumentTask(
            eq(genAppPcsCase.getCaseReference()), documentsCaptor.capture());
        assertThat(documentsCaptor.getValue())
            .extracting(DocumentEntity::getFileName)
            .containsExactly("General Application GA1 - Claimant 1", "evidence.pdf");
    }

}
