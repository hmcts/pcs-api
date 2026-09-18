package uk.gov.hmcts.reform.pcs.ccd.event.genapp;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.camunda.CamundaService;
import uk.gov.hmcts.reform.pcs.camunda.TaskType;
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

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GenAppWaTaskService {

    private final TaskDescriptionService taskDescriptionService;
    private final CamundaService camundaService;
    private final PartyService partyService;
    private final TranslationWAService translationWAService;
    private final DocumentNameService documentNameService;

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
        if (!translationWAService.isTranslationRequired(genAppEntity.getLanguageUsed())) {
            return;
        }

        PartyEntity party = genAppEntity.getParty();
        PartyRole partyRole = partyService.getPartyRole(party);

        if (partyRole != PartyRole.DEFENDANT && partyRole != PartyRole.CLAIMANT) {
            return;
        }

        PcsCaseEntity pcsCaseEntity = genAppEntity.getPcsCase();
        ClaimEntity mainClaim = pcsCaseEntity.getClaims().getFirst();

        // The gen app form will be scheduled for generation so it's referenced by its deterministic filename.
        List<DocumentEntity> documents = new ArrayList<>();
        documents.add(DocumentEntity.builder()
            .fileName(documentNameService.expectedGenAppFilename(genAppEntity, mainClaim))
            .build());

        documents.addAll(genAppEntity.getDocuments().stream()
            .filter(document -> !document.isRemoved())
            .filter(document -> !document.equals(genAppEntity.getSubmissionDocument()))
            .toList());

        if (partyRole == PartyRole.DEFENDANT) {
            translationWAService.createTranslateDefendantSubmittedDocumentTask(pcsCaseEntity, party, documents);
        } else {
            translationWAService.createTranslateClaimantSubmittedDocumentTask(pcsCaseEntity, party, documents);
        }
    }

}
