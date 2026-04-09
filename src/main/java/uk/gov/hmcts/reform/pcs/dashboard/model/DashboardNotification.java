package uk.gov.hmcts.reform.pcs.dashboard.model;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.ComplexType;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ComplexType(generate = true)
public class DashboardNotification {

    @Schema(description = "ID of notification message template",
        examples = {"Notice.AAA3.ClaimIssue.ClaimFee.Required"})
    private String templateId;

    @Schema(description = "Template values for substitution",
        examples = {"""
                     {
                        "appointmentTime": "2025-05-20T10:30:00Z",
                        "location": "London",
                        "amount": 76.15,
                        "dueDate": "2025-05-14"
                     }
                """})
    private Map<String, JsonNode> templateValues;

}
