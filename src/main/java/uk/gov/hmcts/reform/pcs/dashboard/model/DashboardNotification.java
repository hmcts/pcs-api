package uk.gov.hmcts.reform.pcs.dashboard.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Builder
@Data
public class DashboardNotification {

    @Schema(description = "ID of notification message template",
        examples = {"Notice.AAA3.ClaimIssue.ClaimFee.Required"})
    private final String templateId;

    @Schema(description = "Template values for substitution",
        examples = {"""
                     {
                        "appointmentTime": "2025-05-20T10:30:00Z",
                        "location": "London",
                        "amount": 76.15,
                        "dueDate": "2025-05-14"
                     }
                """})
    private final Map<String, Object> templateValues;

}
