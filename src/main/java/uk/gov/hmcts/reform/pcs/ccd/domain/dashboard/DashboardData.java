package uk.gov.hmcts.reform.pcs.ccd.domain.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.ComplexType;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;

import java.util.List;

@Data
@Builder
@ComplexType(generate = true)
@NoArgsConstructor
@AllArgsConstructor
public class DashboardData {
    private String caseId;
    private AddressUK propertyAddress;
    private List<ListValue<DashboardNotification>> notifications;
    private List<ListValue<TaskGroup>> taskGroups;
}
