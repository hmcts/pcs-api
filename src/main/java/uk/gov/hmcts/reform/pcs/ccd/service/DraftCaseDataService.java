package uk.gov.hmcts.reform.pcs.ccd.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
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
import uk.gov.hmcts.reform.pcs.exception.UnsubmittedDataException;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.io.IOException;
import java.util.Date;
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
            DraftCaseData.builder().caseReference(caseReference).eventId(eventId).userId(userId).build(),
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
                                                    UUID partyId,
                                                    String legalRepresentativeOrganisationId) {

        return getUnsubmittedCaseDataInternal(
            DraftCaseData.builder().caseReference(caseReference).eventId(eventId)
                .organisationId(legalRepresentativeOrganisationId).partyId(partyId).build(),
            () -> draftCaseDataRepository
                .findByCaseReferenceAndEventIdAndLegalRepresentativeOrganisationIdAndPartyId(
                    caseReference,
                    eventId,
                    legalRepresentativeOrganisationId,
                    partyId
                )
        );
    }

    public boolean hasUnsubmittedCaseData(long caseReference, EventId eventId) {
        UUID userId = getCurrentUserId();


        return hasUnsubmittedCaseDataInternal(
            DraftCaseData.builder().caseReference(caseReference).eventId(eventId).userId(userId).build(),
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
                                          UUID partyId,
                                          String legalRepresentativeOrganisationId) {

        return hasUnsubmittedCaseDataInternal(
            DraftCaseData.builder().caseReference(caseReference).eventId(eventId).partyId(partyId)
                .organisationId(legalRepresentativeOrganisationId).build(),
            () -> draftCaseDataRepository
                .existsByCaseReferenceAndEventIdAndLegalRepresentativeOrganisationIdAndPartyId(
                    caseReference,
                    eventId,
                    legalRepresentativeOrganisationId,
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

        saveUnsubmittedEventDataInternal(
            eventData,
            DraftCaseData.builder().caseReference(caseReference).eventId(eventId).userId(userId).build(),
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
                                             UUID partyId,
                                             String legalRepresentativeOrganisationId) {


        saveUnsubmittedEventDataInternal(
            eventData,
            DraftCaseData.builder().caseReference(caseReference).eventId(eventId)
                .organisationId(legalRepresentativeOrganisationId).partyId(partyId).build(),
            () -> draftCaseDataRepository
                .findByCaseReferenceAndEventIdAndLegalRepresentativeOrganisationIdAndPartyId(
                    caseReference,
                    eventId,
                    legalRepresentativeOrganisationId,
                    partyId
                )
        );
    }

    private <T> void saveUnsubmittedEventDataInternal(T eventData,
                                                      DraftCaseData draftCaseData,
                                                      Supplier<Optional<DraftCaseDataEntity>> draftSupplier) {

        Objects.requireNonNull(eventData, "eventData must not be null");
        Objects.requireNonNull(draftCaseData.getEventId(), "eventId must not be null");

        if (draftCaseData.getPartyId() != null) {
            log.info(
                "Saving draft: caseReference={}, eventId={}, organisationId={}, partyId={}",
                draftCaseData.getCaseReference(),
                draftCaseData.getEventId(),
                draftCaseData.getOrganisationId(),
                draftCaseData.getPartyId()
            );
        } else {
            log.info(
                "Saving draft: caseReference={}, eventId={}, userId={}",
                draftCaseData.getCaseReference(),
                draftCaseData.getEventId(),
                draftCaseData.getUserId()
            );
        }

        String eventDataJson = writeCaseDataJson(eventData);

        DraftCaseDataEntity draftCaseDataEntity = draftSupplier.get()
            .orElseThrow(() -> new UnsubmittedDataException(
                draftCaseData.getPartyId() != null ? "No draft found for caseReference=" +
                    draftCaseData.getCaseReference() + ", eventId=" + draftCaseData.getEventId()
                      + ", organisationId=" + draftCaseData.getOrganisationId() + ", partyId=" +
                    draftCaseData.getPartyId()
                    : "No draft found for caseReference=" + draftCaseData.getCaseReference() + ", eventId=" +
                    draftCaseData.getEventId()
                      + ", userId=" + draftCaseData.getUserId()));

        if (draftCaseData.getPartyId() != null) {
            log.debug("Replacing existing draft for organisationId={}, partyId={}", draftCaseData.getOrganisationId(),
                      draftCaseData.getPartyId());
        } else {
            log.debug("Replacing existing draft for userId={}", draftCaseData.getUserId());
        }

        draftCaseDataEntity.setCaseData(eventDataJson);

        DraftCaseDataEntity saved = draftCaseDataRepository.save(draftCaseDataEntity);

        if (draftCaseData.getPartyId() != null) {
            log.debug(
                "Draft saved successfully: id={}, caseReference={}, eventId={}, organisationId={}, partyId={}",
                saved.getId(),
                saved.getCaseReference(),
                saved.getEventId(),
                saved.getLegalRepresentativeOrganisationId(),
                saved.getPartyId());
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


        patchUnsubmittedEventDataInternal(DraftCaseData.builder().caseReference(caseReference)
                                              .eventId(eventId).build(), eventData);
    }

    public <T> void patchUnsubmittedEventData(long caseReference, T eventData, EventId eventId, UUID partyId,
                                              String legalRepresentativeOrganisationId) {

        patchUnsubmittedEventDataInternal(DraftCaseData.builder().caseReference(caseReference).eventId(eventId)
                                              .partyId(partyId).organisationId(legalRepresentativeOrganisationId)
                                              .build(), eventData);
    }

    public void patchUnsubmittedCaseData(long caseReference, EventId eventId, String patchEventDataJson, UUID partyId,
                                         String legalRepresentativeOrganisationId) {
        patchInternal(
            DraftCaseData.builder().caseReference(caseReference).eventId(eventId).partyId(partyId)
                .organisationId(legalRepresentativeOrganisationId).build(),
            patchEventDataJson,
            () -> draftCaseDataRepository
                .findByCaseReferenceAndEventIdAndLegalRepresentativeOrganisationIdAndPartyId(
                    caseReference, eventId, legalRepresentativeOrganisationId, partyId
                ),
            () -> createNewDraft(
                caseReference, eventId, legalRepresentativeOrganisationId, patchEventDataJson, partyId
            )
        );
    }

    public void patchUnsubmittedCaseData(long caseReference, EventId eventId, String patchEventDataJson) {
        UUID userId = getCurrentUserId();
        patchInternal(
            DraftCaseData.builder().caseReference(caseReference).eventId(eventId).userId(userId).build(),
            patchEventDataJson,
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
            DraftCaseData.builder().caseReference(caseReference).eventId(eventId).userId(userId).build(),
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
                                          UUID partyId,
                                          String organisationId) {

        deleteUnsubmittedCaseDataInternal(
            DraftCaseData.builder().caseReference(caseReference).eventId(eventId).partyId(partyId)
                .organisationId(organisationId).build(),
            () -> draftCaseDataRepository
                .deleteByCaseReferenceAndEventIdAndLegalRepresentativeOrganisationIdAndPartyId(
                    caseReference,
                    eventId,
                    organisationId,
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

    private DraftCaseDataEntity createNewDraft(long caseReference, EventId eventId, String organisationId,
                                               String caseData, UUID partyId) {
        DraftCaseDataEntity newDraft = new DraftCaseDataEntity();
        newDraft.setCaseReference(caseReference);
        newDraft.setCaseData(caseData);
        newDraft.setEventId(eventId);
        newDraft.setPartyId(partyId);
        newDraft.setLegalRepresentativeOrganisationId(organisationId);
        return newDraft;
    }

    private Optional<PCSCase> getUnsubmittedCaseDataInternal(
        DraftCaseData draftCaseData,
        Supplier<Optional<DraftCaseDataEntity>> draftSupplier
    ) {

        if (draftCaseData.getPartyId() != null) {
            log.info(
                "Getting unsubmitted draft data: caseReference={}, eventId={}, userId={}, partyId={}",
                draftCaseData.getCaseReference(),
                draftCaseData.getEventId(),
                draftCaseData.getOrganisationId(),
                draftCaseData.getPartyId()
            );
        } else {
            log.info(
                "Getting unsubmitted draft data: caseReference={}, eventId={}, userId={}",
                draftCaseData.getCaseReference(),
                draftCaseData.getEventId(),
                draftCaseData.getUserId()
            );
        }

        Optional<PCSCase> optionalCaseData = draftSupplier.get()
            .map(DraftCaseDataEntity::getCaseData)
            .map(this::parseCaseDataJson)
            .map(this::setUnsubmittedDataFlag);

        if (draftCaseData.getPartyId() != null) {
            if (optionalCaseData.isPresent()) {
                log.debug(
                    "Found draft case data for caseReference={}, eventId={}, userId={}, partyId={}",
                    draftCaseData.getCaseReference(),
                    draftCaseData.getEventId(),
                    draftCaseData.getOrganisationId(),
                    draftCaseData.getPartyId()
                );
            } else {
                log.debug(
                    "No draft case data found for caseReference={}, eventId={}, userId={}, partyId={}",
                    draftCaseData.getCaseReference(),
                    draftCaseData.getEventId(),
                    draftCaseData.getOrganisationId(),
                    draftCaseData.getPartyId()
                );
            }
        } else {
            if (optionalCaseData.isPresent()) {
                log.debug(
                    "Found draft case data for caseReference={}, eventId={}, userId={}",
                    draftCaseData.getCaseReference(),
                    draftCaseData.getEventId(),
                    draftCaseData.getUserId()
                );
            } else {
                log.debug(
                    "No draft case data found for caseReference={}, eventId={}, userId={}",
                    draftCaseData.getCaseReference(),
                    draftCaseData.getEventId(),
                    draftCaseData.getUserId()
                );
            }
        }

        return optionalCaseData;
    }

    private void deleteUnsubmittedCaseDataInternal(
        DraftCaseData draftCaseData,
        Runnable deleteAction
    ) {

        if (draftCaseData.getPartyId() != null) {
            log.info(
                "Deleting draft: caseReference={}, eventId={}, organisationId={}, partyId={}",
                draftCaseData.getCaseReference(),
                draftCaseData.getEventId(),
                draftCaseData.getOrganisationId(),
                draftCaseData.getPartyId()
            );
        } else {
            log.info(
                "Deleting draft: caseReference={}, eventId={}, userId={}",
                draftCaseData.getCaseReference(),
                draftCaseData.getEventId(),
                draftCaseData.getUserId()
            );
        }

        deleteAction.run();

        if (draftCaseData.getPartyId() != null) {
            log.debug(
                "Draft deleted successfully for organisationId={} and partyId={}",
                draftCaseData.getOrganisationId(),
                draftCaseData.getPartyId()
            );
        } else {
            log.debug(
                "Draft deleted successfully for userId={}",
                draftCaseData.getUserId()
            );
        }
    }

    private boolean hasUnsubmittedCaseDataInternal(
        DraftCaseData draftCaseData,
        BooleanSupplier existsSupplier
    ) {

        if (draftCaseData.getPartyId() != null) {
            log.info(
                "Checking if draft exists: caseReference={}, eventId={}, userId={}, partyId={}",
                draftCaseData.getCaseReference(),
                draftCaseData.getEventId(),
                draftCaseData.getOrganisationId(),
                draftCaseData.getPartyId()
            );
        } else {
            log.info(
                "Checking if draft exists: caseReference={}, eventId={}, userId={}",
                draftCaseData.getCaseReference(),
                draftCaseData.getEventId(),
                draftCaseData.getUserId()
            );
        }

        boolean exists = existsSupplier.getAsBoolean();

        if (draftCaseData.getPartyId() != null) {
            log.debug(
                "Draft exists check result: caseReference={}, eventId={}, userId={}, partyId={}, exists={}",
                draftCaseData.getCaseReference(),
                draftCaseData.getEventId(),
                draftCaseData.getOrganisationId(),
                draftCaseData.getPartyId(),
                exists
            );
        } else {
            log.debug(
                "Draft exists check result: caseReference={}, eventId={}, userId={}, exists={}",
                draftCaseData.getCaseReference(),
                draftCaseData.getEventId(),
                draftCaseData.getUserId(),
                exists
            );
        }

        return exists;
    }

    private <T> void patchUnsubmittedEventDataInternal(DraftCaseData draftCaseData,
                                                       T eventData) {

        Objects.requireNonNull(eventData, "eventData must not be null");
        Objects.requireNonNull(draftCaseData.getEventId(), "eventId must not be null");


        if (draftCaseData.getPartyId() != null) {
            log.info("Patching draft: caseReference={}, eventId={}, legalRepresentativeOrganisationId={}, partyId={}",
                     draftCaseData.getCaseReference(),
                     draftCaseData.getEventId(),
                     draftCaseData.getOrganisationId(),
                     draftCaseData.getPartyId());
        } else {
            UUID userId = getCurrentUserId();
            log.info("Patching draft: caseReference={}, eventId={}, userId={}",
                     draftCaseData.getCaseReference(),
                     draftCaseData.getEventId(),
                     userId);
        }

        String patchEventDataJson = writeCaseDataJson(eventData);

        if (draftCaseData.getPartyId() != null) {
            patchUnsubmittedCaseData(draftCaseData.getCaseReference(), draftCaseData.getEventId(), patchEventDataJson,
                                     draftCaseData.getPartyId(), draftCaseData.getOrganisationId());
        } else {
            patchUnsubmittedCaseData(draftCaseData.getCaseReference(), draftCaseData.getEventId(), patchEventDataJson);
        }
    }

    private void patchInternal(DraftCaseData draftCaseData,
                               String patchEventDataJson,
                               Supplier<Optional<DraftCaseDataEntity>> findDraft,
                               Supplier<DraftCaseDataEntity> createDraft) {

        DraftCaseDataEntity draftCaseDataEntity = findDraft.get()
            .map(existingDraft -> {
                log.debug(
                    draftCaseData.getPartyId() != null ?
                        "Updating existing draft for organisationId=" + draftCaseData.getOrganisationId() :
                        "Updating existing draft for userId=" + draftCaseData.getUserId());
                existingDraft.setCaseData(
                    mergeCaseDataJson(existingDraft.getCaseData(), patchEventDataJson)
                );
                return existingDraft;
            })
            .orElseGet(() -> {
                log.debug(
                    draftCaseData.getPartyId() != null ?
                    "Creating new draft for caseReference=" + draftCaseData.getCaseReference() + "eventId="
                    + draftCaseData.getEventId() + "organisationId=" + draftCaseData.getOrganisationId()
                    + "partyId=" + draftCaseData.getPartyId() :
                        "Creating new draft for caseReference=" + draftCaseData.getCaseReference() + "eventId="
                        + draftCaseData.getEventId() + "userId=" + draftCaseData.getUserId()
                );
                return createDraft.get();
            });
        draftCaseDataRepository.save(draftCaseDataEntity);
    }


}
