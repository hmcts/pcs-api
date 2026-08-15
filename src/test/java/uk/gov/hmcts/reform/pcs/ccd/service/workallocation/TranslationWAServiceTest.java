package uk.gov.hmcts.reform.pcs.ccd.service.workallocation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.camunda.CamundaService;
import uk.gov.hmcts.reform.pcs.camunda.TaskType;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TranslationWAServiceTest {

    private static final long CASE_REFERENCE = 1234567890123456L;

    @Mock
    private CamundaService camundaService;
    @Mock
    private TaskDescriptionService taskDescriptionService;

    @InjectMocks
    private TranslationWAService underTest;

    @Test
    void shouldBuildDescriptionAndCreateCamundaTask() {
        ClaimEntity mainClaim = ClaimEntity.builder().id(UUID.randomUUID()).build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .caseReference(CASE_REFERENCE)
            .claims(List.of(mainClaim))
            .build();
        PartyEntity party = PartyEntity.builder().id(UUID.randomUUID()).build();
        List<DocumentEntity> documents = List.of(DocumentEntity.builder().fileName("evidence.pdf").build());

        String expectedDescription = "Defendant 1 has uploaded the following documents: evidence.pdf";
        when(taskDescriptionService.createTranslateDefendantDocumentDescription(
            CASE_REFERENCE, mainClaim, party, documents))
            .thenReturn(expectedDescription);

        underTest.createTranslateDefendantDocumentTask(pcsCaseEntity, party, documents);

        verify(camundaService).createTask(
            CASE_REFERENCE, TaskType.TRANSLATE_DEFENDANT_SUBMITTED_DOCUMENT, expectedDescription);
    }

    @Test
    void shouldUseFirstClaimAsMainClaimWhenBuildingDescription() {
        ClaimEntity firstClaim = ClaimEntity.builder().id(UUID.randomUUID()).build();
        ClaimEntity secondClaim = ClaimEntity.builder().id(UUID.randomUUID()).build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .caseReference(CASE_REFERENCE)
            .claims(List.of(firstClaim, secondClaim))
            .build();
        PartyEntity party = PartyEntity.builder().id(UUID.randomUUID()).build();
        List<DocumentEntity> documents = List.of(DocumentEntity.builder().fileName("evidence.pdf").build());

        underTest.createTranslateDefendantDocumentTask(pcsCaseEntity, party, documents);

        verify(taskDescriptionService).createTranslateDefendantDocumentDescription(
            CASE_REFERENCE, firstClaim, party, documents);
    }

    @Test
    void shouldNotCreateTaskWhenDocumentsEmpty() {
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().caseReference(CASE_REFERENCE).build();
        PartyEntity party = PartyEntity.builder().id(UUID.randomUUID()).build();

        underTest.createTranslateDefendantDocumentTask(pcsCaseEntity, party, List.of());

        verifyNoInteractions(camundaService, taskDescriptionService);
    }
}
