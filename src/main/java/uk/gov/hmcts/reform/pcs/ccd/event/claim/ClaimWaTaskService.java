package uk.gov.hmcts.reform.pcs.ccd.event.claim;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.camunda.CamundaService;
import uk.gov.hmcts.reform.pcs.camunda.TaskType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.workallocation.TranslationWAService;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClaimWaTaskService {

    private final CamundaService camundaService;
    private final TranslationWAService translationWAService;
    private final PcsCaseService pcsCaseService;

    public void createTasksForIssuedClaim(long caseReference) {
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);
        ClaimEntity claimEntity = pcsCaseEntity.getMainClaim();

        switch (claimEntity.getLanguageUsed()) {
            case ENGLISH -> {
                if (claimEntity.getGenAppExpected() == VerticalYesNo.YES) {
                    camundaService.createTask(pcsCaseEntity.getCaseReference(), TaskType.NEW_CLAIM_CREATE_NEW_HEARING,
                                              Duration.ofDays(1));
                } else {
                    camundaService.createTask(pcsCaseEntity.getCaseReference(), TaskType.NEW_CLAIM_CREATE_NEW_HEARING);
                }
            }
            case WELSH, ENGLISH_AND_WELSH -> createTranslationTaskForClaim(caseReference, pcsCaseEntity, claimEntity);
        }
    }

    private void createTranslationTaskForClaim(long caseReference,
                                               PcsCaseEntity pcsCaseEntity,
                                               ClaimEntity claimEntity) {

        List<DocumentEntity> documents = pcsCaseEntity.getDocuments().stream()
            .filter(document -> !document.isRemoved()
                && document.getClaim() != null
                && document.getClaim().getId().equals(claimEntity.getId()))
            .toList();

        translationWAService.createTranslateClaimantSubmittedDocumentTask(caseReference, documents);
    }

}
