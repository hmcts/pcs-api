package uk.gov.hmcts.reform.pcs.ccd.event.genapp;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.camunda.CamundaService;
import uk.gov.hmcts.reform.pcs.camunda.TaskType;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.workallocation.TaskDescriptionService;

@Service
@RequiredArgsConstructor
public class GenAppWaTaskService {

    private final TaskDescriptionService taskDescriptionService;
    private final CamundaService camundaService;

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

}
