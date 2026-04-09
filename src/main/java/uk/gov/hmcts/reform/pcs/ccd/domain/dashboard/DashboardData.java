package uk.gov.hmcts.reform.pcs.ccd.domain.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.ComplexType;
import uk.gov.hmcts.ccd.sdk.type.ListValue;

import java.util.List;

@Data
@Builder
@ComplexType(generate = true)
@NoArgsConstructor
@AllArgsConstructor
public class DashboardData {
    private String claimantName;
    private String possessionPropertyAddress;
    private List<ListValue<DashboardNotificationData>> notifications;
    private List<ListValue<TaskGroupData>> taskGroups;
    private String appliedCaseState;
    private String stateResolution;
}
