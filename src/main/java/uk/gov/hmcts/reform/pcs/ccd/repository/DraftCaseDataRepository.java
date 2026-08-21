package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.ccd.entity.DraftCaseDataEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DraftCaseDataRepository extends JpaRepository<DraftCaseDataEntity, Integer> {

    List<DraftCaseDataEntity> findByCaseReferenceAndEventId(
            long caseReference, EventId eventId);

    Optional<DraftCaseDataEntity> findByCaseReferenceAndEventIdAndIdamUserId(
        long caseReference, EventId eventId, UUID idamUserId);

    boolean existsByCaseReferenceAndEventIdAndIdamUserId(
        long caseReference, EventId eventId, UUID idamUserId);

    void deleteByCaseReferenceAndEventIdAndIdamUserId(
        long caseReference, EventId eventId, UUID idamUserId);

    void deleteByCaseReferenceAndEventIdAndOrganisationIdAndPartyId(
        long caseReference, EventId eventId, String legalRepresentativeOrganisationId, UUID partyId);

    boolean existsByCaseReferenceAndEventIdAndOrganisationIdAndPartyId(
        long caseReference, EventId eventId, String legalRepresentativeOrganisationId, UUID partId);

    Optional<DraftCaseDataEntity> findByCaseReferenceAndEventIdAndOrganisationIdAndPartyId(
        long caseReference, EventId eventId, String legalRepresentativeOrganisationId, UUID partId);

}
