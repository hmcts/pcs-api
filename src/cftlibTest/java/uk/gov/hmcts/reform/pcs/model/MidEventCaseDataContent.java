package uk.gov.hmcts.reform.pcs.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.ccd.client.model.Event;

/**
 * Note: This actually maps to a `CaseDataContent` on the ccd data store backend, but
 * the `CaseDataContent` class in the ccd client doesn't model the event_data property
 * so this class is used for mid-event payloads from CftLib tests instead.
 */
@Data
@Builder
public class MidEventCaseDataContent {

    private final Event event;

    @JsonProperty("event_data")
    private final Object eventData;

    @JsonProperty("event_token")
    private final String eventToken;

    @JsonProperty("case_reference")
    private final String caseReference;

}
