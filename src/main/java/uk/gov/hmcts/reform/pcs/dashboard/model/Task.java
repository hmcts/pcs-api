package uk.gov.hmcts.reform.pcs.dashboard.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Task {
    private String templateId;
    private Map<String, Object> templateValues;
    private String status;
}
