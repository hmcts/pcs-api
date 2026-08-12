package uk.gov.hmcts.reform.pcs.camunda;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Builder
@Data
@AllArgsConstructor
public class SendMessageRequest {

    private String messageName;

    private Map<String, DmnValue<?>> processVariables;

    private final Map<String, DmnValue<?>> correlationKeys;
}
