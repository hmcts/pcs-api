package uk.gov.hmcts.reform.pcs.ccd.event.claim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.camunda.CamundaService;
import uk.gov.hmcts.reform.pcs.camunda.TaskType;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.workallocation.TranslationWAService;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
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

    @Captor
    private ArgumentCaptor<List<DocumentEntity>> documentsCaptor;

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
        PcsCaseEntity pcsCaseEntity = stubCaseWithClaim(claim);
        PartyEntity claimant = PartyEntity.builder().id(UUID.randomUUID()).claimCreator(true).build();
        pcsCaseEntity.setParties(new HashSet<>(List.of(claimant)));

        // When
        underTest.createTasksForIssuedClaim(CASE_REFERENCE);

        // Then
        verify(camundaService, never()).createTask(CASE_REFERENCE, TaskType.NEW_CLAIM_CREATE_NEW_HEARING);
    }

    @ParameterizedTest
    @EnumSource(value = LanguageUsed.class, names = {"WELSH", "ENGLISH_AND_WELSH"})
    void shouldCreateTranslateTaskWhenLanguageIsNotEnglish(LanguageUsed languageUsed) {
        ClaimEntity claim = ClaimEntity.builder()
            .id(UUID.randomUUID())
            .languageUsed(languageUsed)
            .build();
        PcsCaseEntity pcsCaseEntity = stubCaseWithClaim(claim);

        DocumentEntity documentEntity = DocumentEntity.builder()
            .fileName("Uploaded doc.pdf")
            .claim(claim)
            .build();

        PartyEntity claimant = PartyEntity.builder().id(UUID.randomUUID()).claimCreator(true).build();
        pcsCaseEntity.setParties(new HashSet<>(List.of(claimant)));
        pcsCaseEntity.addDocuments(List.of(documentEntity));

        // When
        underTest.createTasksForIssuedClaim(CASE_REFERENCE);

        // Then
        verify(translationWAService).createTranslateClaimantSubmittedDocumentTask(
            eq(pcsCaseEntity), eq(claimant), documentsCaptor.capture());
        assertThat(documentsCaptor.getValue())
            .extracting(DocumentEntity::getFileName)
            .containsExactly("Claim - Claimant 1.pdf", "Uploaded doc.pdf");
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
