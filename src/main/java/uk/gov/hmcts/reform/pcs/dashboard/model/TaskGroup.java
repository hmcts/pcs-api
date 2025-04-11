package uk.gov.hmcts.reform.pcs.dashboard.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskGroup {
    private String groupId;
    private Task task;
}
