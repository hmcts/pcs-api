package uk.gov.hmcts.reform.pcs.ccd.domain.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.api.ComplexType;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea;

@Data
@Builder
@ComplexType(generate = true)
@NoArgsConstructor
@AllArgsConstructor
public class DashboardData {
    private String claimantName;
    private String possessionPropertyAddress;
    @CCD(typeOverride = TextArea)
    private String notifications;
    @CCD(typeOverride = TextArea)
    private String taskGroups;
    private String appliedCaseState;
    private String stateResolution;
}
