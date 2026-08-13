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
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.repository.DraftCaseDataRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.exception.UnsubmittedDataException;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.io.IOException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import static java.util.Map.entry;

@Service
@Slf4j
public class DraftCaseDataService {

    /** HDPI-6221 keys drafts on the user so a citizen and their legal rep do not share one. */
    enum DraftOwnership {
        PARTY,
        USER,
        NO_DRAFTS
    }

    private static final Map<EventId, DraftOwnership> OWNERSHIP = new EnumMap<>(Map.ofEntries(
        entry(EventId.resumePossessionClaim, DraftOwnership.PARTY),
        entry(EventId.respondPossessionClaim, DraftOwnership.USER),
        // Claimant-owned by the same reasoning as making a claim, and should become PARTY. Left as
        // USER until that change is agreed, since it widens behaviour beyond the claim journey.
        entry(EventId.enforceTheOrder, DraftOwnership.USER),
        entry(EventId.createPossessionClaim, DraftOwnership.NO_DRAFTS),
        entry(EventId.submitDefendantResponse, DraftOwnership.NO_DRAFTS),
        entry(EventId.makeAnApplication, DraftOwnership.NO_DRAFTS),
        entry(EventId.createTestCase, DraftOwnership.NO_DRAFTS),
        entry(EventId.createCaseLink, DraftOwnership.NO_DRAFTS),
        entry(EventId.maintainCaseLink, DraftOwnership.NO_DRAFTS),
        entry(EventId.dashboardView, DraftOwnership.NO_DRAFTS),
        entry(EventId.confirmEviction, DraftOwnership.NO_DRAFTS),
        entry(EventId.uploadDocuments, DraftOwnership.NO_DRAFTS),
        entry(EventId.legalRepDocumentUpload, DraftOwnership.NO_DRAFTS),
        entry(EventId.amendDocuments, DraftOwnership.NO_DRAFTS),
        entry(EventId.addCaseNote, DraftOwnership.NO_DRAFTS),
        entry(EventId.addCaseReviewDate, DraftOwnership.NO_DRAFTS),
        entry(EventId.createFlags, DraftOwnership.NO_DRAFTS),
        entry(EventId.amendFlags, DraftOwnership.NO_DRAFTS),
        entry(EventId.claimIssuePayment, DraftOwnership.NO_DRAFTS),
        entry(EventId.changeCaseState, DraftOwnership.NO_DRAFTS),
        entry(EventId.enterGenApp, DraftOwnership.NO_DRAFTS),
        entry(EventId.caseworkerUploadDocuments, DraftOwnership.NO_DRAFTS),
        entry(EventId.removeDocument, DraftOwnership.NO_DRAFTS)
    ));

    static {
        failIfAnyEventIsUnclassified();
    }

    /**
     * An unclassified event would quietly fall back to a per-user draft, which is the defect this
     * classification exists to prevent, so it stops the service starting instead.
     */
    private static void failIfAnyEventIsUnclassified() {
        if (!OWNERSHIP.keySet().equals(EnumSet.allOf(EventId.class))) {
            throw new IllegalStateException(
                "Draft ownership not classified for " + EnumSet.complementOf(EnumSet.copyOf(OWNERSHIP.keySet()))
                    + ". Every EventId must be classified; use NO_DRAFTS for journeys that never save a draft.");
        }
    }

    private final DraftCaseDataRepository draftCaseDataRepository;
    private final PcsCaseRepository pcsCaseRepository;
    private final ObjectMapper objectMapper;
    private final DraftCaseJsonMerger draftCaseJsonMerger;
    private final SecurityContextService securityContextService;

    public DraftCaseDataService(DraftCaseDataRepository draftCaseDataRepository,
                                PcsCaseRepository pcsCaseRepository,
                                @Qualifier("draftCaseDataObjectMapper") ObjectMapper objectMapper,
                                DraftCaseJsonMerger draftCaseJsonMerger,
                                SecurityContextService securityContextService) {
        this.draftCaseDataRepository = draftCaseDataRepository;
        this.pcsCaseRepository = pcsCaseRepository;
        this.objectMapper = objectMapper;
        this.draftCaseJsonMerger = draftCaseJsonMerger;
        this.securityContextService = securityContextService;
    }

    private UUID getCurrentUserId() {
        return UUID.fromString(securityContextService.getCurrentUserDetails().getUid());
    }

    /**
     * Throws rather than falling back to the user key: keying on the user would bring back the
     * per-user split with no symptom but "my colleague cannot see my answers".
     */
    private Optional<String> ownerOrganisationId(long caseReference, EventId eventId) {
        DraftOwnership ownership = OWNERSHIP.get(eventId);

        if (ownership == DraftOwnership.NO_DRAFTS) {
            throw new IllegalStateException(
                eventId + " is classified as NO_DRAFTS but reached the draft service. Classify it as "
                    + "PARTY or USER.");
        }
        if (ownership != DraftOwnership.PARTY) {
            return Optional.empty();
        }

        List<String> organisationIds = pcsCaseRepository.findOwningOrganisationIds(caseReference, PartyRole.CLAIMANT);

        if (organisationIds.isEmpty()) {
            throw new IllegalStateException(
                "Case " + caseReference + " has no organisation-owned claimant party to own its draft");
        }
        if (organisationIds.size() > 1) {
            throw new IllegalStateException(
                "Case " + caseReference + " is owned by " + organisationIds.size() + " organisations; "
                    + "cannot determine which owns the draft");
        }

        return Optional.of(organisationIds.getFirst());
    }

    private Optional<DraftCaseDataEntity> findDraft(long caseReference, EventId eventId, UUID userId,
                                                    Optional<String> ownerOrganisationId) {
        return ownerOrganisationId
            .map(organisationId -> draftCaseDataRepository
                .findByCaseReferenceAndEventIdAndOrganisationId(caseReference, eventId, organisationId))
            .orElseGet(() -> draftCaseDataRepository
                .findByCaseReferenceAndEventIdAndIdamUserIdAndPartyIdIsNullAndOrganisationIdIsNull(
                    caseReference, eventId, userId));
    }

    public Optional<PCSCase> getUnsubmittedCaseData(long caseReference, EventId eventId) {
        UUID userId = getCurrentUserId();
        Optional<String> ownerOrganisationId = ownerOrganisationId(caseReference, eventId);

        return getUnsubmittedCaseDataInternal(
            caseReference,
            eventId,
            userId,
            null,
            () -> findDraft(caseReference, eventId, userId, ownerOrganisationId)
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
        Optional<String> ownerOrganisationId = ownerOrganisationId(caseReference, eventId);

        return hasUnsubmittedCaseDataInternal(
            caseReference,
            eventId,
            userId,
            null,
            () -> ownerOrganisationId
                .map(organisationId -> draftCaseDataRepository
                    .existsByCaseReferenceAndEventIdAndOrganisationId(caseReference, eventId, organisationId))
                .orElseGet(() -> draftCaseDataRepository
                    .existsByCaseReferenceAndEventIdAndIdamUserIdAndPartyIdIsNullAndOrganisationIdIsNull(
                        caseReference, eventId, userId))
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
        Optional<String> ownerOrganisationId = ownerOrganisationId(caseReference, eventId);

        saveUnsubmittedEventDataInternal(
            caseReference,
            eventData,
            eventId,
            userId,
            null,
            () -> findDraft(caseReference, eventId, userId, ownerOrganisationId)
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
        Optional<String> ownerOrganisationId = ownerOrganisationId(caseReference, eventId);
        patchInternal(
            caseReference,
            eventId,
            patchEventDataJson,
            userId,
            () -> findDraft(caseReference, eventId, userId, ownerOrganisationId),
            () -> createNewDraft(
                caseReference, eventId, userId, patchEventDataJson, ownerOrganisationId
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
        Optional<String> ownerOrganisationId = ownerOrganisationId(caseReference, eventId);

        deleteUnsubmittedCaseDataInternal(
            caseReference,
            eventId,
            userId,
            null,
            () -> ownerOrganisationId.ifPresentOrElse(
                organisationId -> draftCaseDataRepository
                    .deleteByCaseReferenceAndEventIdAndOrganisationId(caseReference, eventId, organisationId),
                () -> draftCaseDataRepository
                    .deleteByCaseReferenceAndEventIdAndIdamUserIdAndPartyIdIsNullAndOrganisationIdIsNull(
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
     * The owning organisation identifies the draft when present; idamUserId is still written either
     * way, so a shared draft records who last touched it.
     */
    private DraftCaseDataEntity createNewDraft(long caseReference, EventId eventId, UUID userId, String caseData,
                                               Optional<String> ownerOrganisationId) {
        DraftCaseDataEntity newDraft = createNewDraft(caseReference, eventId, userId, caseData);
        ownerOrganisationId.ifPresent(newDraft::setOrganisationId);
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
