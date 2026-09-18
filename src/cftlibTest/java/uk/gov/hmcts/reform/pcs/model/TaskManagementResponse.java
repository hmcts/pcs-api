package uk.gov.hmcts.reform.pcs.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TaskManagementResponse {

    private List<WaTask> tasks;

}
