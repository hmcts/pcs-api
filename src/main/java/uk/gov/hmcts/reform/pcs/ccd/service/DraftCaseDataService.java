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
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

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

        return getUnsubmittedCaseDataInternal(
            caseReference,
            eventId,
            userId,
            null,
            () -> draftCaseDataRepository
                .findByCaseReferenceAndEventIdAndIdamUserId(
                    caseReference,
                    eventId,
                    userId
                )
        );
    }

    public Optional<PCSCase> getUnsubmittedCaseData(long caseReference,
                                                    EventId eventId,
                                                    UUID partyId) {
        UUID userId = getCurrentUserId();

        return getUnsubmittedCaseDataInternal(
            caseReference,
            eventId,
            userId,
            partyId,
            () -> draftCaseDataRepository
                .findByCaseReferenceAndEventIdAndIdamUserIdAndPartyId(
                    caseReference,
                    eventId,
                    userId,
                    partyId
                )
        );
    }

    public boolean hasUnsubmittedCaseData(long caseReference, EventId eventId) {
        UUID userId = getCurrentUserId();

        return hasUnsubmittedCaseDataInternal(
            caseReference,
            eventId,
            userId,
            null,
            () -> draftCaseDataRepository
                .existsByCaseReferenceAndEventIdAndIdamUserId(
                    caseReference,
                    eventId,
                    userId
                )
        );
    }

    public boolean hasUnsubmittedCaseData(long caseReference,
                                          EventId eventId,
                                          UUID partyId) {

        UUID userId = getCurrentUserId();

        return hasUnsubmittedCaseDataInternal(
            caseReference,
            eventId,
            userId,
            partyId,
            () -> draftCaseDataRepository
                .existsByCaseReferenceAndEventIdAndIdamUserIdAndPartyId(
                    caseReference,
                    eventId,
                    userId,
                    partyId
                )
        );
    }

    @Transactional
    public <T> void saveUnsubmittedEventData(long caseReference,
                                             T eventData,
                                             EventId eventId) {

        UUID userId = getCurrentUserId();

        saveUnsubmittedEventDataInternal(
            caseReference,
            eventData,
            eventId,
            userId,
            null,
            () -> draftCaseDataRepository
                .findByCaseReferenceAndEventIdAndIdamUserId(
                    caseReference,
                    eventId,
                    userId
                )
        );
    }

    @Transactional
    public <T> void saveUnsubmittedEventData(long caseReference,
                                             T eventData,
                                             EventId eventId,
                                             UUID partyId) {

        UUID userId = getCurrentUserId();

        saveUnsubmittedEventDataInternal(
            caseReference,
            eventData,
            eventId,
            userId,
            partyId,
            () -> draftCaseDataRepository
                .findByCaseReferenceAndEventIdAndIdamUserIdAndPartyId(
                    caseReference,
                    eventId,
                    userId,
                    partyId
                )
        );
    }

    private <T> void saveUnsubmittedEventDataInternal(long caseReference,
                                                      T eventData,
                                                      EventId eventId,
                                                      UUID userId,
                                                      UUID partyId,
                                                      Supplier<Optional<DraftCaseDataEntity>> draftSupplier) {

        Objects.requireNonNull(eventData, "eventData must not be null");
        Objects.requireNonNull(eventId, "eventId must not be null");

        if (partyId != null) {
            log.info(
                "Saving draft: caseReference={}, eventId={}, userId={}, partyId={}",
                caseReference,
                eventId,
                userId,
                partyId
            );
        } else {
            log.info(
                "Saving draft: caseReference={}, eventId={}, userId={}",
                caseReference,
                eventId,
                userId
            );
        }

        String eventDataJson = writeCaseDataJson(eventData);

        DraftCaseDataEntity draftCaseDataEntity = draftSupplier.get()
            .orElseThrow(() -> new UnsubmittedDataException(
                partyId != null ? "No draft found for caseReference=" + caseReference + ", eventId=" + eventId
                      + ", userId=" + userId + ", partyId=" + partyId
                    : "No draft found for caseReference=" + caseReference + ", eventId=" + eventId
                      + ", userId=" + userId));

        log.debug("Replacing existing draft for userId={}", userId);

        draftCaseDataEntity.setCaseData(eventDataJson);

        DraftCaseDataEntity saved = draftCaseDataRepository.save(draftCaseDataEntity);

        if (partyId != null) {
            log.debug(
                "Draft saved successfully: id={}, caseReference={}, eventId={}, userId={}, partyId={}",
                saved.getId(),
                saved.getCaseReference(),
                saved.getEventId(),
                saved.getIdamUserId(),
                partyId);
        } else {
            log.debug(
                "Draft saved successfully: id={}, caseReference={}, eventId={}, userId={}",
                saved.getId(),
                saved.getCaseReference(),
                saved.getEventId(),
                saved.getIdamUserId());
        }
    }

    public <T> void patchUnsubmittedEventData(long caseReference, T eventData, EventId eventId) {

        patchUnsubmittedEventDataInternal(caseReference, eventData, eventId, null);
    }

    public <T> void patchUnsubmittedEventData(long caseReference, T eventData, EventId eventId, UUID partyId) {

        patchUnsubmittedEventDataInternal(caseReference, eventData, eventId, partyId);
    }

    public void patchUnsubmittedCaseData(long caseReference, EventId eventId, String patchEventDataJson, UUID partyId) {
        UUID userId = getCurrentUserId();
        patchInternal(
            caseReference,
            eventId,
            patchEventDataJson,
            userId,
            () -> draftCaseDataRepository
                .findByCaseReferenceAndEventIdAndIdamUserIdAndPartyId(
                    caseReference, eventId, userId, partyId
                ),
            () -> createNewDraft(
                caseReference, eventId, userId, patchEventDataJson, partyId
            )
        );
    }

    public void patchUnsubmittedCaseData(long caseReference, EventId eventId, String patchEventDataJson) {
        UUID userId = getCurrentUserId();
        patchInternal(
            caseReference,
            eventId,
            patchEventDataJson,
            userId,
            () -> draftCaseDataRepository
                .findByCaseReferenceAndEventIdAndIdamUserId(
                    caseReference, eventId, userId
                ),
            () -> createNewDraft(
                caseReference, eventId, userId, patchEventDataJson
            )
        );
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

        deleteUnsubmittedCaseDataInternal(
            caseReference,
            eventId,
            userId,
            null,
            () -> draftCaseDataRepository
                .deleteByCaseReferenceAndEventIdAndIdamUserId(
                    caseReference,
                    eventId,
                    userId
                )
        );
    }

    @Transactional
    public void deleteUnsubmittedCaseData(long caseReference,
                                          EventId eventId,
                                          UUID partyId) {

        UUID userId = getCurrentUserId();

        deleteUnsubmittedCaseDataInternal(
            caseReference,
            eventId,
            userId,
            partyId,
            () -> draftCaseDataRepository
                .deleteByCaseReferenceAndEventIdAndIdamUserIdAndPartyId(
                    caseReference,
                    eventId,
                    userId,
                    partyId
                )
        );
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

    private DraftCaseDataEntity createNewDraft(long caseReference, EventId eventId, UUID userId, String caseData) {
        DraftCaseDataEntity newDraft = new DraftCaseDataEntity();
        newDraft.setCaseReference(caseReference);
        newDraft.setCaseData(caseData);
        newDraft.setEventId(eventId);
        newDraft.setIdamUserId(userId);
        return newDraft;
    }

    private DraftCaseDataEntity createNewDraft(long caseReference, EventId eventId, UUID userId, String caseData,
                                               UUID partyId) {
        DraftCaseDataEntity newDraft = createNewDraft(caseReference, eventId, userId, caseData);
        newDraft.setPartyId(partyId);
        return newDraft;
    }

    private Optional<PCSCase> getUnsubmittedCaseDataInternal(
        long caseReference,
        EventId eventId,
        UUID userId,
        UUID partyId,
        Supplier<Optional<DraftCaseDataEntity>> draftSupplier
    ) {

        if (partyId != null) {
            log.info(
                "Getting unsubmitted draft data: caseReference={}, eventId={}, userId={}, partyId={}",
                caseReference,
                eventId,
                userId,
                partyId
            );
        } else {
            log.info(
                "Getting unsubmitted draft data: caseReference={}, eventId={}, userId={}",
                caseReference,
                eventId,
                userId
            );
        }

        Optional<PCSCase> optionalCaseData = draftSupplier.get()
            .map(DraftCaseDataEntity::getCaseData)
            .map(this::parseCaseDataJson)
            .map(this::setUnsubmittedDataFlag);

        if (partyId != null) {
            if (optionalCaseData.isPresent()) {
                log.debug(
                    "Found draft case data for caseReference={}, eventId={}, userId={}, partyId={}",
                    caseReference,
                    eventId,
                    userId,
                    partyId
                );
            } else {
                log.debug(
                    "No draft case data found for caseReference={}, eventId={}, userId={}, partyId={}",
                    caseReference,
                    eventId,
                    userId,
                    partyId
                );
            }
        } else {
            if (optionalCaseData.isPresent()) {
                log.debug(
                    "Found draft case data for caseReference={}, eventId={}, userId={}",
                    caseReference,
                    eventId,
                    userId
                );
            } else {
                log.debug(
                    "No draft case data found for caseReference={}, eventId={}, userId={}",
                    caseReference,
                    eventId,
                    userId
                );
            }
        }

        return optionalCaseData;
    }

    private void deleteUnsubmittedCaseDataInternal(
        long caseReference,
        EventId eventId,
        UUID userId,
        UUID partyId,
        Runnable deleteAction
    ) {

        if (partyId != null) {
            log.info(
                "Deleting draft: caseReference={}, eventId={}, userId={}, partyId={}",
                caseReference,
                eventId,
                userId,
                partyId
            );
        } else {
            log.info(
                "Deleting draft: caseReference={}, eventId={}, userId={}",
                caseReference,
                eventId,
                userId
            );
        }

        deleteAction.run();

        if (partyId != null) {
            log.debug(
                "Draft deleted successfully for userId={} and partyId={}",
                userId,
                partyId
            );
        } else {
            log.debug(
                "Draft deleted successfully for userId={}",
                userId
            );
        }
    }

    private boolean hasUnsubmittedCaseDataInternal(
        long caseReference,
        EventId eventId,
        UUID userId,
        UUID partyId,
        BooleanSupplier existsSupplier
    ) {

        if (partyId != null) {
            log.info(
                "Checking if draft exists: caseReference={}, eventId={}, userId={}, partyId={}",
                caseReference,
                eventId,
                userId,
                partyId
            );
        } else {
            log.info(
                "Checking if draft exists: caseReference={}, eventId={}, userId={}",
                caseReference,
                eventId,
                userId
            );
        }

        boolean exists = existsSupplier.getAsBoolean();

        if (partyId != null) {
            log.debug(
                "Draft exists check result: caseReference={}, eventId={}, userId={}, partyId={}, exists={}",
                caseReference,
                eventId,
                userId,
                partyId,
                exists
            );
        } else {
            log.debug(
                "Draft exists check result: caseReference={}, eventId={}, userId={}, exists={}",
                caseReference,
                eventId,
                userId,
                exists
            );
        }

        return exists;
    }

    private <T> void patchUnsubmittedEventDataInternal(long caseReference,
                                                       T eventData,
                                                       EventId eventId,
                                                       UUID partyId) {

        Objects.requireNonNull(eventData, "eventData must not be null");
        Objects.requireNonNull(eventId, "eventId must not be null");

        UUID userId = getCurrentUserId();

        if (partyId != null) {
            log.info("Patching draft: caseReference={}, eventId={}, userId={}, partyId={}",
                     caseReference, eventId, userId, partyId);
        } else {
            log.info("Patching draft: caseReference={}, eventId={}, userId={}",
                     caseReference, eventId, userId);
        }

        String patchEventDataJson = writeCaseDataJson(eventData);

        if (partyId != null) {
            patchUnsubmittedCaseData(caseReference, eventId, patchEventDataJson, partyId);
        } else {
            patchUnsubmittedCaseData(caseReference, eventId, patchEventDataJson);
        }
    }

    private void patchInternal(long caseReference,
                               EventId eventId,
                               String patchEventDataJson,
                               UUID userId,
                               Supplier<Optional<DraftCaseDataEntity>> findDraft,
                               Supplier<DraftCaseDataEntity> createDraft) {

        DraftCaseDataEntity draftCaseDataEntity = findDraft.get()
            .map(existingDraft -> {
                log.debug("Updating existing draft for userId={}", userId);
                existingDraft.setCaseData(
                    mergeCaseDataJson(existingDraft.getCaseData(), patchEventDataJson)
                );
                return existingDraft;
            })
            .orElseGet(() -> {
                log.debug(
                    "Creating new draft for caseReference={}, eventId={}, userId={}",
                    caseReference, eventId, userId
                );
                return createDraft.get();
            });
        draftCaseDataRepository.save(draftCaseDataEntity);
    }
}
