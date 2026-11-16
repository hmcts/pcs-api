package uk.gov.hmcts.reform.pcs.ccd.event.claim;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.camunda.CamundaService;
import uk.gov.hmcts.reform.pcs.camunda.TaskType;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.workallocation.TaskDescriptionService;
import uk.gov.hmcts.reform.pcs.ccd.service.workallocation.TranslationWAService;
import uk.gov.hmcts.reform.pcs.exception.PartyNotFoundException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static uk.gov.hmcts.reform.pcs.ccd.service.claimform.ClaimFormDocumentGenerator.expectedClaimFormFilename;
import static uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentNameService.GENERATED_DOC_EXTENSION;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClaimWaTaskService {

    private final CamundaService camundaService;
    private final TranslationWAService translationWAService;
    private final PcsCaseService pcsCaseService;
    private final TaskDescriptionService taskDescriptionService;

    public void createTasksForIssuedClaim(long caseReference) {
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);
        ClaimEntity claimEntity = pcsCaseEntity.getMainClaim();

        if (claimEntity == null) {
            log.error("No main claim on case");
            return;
        }

        switch (claimEntity.getLanguageUsed()) {
            case ENGLISH -> {
                if (claimEntity.getGenAppExpected() == VerticalYesNo.YES) {
                    camundaService.createTask(pcsCaseEntity.getCaseReference(), TaskType.NEW_CLAIM_CREATE_NEW_HEARING,
                                              Duration.ofDays(1));
                } else {
                    camundaService.createTask(pcsCaseEntity.getCaseReference(), TaskType.NEW_CLAIM_CREATE_NEW_HEARING);
                }
            }
            case WELSH, ENGLISH_AND_WELSH -> createTranslationTaskForClaim(pcsCaseEntity, claimEntity);
        }

        createReviewAdditionalDocumentsTask(pcsCaseEntity, claimEntity);
    }

    private void createReviewAdditionalDocumentsTask(PcsCaseEntity pcsCaseEntity, ClaimEntity claimEntity) {
        List<DocumentEntity> uncategorisedDocuments = findUncategorisedClaimDocuments(pcsCaseEntity, claimEntity);
        if (uncategorisedDocuments.isEmpty()) {
            return;
        }

        String taskDescription = taskDescriptionService.createClaimAdditionalDocumentsDescription(
            pcsCaseEntity.getCaseReference(), claimEntity, getClaimCreator(pcsCaseEntity), uncategorisedDocuments);
        camundaService.createTask(
            pcsCaseEntity.getCaseReference(), TaskType.REVIEW_ADDITIONAL_DOCS_CLAIM, taskDescription);
    }

    private static List<DocumentEntity> findUncategorisedClaimDocuments(PcsCaseEntity pcsCaseEntity,
                                                                        ClaimEntity claimEntity) {
        return pcsCaseEntity.getDocuments().stream()
            .filter(document -> document.getType() == DocumentType.OTHER)
            .filter(document -> belongsToClaim(document, claimEntity))
            .toList();
    }

    private static boolean belongsToClaim(DocumentEntity document, ClaimEntity claimEntity) {
        return document.getClaim() != null && Objects.equals(document.getClaim().getId(), claimEntity.getId());
    }

    private void createTranslationTaskForClaim(PcsCaseEntity pcsCaseEntity, ClaimEntity claimEntity) {
        PartyEntity claimant = getClaimCreator(pcsCaseEntity);

        // The claim form is scheduled for generation so we reference it by its deterministic filename.
        List<DocumentEntity> documents = new ArrayList<>();
        documents.add(DocumentEntity.builder()
                          .fileName(expectedClaimFormFilename() + GENERATED_DOC_EXTENSION)
                          .build());

        documents.addAll(pcsCaseEntity.getDocuments().stream()
                             .filter(document -> document.getClaim() != null
                                 && document.getClaim().getId().equals(claimEntity.getId()))
                             .toList());

        translationWAService.createTranslateClaimantSubmittedDocumentTask(pcsCaseEntity, claimant, documents);
    }

    private static PartyEntity getClaimCreator(PcsCaseEntity pcsCaseEntity) {
        return pcsCaseEntity.getParties().stream()
            .filter(PartyEntity::isClaimCreator)
            .findFirst()
            .orElseThrow(() -> new PartyNotFoundException(
                "No claim creator found for case " + pcsCaseEntity.getCaseReference()));
    }

}
