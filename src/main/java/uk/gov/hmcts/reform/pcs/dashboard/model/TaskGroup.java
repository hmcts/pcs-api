package uk.gov.hmcts.reform.pcs.dashboard.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskGroup {
    private String groupId;
    private String status;
    private Task tasks;
}
