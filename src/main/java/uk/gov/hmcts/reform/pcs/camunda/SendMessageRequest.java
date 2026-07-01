package uk.gov.hmcts.reform.pcs.camunda;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {

    private String messageName;

    // Used in task creation
    private Map<String, DmnValue<?>> processVariables;

    // Used in task cancellation
    private Map<String, DmnValue<?>> correlationKeys;

    private boolean all;
}
