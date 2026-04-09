package uk.gov.hmcts.reform.pcs.ccd.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

import java.util.List;
import java.util.Optional;

/**
 * Processor for applying clearFields logic to draft case data.
 * Detects which event object contains clearFields and applies field removals relative to that object.
 */
@Component
@Slf4j
public class DraftClearFieldsProcessor {

    private final ObjectMapper objectMapper;

    public DraftClearFieldsProcessor(@Qualifier("draftCaseDataObjectMapper") ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Optional<ClearFieldsContext> extractClearFieldsContext(Object eventData) {
        if (!(eventData instanceof PCSCase pcsCase)) {
            return Optional.empty();
        }

        return findClearFieldsInResponse(pcsCase)
            .or(() -> findClearFieldsAtRoot(pcsCase));
    }

    private Optional<ClearFieldsContext> findClearFieldsInResponse(PCSCase pcsCase) {
        if (pcsCase.getPossessionClaimResponse() != null
            && hasEntries(pcsCase.getPossessionClaimResponse().getClearFields())) {
            return Optional.of(new ClearFieldsContext(
                "possessionClaimResponse",
                pcsCase.getPossessionClaimResponse().getClearFields()
            ));
        }
        return Optional.empty();
    }

    private Optional<ClearFieldsContext> findClearFieldsAtRoot(PCSCase pcsCase) {
        if (hasEntries(pcsCase.getClearFields())) {
            return Optional.of(new ClearFieldsContext("", pcsCase.getClearFields()));
        }
        return Optional.empty();
    }

    private boolean hasEntries(List<String> list) {
        return list != null && !list.isEmpty();
    }

    /**
     * Clears the specified fields from the merged JSON and strips the clearFields property itself.
     */
    public String applyClearFields(String mergedJson, ClearFieldsContext context) throws JsonProcessingException {
        ObjectNode root = parseJsonToTree(mergedJson);
        ObjectNode targetNode = navigateToRootNode(root, context.getRootNodeName());

        if (targetNode == null) {
            log.warn("Could not find root node '{}' for applying clearFields", context.getRootNodeName());
            return serializeJsonTree(root);
        }

        for (String fieldPath : context.getClearFields()) {
            removeField(targetNode, fieldPath);
        }

        removeClearFieldsProperty(root, context.getRootNodeName());
        return serializeJsonTree(root);
    }

    private ObjectNode navigateToRootNode(ObjectNode root, String rootNodeName) {
        if (rootNodeName == null || rootNodeName.isEmpty()) {
            return root;
        }

        JsonNode node = root.get(rootNodeName);
        if (node != null && node.isObject()) {
            return (ObjectNode) node;
        }

        return null;
    }

    private void removeField(ObjectNode targetNode, String fieldPath) {
        if (fieldPath == null || fieldPath.isEmpty()) {
            log.debug("Skipping empty field path");
            return;
        }

        String[] pathSegments = fieldPath.split("\\.");
        ObjectNode parentNode = navigateToParentNode(targetNode, pathSegments);

        if (parentNode == null) {
            log.debug("Could not navigate to parent node for field path: {}", fieldPath);
            return;
        }

        String fieldName = getFieldName(pathSegments);
        if (parentNode.has(fieldName)) {
            parentNode.remove(fieldName);
            log.debug("Removed field: {}", fieldPath);
        } else {
            log.debug("Field not found (already cleared or never set): {}", fieldPath);
        }
    }

    private ObjectNode navigateToParentNode(ObjectNode startNode, String[] pathSegments) {
        ObjectNode current = startNode;

        for (int i = 0; i < pathSegments.length - 1; i++) {
            JsonNode next = current.get(pathSegments[i]);
            if (next == null || !next.isObject()) {
                return null;
            }
            current = (ObjectNode) next;
        }

        return current;
    }

    private String getFieldName(String[] pathSegments) {
        return pathSegments[pathSegments.length - 1];
    }

    private void removeClearFieldsProperty(ObjectNode root, String rootNodeName) {
        ObjectNode target = navigateToRootNode(root, rootNodeName);
        if (target != null) {
            target.remove("clearFields");
        }
    }

    private ObjectNode parseJsonToTree(String json) throws JsonProcessingException {
        return (ObjectNode) objectMapper.readTree(json);
    }

    private String serializeJsonTree(ObjectNode root) throws JsonProcessingException {
        return objectMapper.writeValueAsString(root);
    }
}
