package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.ccd.entity.DraftCaseDataEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;

import java.util.Optional;
import java.util.UUID;

public interface DraftCaseDataRepository extends JpaRepository<DraftCaseDataEntity, Integer> {

    Optional<DraftCaseDataEntity> findByCaseReferenceAndEventIdAndIdamUserId(
        long caseReference, EventId eventId, UUID idamUserId);

    boolean existsByCaseReferenceAndEventIdAndIdamUserId(
        long caseReference, EventId eventId, UUID idamUserId);

    void deleteByCaseReferenceAndEventIdAndIdamUserId(
        long caseReference, EventId eventId, UUID idamUserId);

    // Claim drafts are keyed on the organisation alone and carry no party. The PartyIdIsNull is
    // load-bearing: the legal representative journey writes drafts for the same case, event and
    // organisation but with a party, so without it a firm's defendant draft is returned here - and
    // a firm representing two defendants makes the lookup non-unique and throws.
    Optional<DraftCaseDataEntity> findByCaseReferenceAndEventIdAndOrganisationIdAndPartyIdIsNull(
        long caseReference, EventId eventId, String organisationId);

    boolean existsByCaseReferenceAndEventIdAndOrganisationIdAndPartyIdIsNull(
        long caseReference, EventId eventId, String organisationId);

    void deleteByCaseReferenceAndEventIdAndOrganisationIdAndPartyIdIsNull(
        long caseReference, EventId eventId, String organisationId);

    void deleteByCaseReferenceAndEventIdAndIdamUserIdAndPartyId(
        long caseReference, EventId eventId, UUID idamUserId, UUID partyId);

    void deleteByCaseReferenceAndEventIdAndOrganisationIdAndPartyId(
        long caseReference, EventId eventId, String legalRepresentativeOrganisationId, UUID partyId);

    boolean existsByCaseReferenceAndEventIdAndOrganisationIdAndPartyId(
        long caseReference, EventId eventId, String legalRepresentativeOrganisationId, UUID partId);

    boolean existsByCaseReferenceAndEventIdAndIdamUserIdAndPartyId(
        long caseReference, EventId eventId, UUID idamUserId, UUID partyId);

    Optional<DraftCaseDataEntity> findByCaseReferenceAndEventIdAndIdamUserIdAndPartyIdIsNull(
        long caseReference, EventId eventId, UUID idamUserId);

    Optional<DraftCaseDataEntity> findByCaseReferenceAndEventIdAndOrganisationIdAndPartyId(
        long caseReference, EventId eventId, String legalRepresentativeOrganisationId, UUID partId);

}
