package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.ccd.entity.DraftCaseDataEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;

import java.util.Optional;
import java.util.UUID;

public interface DraftCaseDataRepository extends JpaRepository<DraftCaseDataEntity, UUID> {

    Optional<DraftCaseDataEntity> findByCaseReferenceAndEventIdAndIdamUserId(
        long caseReference, EventId eventId, UUID idamUserId);

    boolean existsByCaseReferenceAndEventIdAndIdamUserId(
        long caseReference, EventId eventId, UUID idamUserId);

    void deleteByCaseReferenceAndEventIdAndIdamUserId(
        long caseReference, EventId eventId, UUID idamUserId);

    void deleteByCaseReferenceAndEventIdAndIdamUserIdAndPartyId(
        long caseReference, EventId eventId, UUID idamUserId, UUID partyId);

    void deleteByCaseReferenceAndEventIdAndLegalRepresentativeOrganisationIdAndPartyId(
        long caseReference, EventId eventId, String legalRepresentativeOrganisationId, UUID partyId);

    Optional<DraftCaseDataEntity> findByCaseReferenceAndEventIdAndIdamUserIdAndPartyId(
        long caseReference, EventId eventId, UUID idamUserId, UUID partyId);

    boolean existsByCaseReferenceAndEventIdAndLegalRepresentativeOrganisationIdAndPartyId(
        long caseReference, EventId eventId, String legalRepresentativeOrganisationId, UUID partId);

    Optional<DraftCaseDataEntity> findByCaseReferenceAndEventIdAndLegalRepresentativeOrganisationIdAndPartyId(
        long caseReference, EventId eventId, String legalRepresentativeOrganisationId, UUID partId);

}
