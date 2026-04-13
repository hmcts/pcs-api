package uk.gov.hmcts.reform.pcs.ccd.domain.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardNotification {
    private String templateId;
    private Map<String, Object> templateValues;
}
