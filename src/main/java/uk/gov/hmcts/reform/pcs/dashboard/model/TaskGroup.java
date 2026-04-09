package uk.gov.hmcts.reform.pcs.dashboard.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.ComplexType;
import uk.gov.hmcts.ccd.sdk.type.ListValue;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ComplexType(generate = true)
public class TaskGroup {
    private String groupId;
    private List<ListValue<Task>> tasks;
}
