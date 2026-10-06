package uk.gov.hmcts.reform.pcs.camunda;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
public class CamundaRequestTaskData {

    enum Action {
        CREATE,
        CANCEL
    }

    private final Action action;

    private final long caseReference;

    private final TaskType taskType;

    private final String taskDescription;

    private final UUID idempotencyKey;

    /** The order or application a task is about, for a task there can be one of per order or application. */
    private final TaskSubject subject;

    /** Whom the task is for, which the configuration DMN makes its assignee. */
    private final UUID assignee;

}
