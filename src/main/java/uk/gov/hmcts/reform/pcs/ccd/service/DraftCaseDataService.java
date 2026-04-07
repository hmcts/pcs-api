package uk.gov.hmcts.reform.pcs.ccd.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final DraftClearFieldsProcessor clearFieldsProcessor;

    public DraftCaseDataService(DraftCaseDataRepository draftCaseDataRepository,
                                @Qualifier("draftCaseDataObjectMapper") ObjectMapper objectMapper,
                                DraftCaseJsonMerger draftCaseJsonMerger,
                                SecurityContextService securityContextService,
                                DraftClearFieldsProcessor clearFieldsProcessor) {
        this.draftCaseDataRepository = draftCaseDataRepository;
        this.objectMapper = objectMapper;
        this.draftCaseJsonMerger = draftCaseJsonMerger;
        this.securityContextService = securityContextService;
        this.clearFieldsProcessor = clearFieldsProcessor;
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

        Optional<ClearFieldsContext> clearFieldsContext = clearFieldsProcessor.extractClearFieldsContext(eventData);
        String patchEventDataJson = writeCaseDataJson(eventData);
        patchUnsubmittedCaseData(caseReference, eventId, patchEventDataJson, clearFieldsContext);
    }

    public void patchUnsubmittedCaseData(long caseReference, EventId eventId,
                                          String patchEventDataJson, Optional<ClearFieldsContext> clearFieldsContext) {
        UUID userId = getCurrentUserId();

        // Step 1: Load existing draft JSON or use empty object as base
        String baseDraftJson = draftCaseDataRepository
            .findByCaseReferenceAndEventIdAndIdamUserId(caseReference, eventId, userId)
            .map(existingDraft -> {
                log.debug("Found existing draft for userId={}", userId);
                return existingDraft.getCaseData();
            })
            .orElseGet(() -> {
                log.debug("No existing draft found, using empty base for userId={}", userId);
                return "{}";
            });

        // Step 2: Merge patch into base
        String mergedJson = mergeCaseDataJson(baseDraftJson, patchEventDataJson);

        // Step 3: Apply clearFields if present (removes fields + strips clearFields property)
        final String finalJson = clearFieldsContext.isPresent()
            ? applyClearFieldsAndSerialize(mergedJson, clearFieldsContext.get())
            : mergedJson;

        // Step 4: Save final JSON
        DraftCaseDataEntity entityToSave = draftCaseDataRepository
            .findByCaseReferenceAndEventIdAndIdamUserId(caseReference, eventId, userId)
            .map(existingDraft -> {
                existingDraft.setCaseData(finalJson);
                return existingDraft;
            })
            .orElseGet(() -> createNewDraft(caseReference, eventId, userId, finalJson));

        DraftCaseDataEntity saved = draftCaseDataRepository.save(entityToSave);
        log.debug("Draft saved successfully: id={}, caseReference={}, eventId={}, userId={}",
            saved.getId(), saved.getCaseReference(), saved.getEventId(), saved.getIdamUserId());
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

    private String applyClearFieldsAndSerialize(String mergedJson, ClearFieldsContext context) {
        try {
            return clearFieldsProcessor.applyClearFields(mergedJson, context);
        } catch (JsonProcessingException e) {
            log.error("Failed to apply clearFields", e);
            throw new UnsubmittedDataException("Failed to clear fields", e);
        }
    }

}
