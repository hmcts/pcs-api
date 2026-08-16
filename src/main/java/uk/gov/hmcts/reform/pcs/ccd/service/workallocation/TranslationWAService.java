package uk.gov.hmcts.reform.pcs.ccd.service.workallocation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.camunda.CamundaService;
import uk.gov.hmcts.reform.pcs.camunda.TaskType;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TranslationWAService {

    private final CamundaService camundaService;
    private final TaskDescriptionService taskDescriptionService;

    public void createTranslateDefendantSubmittedDocumentTask(PcsCaseEntity pcsCaseEntity, PartyEntity party,
                                                      List<DocumentEntity> documents) {
        if (documents.isEmpty()) {
            return;
        }

        long caseReference = pcsCaseEntity.getCaseReference();
        ClaimEntity mainClaim = pcsCaseEntity.getClaims().getFirst();

        String description = taskDescriptionService.createTranslateDefendantDocumentDescription(
            caseReference, mainClaim, party, documents);

        camundaService.createTask(caseReference, TaskType.TRANSLATE_DEFENDANT_SUBMITTED_DOCUMENT, description);
    }

    public void createTranslateClaimantSubmittedDocumentTask(long caseReference, List<DocumentEntity> documents) {
        if (documents.isEmpty()) {
            return;
        }

        String description = taskDescriptionService.createTranslateClaimantDocumentDescription(
            caseReference, documents);

        camundaService.createTask(caseReference, TaskType.TRANSLATE_CLAIMANT_SUBMITTED_DOCUMENT, description);
    }
}
