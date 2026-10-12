package uk.gov.hmcts.reform.pcs.functional.testutils.eventData;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import uk.gov.hmcts.reform.pcs.functional.testutils.EventDataUtil;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ResumePossessionClaimEventData extends EventDataUtil {

    public ResumePossessionClaimEventData(ObjectNode data) {
        super(data);
    }

    public ResumePossessionClaimEventData addDefendant(String defendantKey, Map<String, String> defendantValues) {
        ObjectNode defendant = JsonNodeFactory.instance.objectNode();
        defendant.put("nameKnown", "NO");
        defendant.putNull("firstName");
        defendant.putNull("lastName");
        defendant.put("addressKnown", "NO");
        defendant.putNull("addressSameAsPossession");

        ObjectNode correspondenceAddress = defendant.putObject("correspondenceAddress");
        Set<String> addressFields = Set.of(
            "AddressLine1", "AddressLine2", "AddressLine3", "PostTown", "County", "Country", "PostCode"
        );
        for (String addressField : addressFields) {
            correspondenceAddress.putNull(addressField);
        }

        defendantValues.forEach((field, value) -> {
            if (addressFields.contains(field)) {
                correspondenceAddress.put(field, value);
            } else {
                defendant.put(field, value);
            }
        });

        if (!"defendant1".equals(defendantKey)) {
            eventData.put("addAnotherDefendant", "YES");
            JsonNode additionalDefendants = eventData.get("additionalDefendants");
            ArrayNode defendants = additionalDefendants instanceof ArrayNode arrayNode
                ? arrayNode
                : eventData.putArray("additionalDefendants");
            ObjectNode additionalDefendant = defendants.addObject();
            additionalDefendant.set("value", defendant);
            additionalDefendant.put("id", UUID.randomUUID().toString());
        } else {
            eventData.set(defendantKey, defendant);
        }
        return this;
    }
}
