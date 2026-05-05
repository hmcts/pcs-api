package uk.gov.hmcts.reform.pcs.ccd.domain.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.type.ListValue;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardNotification {
    private String templateId;
    private List<ListValue<TemplateValue>> templateValues;
}
