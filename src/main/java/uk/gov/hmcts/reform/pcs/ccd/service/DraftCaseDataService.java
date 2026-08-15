package uk.gov.hmcts.reform.pcs.ccd.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.entity.DraftCaseDataEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.repository.DraftCaseDataRepository;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationService;
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
    private final OrganisationService organisationService;
    private final ObjectMapper objectMapper;
    private final DraftCaseJsonMerger draftCaseJsonMerger;
    private final SecurityContextService securityContextService;

    public DraftCaseDataService(DraftCaseDataRepository draftCaseDataRepository,
                                OrganisationService organisationService,
                                @Qualifier("draftCaseDataObjectMapper") ObjectMapper objectMapper,
                                DraftCaseJsonMerger draftCaseJsonMerger,
                                SecurityContextService securityContextService) {
        this.draftCaseDataRepository = draftCaseDataRepository;
        this.organisationService = organisationService;
        this.objectMapper = objectMapper;
        this.draftCaseJsonMerger = draftCaseJsonMerger;
        this.securityContextService = securityContextService;
    }

    private UUID getCurrentUserId() {
        return UUID.fromString(securityContextService.getCurrentUserDetails().getUid());
    }

    /**
     * A user acting for an organisation shares one draft with their colleagues; a citizen keeps
     * their own. Keyed on the caller's organisation rather than the case's, so a draft is never
     * readable by an organisation that is not the caller's.
     */
    private Optional<String> currentUserOrganisationId() {
        return Optional.ofNullable(organisationService.getOrganisationIdForCurrentUser());
    }

    private Optional<DraftCaseDataEntity> findDraft(long caseReference, EventId eventId, UUID userId,
                                                    Optional<String> organisationId) {
        return organisationId
            .map(orgId -> draftCaseDataRepository
                .findByCaseReferenceAndEventIdAndOrganisationId(caseReference, eventId, orgId)
                .or(() -> adoptUserKeyedDraft(caseReference, eventId, userId, orgId)))
            .orElseGet(() -> draftCaseDataRepository
                .findByCaseReferenceAndEventIdAndIdamUserId(caseReference, eventId, userId));
    }

    /**
     * Drafts saved before organisation keying carry no organisation, so the owner lookup misses them
     * and the user would silently start again. The first open moves the row across.
     */
    private Optional<DraftCaseDataEntity> adoptUserKeyedDraft(long caseReference, EventId eventId, UUID userId,
                                                             String organisationId) {
        return draftCaseDataRepository
            .findByCaseReferenceAndEventIdAndIdamUserId(caseReference, eventId, userId)
            .map(draft -> {
                draft.setOrganisationId(organisationId);
                return draftCaseDataRepository.save(draft);
            });
    }

    public Optional<PCSCase> getUnsubmittedCaseData(long caseReference, EventId eventId) {
        UUID userId = getCurrentUserId();
        Optional<String> organisationId = currentUserOrganisationId();

        return getUnsubmittedCaseDataInternal(
            caseReference,
            eventId,
            userId,
            null,
            () -> findDraft(caseReference, eventId, userId, organisationId)
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
        Optional<String> organisationId = currentUserOrganisationId();

        return hasUnsubmittedCaseDataInternal(
            caseReference,
            eventId,
            userId,
            null,
            () -> draftExists(caseReference, eventId, userId, organisationId)
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

    /**
     * Reports a not-yet-adopted draft as present too, so the dashboard agrees with what opening the
     * journey will find. Adoption itself waits for that open rather than writing on a read.
     */
    private boolean draftExists(long caseReference, EventId eventId, UUID userId,
                                Optional<String> organisationId) {
        return organisationId
            .map(orgId -> draftCaseDataRepository
                .existsByCaseReferenceAndEventIdAndOrganisationId(caseReference, eventId, orgId))
            .orElse(false)
            || draftCaseDataRepository
                .existsByCaseReferenceAndEventIdAndIdamUserId(caseReference, eventId, userId);
    }

    /**
    * For dashboard display only. A respond draft may exist after START with only
    * claimant-populated contact details; that is not treated as "in progress".
    */
    public boolean hasMeaningfulRespondDraft(long caseReference, EventId eventId) {
        if (!hasUnsubmittedCaseData(caseReference, eventId)) {
            return false;
        }
        return getUnsubmittedCaseData(caseReference, eventId)
            .map(PCSCase::getPossessionClaimResponse)
            .map(PossessionClaimResponse::getDefendantResponses)
            .isPresent();
    }

    @Transactional
    public <T> void saveUnsubmittedEventData(long caseReference,
                                             T eventData,
                                             EventId eventId) {

        UUID userId = getCurrentUserId();
        Optional<String> organisationId = currentUserOrganisationId();

        saveUnsubmittedEventDataInternal(
            caseReference,
            eventData,
            eventId,
            userId,
            null,
            () -> findDraft(caseReference, eventId, userId, organisationId)
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
        Optional<String> organisationId = currentUserOrganisationId();
        patchInternal(
            caseReference,
            eventId,
            patchEventDataJson,
            userId,
            () -> findDraft(caseReference, eventId, userId, organisationId),
            () -> createNewDraft(
                caseReference, eventId, userId, patchEventDataJson, organisationId
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
        Optional<String> organisationId = currentUserOrganisationId();

        deleteUnsubmittedCaseDataInternal(
            caseReference,
            eventId,
            userId,
            null,
            () -> organisationId.ifPresentOrElse(
                orgId -> draftCaseDataRepository
                    .deleteByCaseReferenceAndEventIdAndOrganisationId(caseReference, eventId, orgId),
                () -> draftCaseDataRepository
                    .deleteByCaseReferenceAndEventIdAndIdamUserId(
                        caseReference, eventId, userId))
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

    /**
     * The organisation identifies the draft when present; idamUserId is still written either way,
     * so a shared draft records who last touched it.
     */
    private DraftCaseDataEntity createNewDraft(long caseReference, EventId eventId, UUID userId, String caseData,
                                               Optional<String> organisationId) {
        DraftCaseDataEntity newDraft = createNewDraft(caseReference, eventId, userId, caseData);
        organisationId.ifPresent(newDraft::setOrganisationId);
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
