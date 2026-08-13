package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.ccd.entity.DraftCaseDataEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;

import java.util.Optional;
import java.util.UUID;

public interface DraftCaseDataRepository extends JpaRepository<DraftCaseDataEntity, Integer> {

    /*
     * The user-keyed lookups must exclude both other shapes. Without AndPartyIdIsNull a user holding
     * a plain and a party draft on one case and event matches two rows and Spring Data throws on the
     * Optional; without AndOrganisationIdIsNull the same happens once a journey moves to organisation
     * ownership, because owned rows still carry idamUserId as the record of who last wrote them.
     */

    Optional<DraftCaseDataEntity> findByCaseReferenceAndEventIdAndIdamUserIdAndPartyIdIsNullAndOrganisationIdIsNull(
        long caseReference, EventId eventId, UUID idamUserId);

    boolean existsByCaseReferenceAndEventIdAndIdamUserIdAndPartyIdIsNullAndOrganisationIdIsNull(
        long caseReference, EventId eventId, UUID idamUserId);

    void deleteByCaseReferenceAndEventIdAndIdamUserIdAndPartyIdIsNullAndOrganisationIdIsNull(
        long caseReference, EventId eventId, UUID idamUserId);

    Optional<DraftCaseDataEntity> findByCaseReferenceAndEventIdAndOrganisationId(
        long caseReference, EventId eventId, String organisationId);

    boolean existsByCaseReferenceAndEventIdAndOrganisationId(
        long caseReference, EventId eventId, String organisationId);

    void deleteByCaseReferenceAndEventIdAndOrganisationId(
        long caseReference, EventId eventId, String organisationId);

    void deleteByCaseReferenceAndEventIdAndIdamUserIdAndPartyId(
        long caseReference, EventId eventId, UUID idamUserId, UUID partyId);

    Optional<DraftCaseDataEntity> findByCaseReferenceAndEventIdAndIdamUserIdAndPartyId(
        long caseReference, EventId eventId, UUID idamUserId, UUID partyId);

    boolean existsByCaseReferenceAndEventIdAndIdamUserIdAndPartyId(
        long caseReference, EventId eventId, UUID idamUserId, UUID partyId);

}
