package uk.gov.hmcts.reform.pcs.ccd.event.genapp;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.camunda.CamundaService;
import uk.gov.hmcts.reform.pcs.camunda.TaskType;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.service.workallocation.TaskDescriptionService;
import uk.gov.hmcts.reform.pcs.ccd.service.workallocation.TranslationWAService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GenAppWaTaskService {

    private final TaskDescriptionService taskDescriptionService;
    private final CamundaService camundaService;
    private final PartyService partyService;
    private final TranslationWAService translationWAService;

    public void createReviewGenAppTask(long caseReference, GenAppEntity genAppEntity) {
        String description = taskDescriptionService
            .createReviewGenAppDescription(caseReference, genAppEntity);

        TaskType taskType = switch (genAppEntity.getType()) {
            case ADJOURN -> TaskType.REVIEW_ADJOURN_GEN_APP;
            case SET_ASIDE -> TaskType.REVIEW_SET_ASIDE_GEN_APP;
            case SOMETHING_ELSE -> TaskType.REVIEW_GEN_APP;
        };

        camundaService.createTask(caseReference, taskType, description);
    }

    public void createTranslationTaskForGenApp(GenAppEntity genAppEntity) {
        PartyEntity party = genAppEntity.getParty();
        if (partyService.getPartyRole(party) != PartyRole.DEFENDANT) {
            return;
        }

        if (!translationWAService.isTranslationRequired(genAppEntity.getLanguageUsed())) {
            return;
        }

        List<DocumentEntity> documents = genAppEntity.getDocuments().stream()
            .filter(document -> !document.isRemoved())
            .toList();

        PcsCaseEntity pcsCaseEntity = genAppEntity.getPcsCase();
        translationWAService.createTranslateDefendantSubmittedDocumentTask(pcsCaseEntity, party, documents);
    }

}
