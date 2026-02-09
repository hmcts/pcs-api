package uk.gov.hmcts.reform.pcs.ccd.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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

        Optional<DraftCaseDataEntity> optionalCaseData = draftCaseDataRepository
            .findByCaseReferenceAndEventIdAndIdamUserId(caseReference, eventId, userId);
        PCSCase pcsCase = null;
        if (optionalCaseData.isPresent()) {
            log.debug("Found draft case data for caseReference={}, eventId={}, userId={}",
                caseReference, eventId, userId);
            String caseDataJson = optionalCaseData.get().getCaseData();
            optionalCaseData = Optional.empty();
            pcsCase = parseCaseDataJson(caseDataJson);
            setUnsubmittedDataFlag(pcsCase);
        } else {
            log.debug("No draft case data found for caseReference={}, eventId={}, userId={}",
                caseReference, eventId, userId);
        }
        return Optional.ofNullable(pcsCase);
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

        String patchEventDataJson = writeCaseDataJson(eventData);
        patchUnsubmittedCaseData(caseReference, eventId, patchEventDataJson);
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

    private String mergeCaseDataJson(String baseCaseDataJson, String patchCaseDataJson) {
        try {
            return draftCaseJsonMerger.mergeJson(baseCaseDataJson, patchCaseDataJson);
        } catch (IOException e) {
            log.error("Unable to merge case data patch JSON", e);
            throw new UnsubmittedDataException("Failed to update draft case data", e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
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

}
