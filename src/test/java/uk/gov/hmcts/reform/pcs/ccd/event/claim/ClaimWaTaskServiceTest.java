package uk.gov.hmcts.reform.pcs.ccd.event.claim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.camunda.CamundaService;
import uk.gov.hmcts.reform.pcs.camunda.TaskType;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.workallocation.TranslationWAService;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimWaTaskServiceTest {

    private static final long CASE_REFERENCE = 1234L;
    @Mock
    private CamundaService camundaService;
    @Mock
    private TranslationWAService translationWAService;
    @Mock
    private PcsCaseService pcsCaseService;

    private ClaimWaTaskService underTest;

    @BeforeEach
    void setUp() {
        underTest = new ClaimWaTaskService(camundaService, translationWAService, pcsCaseService);
    }

    @ParameterizedTest
    @EnumSource(value = LanguageUsed.class, names = {"WELSH", "ENGLISH_AND_WELSH"})
    void shouldNotCreateHearingTaskWhenLanguageIsNotEnglish(LanguageUsed languageUsed) {
        // Given
        ClaimEntity claim = ClaimEntity.builder().languageUsed(languageUsed).build();
        stubCaseWithClaim(claim);

        // When
        underTest.createTasksForIssuedClaim(CASE_REFERENCE);

        // Then
        verify(camundaService, never()).createTask(CASE_REFERENCE, TaskType.NEW_CLAIM_CREATE_NEW_HEARING);
    }

    @ParameterizedTest
    @EnumSource(value = LanguageUsed.class, names = {"WELSH", "ENGLISH_AND_WELSH"})
    void shouldNotCreateTranslateTaskWhenNoDocumentsExist(LanguageUsed languageUsed) {
        // Given
        ClaimEntity claim = ClaimEntity.builder().languageUsed(languageUsed).build();
        stubCaseWithClaim(claim);

        // When
        underTest.createTasksForIssuedClaim(CASE_REFERENCE);

        // Then
        verify(translationWAService).createTranslateClaimantSubmittedDocumentTask(CASE_REFERENCE, List.of());
    }

    @ParameterizedTest
    @EnumSource(value = LanguageUsed.class, names = {"WELSH", "ENGLISH_AND_WELSH"})
    void shouldCreateTranslateTaskWhenLanguageIsNotEnglishAndDocumentsExist(LanguageUsed languageUsed) {
        ClaimEntity claim = ClaimEntity.builder()
            .id(UUID.randomUUID())
            .languageUsed(languageUsed)
            .build();
        PcsCaseEntity pcsCaseEntity = stubCaseWithClaim(claim);

        DocumentEntity documentEntity = DocumentEntity.builder().claim(claim).build();
        DocumentEntity removedDocument = DocumentEntity.builder().claim(claim).removed(true).build();

        pcsCaseEntity.addDocuments(List.of(documentEntity, removedDocument));

        // When
        underTest.createTasksForIssuedClaim(CASE_REFERENCE);

        // Then
        verify(translationWAService)
            .createTranslateClaimantSubmittedDocumentTask(CASE_REFERENCE, List.of(documentEntity));
        verify(camundaService, never()).createTask(CASE_REFERENCE, TaskType.NEW_CLAIM_CREATE_NEW_HEARING);
    }

    @Test
    void shouldNotCreateTranslateTaskWhenLanguageIsEnglish() {
        // Given
        ClaimEntity claim = ClaimEntity.builder().languageUsed(LanguageUsed.ENGLISH).build();
        stubCaseWithClaim(claim);

        // When
        underTest.createTasksForIssuedClaim(CASE_REFERENCE);

        // Then
        verifyNoInteractions(translationWAService);
    }

    @Test
    void shouldDelayCreatingWaTaskByOneDayIfGenAppExpected() {
        ClaimEntity claim = ClaimEntity.builder()
            .languageUsed(LanguageUsed.ENGLISH)
            .genAppExpected(VerticalYesNo.YES)
            .build();

        stubCaseWithClaim(claim);

        // When
        underTest.createTasksForIssuedClaim(CASE_REFERENCE);

        // Then
        verify(camundaService).createTask(CASE_REFERENCE, TaskType.NEW_CLAIM_CREATE_NEW_HEARING, Duration.ofDays(1));
    }

    private PcsCaseEntity stubCaseWithClaim(ClaimEntity claim) {
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .caseReference(CASE_REFERENCE)
            .claims(List.of(claim))
            .build();

        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);

        return pcsCaseEntity;
    }

}
