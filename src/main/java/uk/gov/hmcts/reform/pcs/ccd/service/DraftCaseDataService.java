package uk.gov.hmcts.reform.pcs.ccd.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.DraftCaseDataEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.repository.DraftCaseDataRepository;
import uk.gov.hmcts.reform.pcs.exception.UnsubmittedDataException;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class DraftCaseDataService {

    private final DraftCaseDataRepository draftCaseDataRepository;
    private final ObjectMapper objectMapper;
    private final DraftCaseJsonMerger draftCaseJsonMerger;
    private final SecurityContextService securityContextService;

    public DraftCaseDataService(DraftCaseDataRepository draftCaseDataRepository,
                                @Qualifier("draftCaseDataObjectMapper") ObjectMapper objectMapper,
                                DraftCaseJsonMerger draftCaseJsonMerger,
                                SecurityContextService securityContextService) {
        this.draftCaseDataRepository = draftCaseDataRepository;
        this.objectMapper = objectMapper;
        this.draftCaseJsonMerger = draftCaseJsonMerger;
        this.securityContextService = securityContextService;
    }

    private UUID getCurrentUserId() {
        return UUID.fromString(securityContextService.getCurrentUserDetails().getUid());
    }

    public Optional<PCSCase> getUnsubmittedCaseData(long caseReference, EventId eventId) {
        UUID userId = getCurrentUserId();
        log.info("Getting unsubmitted draft data: caseReference={}, eventId={}, userId={}",
            caseReference, eventId, userId);

        Optional<PCSCase> optionalCaseData = draftCaseDataRepository
            .findByCaseReferenceAndEventIdAndIdamUserId(caseReference, eventId, userId)
                .map(DraftCaseDataEntity::getCaseData)
                .map(this::parseCaseDataJson)
                .map(this::setUnsubmittedDataFlag);

        if (optionalCaseData.isPresent()) {
            log.debug("Found draft case data for caseReference={}, eventId={}, userId={}",
                caseReference, eventId, userId);
        } else {
            log.debug("No draft case data found for caseReference={}, eventId={}, userId={}",
                caseReference, eventId, userId);
        }

        return optionalCaseData;
    }

    public boolean hasUnsubmittedCaseData(long caseReference, EventId eventId) {
        UUID userId = getCurrentUserId();
        log.info("Checking if draft exists: caseReference={}, eventId={}, userId={}",
            caseReference, eventId, userId);

        boolean exists = draftCaseDataRepository.existsByCaseReferenceAndEventIdAndIdamUserId(
            caseReference, eventId, userId);

        log.debug("Draft exists check result: caseReference={}, eventId={}, userId={}, exists={}",
            caseReference, eventId, userId, exists);

        return exists;
    }

    public <T> void patchUnsubmittedEventData(long caseReference, T eventData, EventId eventId) {
        Objects.requireNonNull(eventData, "eventData must not be null");
        Objects.requireNonNull(eventId, "eventId must not be null");

        UUID userId = getCurrentUserId();
        log.info("Patching draft: caseReference={}, eventId={}, userId={}", caseReference, eventId, userId);

        List<String> clearFields = extractClearFields(eventData);
        String patchEventDataJson = writeCaseDataJson(eventData);
        patchUnsubmittedCaseData(caseReference, eventId, patchEventDataJson, clearFields);
    }

    public void patchUnsubmittedCaseData(long caseReference, EventId eventId, String patchEventDataJson) {
        UUID userId = getCurrentUserId();
        DraftCaseDataEntity draftCaseDataEntity = draftCaseDataRepository
            .findByCaseReferenceAndEventIdAndIdamUserId(caseReference, eventId, userId)
            .map(existingDraft -> {
                log.debug("Updating existing draft for userId={}", userId);
                existingDraft.setCaseData(mergeCaseDataJson(existingDraft.getCaseData(), patchEventDataJson));
                return existingDraft;
            }).orElseGet(() -> {
                log.debug("Creating new draft for caseReference={}, eventId={}, userId={}",
                    caseReference, eventId, userId);
                return createNewDraft(caseReference, eventId, userId, patchEventDataJson);
            });

        DraftCaseDataEntity saved = draftCaseDataRepository.save(draftCaseDataEntity);
        log.debug("Draft saved successfully: id={}, caseReference={}, eventId={}, userId={}",
            saved.getId(), saved.getCaseReference(), saved.getEventId(), saved.getIdamUserId());
    }

    public void patchUnsubmittedCaseData(long caseReference, EventId eventId,
                                          String patchEventDataJson, List<String> clearFields) {
        UUID userId = getCurrentUserId();
        DraftCaseDataEntity draftCaseDataEntity = draftCaseDataRepository
            .findByCaseReferenceAndEventIdAndIdamUserId(caseReference, eventId, userId)
            .map(existingDraft -> {
                String mergedJson = mergeCaseDataJson(existingDraft.getCaseData(), patchEventDataJson);

                if (clearFields != null && !clearFields.isEmpty()) {
                    mergedJson = applyClearFieldsAndSerialize(mergedJson, clearFields);
                }

                existingDraft.setCaseData(mergedJson);
                return existingDraft;
            }).orElseGet(() -> createNewDraft(caseReference, eventId, userId, patchEventDataJson));

        draftCaseDataRepository.save(draftCaseDataEntity);
    }

    private String mergeCaseDataJson(String baseCaseDataJson, String patchCaseDataJson) {
        try {
            return draftCaseJsonMerger.mergeJson(baseCaseDataJson, patchCaseDataJson);
        } catch (IOException e) {
            log.error("Unable to merge case data patch JSON", e);
            throw new UnsubmittedDataException("Failed to update draft case data", e);
        }
    }

    @Transactional
    public void deleteUnsubmittedCaseData(long caseReference, EventId eventId) {
        UUID userId = getCurrentUserId();
        log.info("Deleting draft: caseReference={}, eventId={}, userId={}", caseReference, eventId, userId);
        draftCaseDataRepository.deleteByCaseReferenceAndEventIdAndIdamUserId(caseReference, eventId, userId);
        log.debug("Draft deleted successfully for userId={}", userId);
    }

    public PCSCase parseCaseDataJson(String caseDataJson) {
        try {
            return objectMapper.readValue(caseDataJson, PCSCase.class);
        } catch (JsonProcessingException e) {
            log.error("Unable to parse draft case data JSON", e);
            throw new UnsubmittedDataException("Failed to read saved answers", e);
        }
    }

    private <T> String writeCaseDataJson(T caseData) {
        try {
            return objectMapper.writeValueAsString(caseData);
        } catch (JsonProcessingException e) {
            log.error("Unable to write draft case data JSON", e);
            throw new UnsubmittedDataException("Failed to save answers", e);
        }
    }

    private PCSCase setUnsubmittedDataFlag(PCSCase pcsCase) {
        pcsCase.setHasUnsubmittedCaseData(YesOrNo.YES);
        return pcsCase;
    }

    private DraftCaseDataEntity createNewDraft(long caseReference, EventId eventId,
                                                UUID userId, String caseData) {
        DraftCaseDataEntity newDraft = new DraftCaseDataEntity();
        newDraft.setCaseReference(caseReference);
        newDraft.setCaseData(caseData);
        newDraft.setEventId(eventId);
        newDraft.setIdamUserId(userId);
        return newDraft;
    }

    private <T> List<String> extractClearFields(T eventData) {
        if (eventData instanceof PCSCase) {
            PCSCase pcsCase = (PCSCase) eventData;
            if (pcsCase.getPossessionClaimResponse() != null) {
                List<String> clearFields = pcsCase.getPossessionClaimResponse().getClearFields();
                return clearFields != null ? clearFields : List.of();
            }
        }
        return List.of();
    }

    private String applyClearFieldsAndSerialize(String mergedJson, List<String> clearFields) {
        try {
            ObjectNode root = parseJsonToTree(mergedJson);

            clearFieldsFromPossessionClaimResponse(root, clearFields);
            return serializeJsonTree(root);
        } catch (JsonProcessingException e) {
            log.error("Failed to apply clearFields", e);
            throw new UnsubmittedDataException("Failed to clear fields", e);
        }
    }

    private ObjectNode parseJsonToTree(String json) throws JsonProcessingException {
        return (ObjectNode) objectMapper.readTree(json);
    }

    private void clearFieldsFromPossessionClaimResponse(ObjectNode root, List<String> clearFields) {
        JsonNode pcrNode = root.at("/possessionClaimResponse");
        if (!pcrNode.isObject()) {
            return;
        }

        ObjectNode possessionClaimResponse = (ObjectNode) pcrNode;

        for (String fieldPath : clearFields) {
            removeField(possessionClaimResponse, fieldPath);
        }

        possessionClaimResponse.remove("clearFields");

        removeAllNullFields(possessionClaimResponse);
    }

    private String serializeJsonTree(ObjectNode root) throws JsonProcessingException {
        return objectMapper.writeValueAsString(root);
    }

    private void removeField(ObjectNode root, String fieldPath) {
        String[] pathSegments = fieldPath.split("\\.");
        ObjectNode parentNode = navigateToParentNode(root, pathSegments);

        if (parentNode == null) {
            return;
        }

        String fieldName = getFieldName(pathSegments);
        parentNode.remove(fieldName);
    }

    private ObjectNode navigateToParentNode(ObjectNode root, String[] pathSegments) {
        ObjectNode current = root;

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

    private void removeAllNullFields(ObjectNode node) {
        List<String> fieldsToRemove = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();

        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String fieldName = field.getKey();
            JsonNode fieldValue = field.getValue();

            if (fieldValue.isNull()) {
                fieldsToRemove.add(fieldName);
            } else if (fieldValue.isObject()) {
                removeAllNullFields((ObjectNode) fieldValue);
            }
        }

        for (String fieldName : fieldsToRemove) {
            node.remove(fieldName);
        }
    }

}
