package uk.gov.hmcts.reform.pcs.camunda;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

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

}
