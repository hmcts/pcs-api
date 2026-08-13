package uk.gov.hmcts.reform.pcs.ccd.event.genapp;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.camunda.CamundaService;
import uk.gov.hmcts.reform.pcs.camunda.TaskType;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.workallocation.TaskDescriptionService;

@Service
@RequiredArgsConstructor
public class GenAppWaTaskService {

    private final TaskDescriptionService taskDescriptionService;
    private final CamundaService camundaService;

    public void createReviewGenAppTask(long caseReference, GenAppEntity genAppEntity) {
        if (genAppEntity.getType() == GenAppType.ADJOURN) {
            String description = taskDescriptionService.createReviewAdjournGenAppDescription(
                caseReference,
                genAppEntity
            );
            camundaService.createTask(caseReference, TaskType.REVIEW_ADJOURN_GEN_APP, description);
        }
    }

}
