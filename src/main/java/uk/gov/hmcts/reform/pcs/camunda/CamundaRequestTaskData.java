package uk.gov.hmcts.reform.pcs.camunda;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
public class CamundaRequestTaskData {

    public enum Action {
        CREATE,
        CANCEL
    }

    private final Action action;

    private final long caseReference;

    private final TaskType taskType;

    private final String taskDescription;

    private final UUID idempotencyKey;

    private Map<String, Object> taskDetails;

    /** Whom the task is for, which the configuration DMN makes its assignee. */
    private final String assignee;

}
