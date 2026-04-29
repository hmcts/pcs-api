package uk.gov.hmcts.reform.pcs.ccd.domain.dashboard;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.type.ListValue;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskGroup {
    private TaskGroupId groupId;
    private List<ListValue<Task>> tasks;
}
