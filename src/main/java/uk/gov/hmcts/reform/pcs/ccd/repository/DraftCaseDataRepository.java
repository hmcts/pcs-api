package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.ccd.entity.DraftCaseDataEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DraftCaseDataRepository extends JpaRepository<DraftCaseDataEntity, Integer> {

    // PartyIdIsNull is load-bearing: LR drafts share case/event/org but carry a party;
    // without it a firm's defendant draft matches here (and two defendants -> non-unique).
    Optional<DraftCaseDataEntity> findByCaseReferenceAndEventIdAndOrganisationIdAndPartyIdIsNull(
        long caseReference, EventId eventId, String organisationId);

    List<DraftCaseDataEntity> findByCaseReferenceAndEventId(
            long caseReference, EventId eventId);

    boolean existsByCaseReferenceAndEventIdAndOrganisationIdAndPartyIdIsNull(
        long caseReference, EventId eventId, String organisationId);

    void deleteByCaseReferenceAndEventIdAndOrganisationIdAndPartyIdIsNull(
        long caseReference, EventId eventId, String organisationId);

    void deleteByCaseReferenceAndEventIdAndIdamUserId(
        long caseReference, EventId eventId, UUID idamUserId);

    void deleteByCaseReferenceAndEventIdAndOrganisationIdAndPartyId(
        long caseReference, EventId eventId, String legalRepresentativeOrganisationId, UUID partyId);

    boolean existsByCaseReferenceAndEventIdAndOrganisationIdAndPartyId(
        long caseReference, EventId eventId, String legalRepresentativeOrganisationId, UUID partId);

    boolean existsByCaseReferenceAndEventIdAndIdamUserIdAndPartyId(
        long caseReference, EventId eventId, UUID idamUserId, UUID partyId);

    Optional<DraftCaseDataEntity> findByCaseReferenceAndEventIdAndIdamUserIdAndPartyIdIsNull(
        long caseReference, EventId eventId, UUID idamUserId);

    boolean existsByCaseReferenceAndEventIdAndIdamUserIdAndPartyIdIsNull(
        long caseReference, EventId eventId, UUID idamUserId);

    void deleteByCaseReferenceAndEventIdAndIdamUserIdAndPartyIdIsNull(
        long caseReference, EventId eventId, UUID idamUserId);

    Optional<DraftCaseDataEntity> findByCaseReferenceAndEventIdAndOrganisationIdAndPartyId(
        long caseReference, EventId eventId, String legalRepresentativeOrganisationId, UUID partId);

}
