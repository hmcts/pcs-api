package uk.gov.hmcts.reform.pcs.functional.testutils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import uk.gov.hmcts.reform.pcs.functional.config.CaseTypeEvents;
import uk.gov.hmcts.reform.pcs.functional.testutils.eventData.RespondPossessionClaimEventData;
import uk.gov.hmcts.reform.pcs.functional.testutils.eventData.ResumePossessionClaimEventData;

import java.io.IOException;
import java.util.Map;

public class EventDataUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    protected final ObjectNode eventData;

    protected EventDataUtil(ObjectNode eventData) {
        this.eventData = eventData;
    }

    public ObjectNode getData() {
        return eventData;
    }

    public static EventDataUtil forEvent(CaseTypeEvents event) {
        ObjectNode eventData = loadEventData(event);
        return switch (event) {
            case RESUME_POSSESSION_CLAIM -> new ResumePossessionClaimEventData(eventData);
            case RESPOND_POSSESSION_CLAIM -> new RespondPossessionClaimEventData(eventData);
            default -> new EventDataUtil(eventData);
        };
    }

    private static ObjectNode loadEventData(CaseTypeEvents event) {
        String resourcePath = "/payloads/ccdEvent/" + event.getEventId() + ".json";

        try {
            JsonNode data = MAPPER.readTree(PayloadLoader.load(resourcePath, Map.of("draftVersion", 0)));
            if (!(data instanceof ObjectNode objectData)) {
                throw new IllegalArgumentException("Event payload must be a JSON object: " + resourcePath);
            }
            return objectData;
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse event payload: " + resourcePath, e);
        }
    }
}
