package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uk.gov.hmcts.reform.pcs.ccd.entity.DraftCaseDataEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DraftCaseDataRepository extends JpaRepository<DraftCaseDataEntity, Integer> {

    /** The stored JSON of every draft on the case that the user, or their organisation, owns. */
    @Query("select d.caseData from DraftCaseDataEntity d where d.caseReference = :caseReference "
        + "and (d.idamUserId = :userId or d.organisationId = :organisationId)")
    List<String> findCaseDataOwnedBy(@Param("caseReference") long caseReference,
                                     @Param("userId") String userId,
                                     @Param("organisationId") String organisationId);

    // PartyIdIsNull is load-bearing: LR drafts share case/event/org but carry a party;
    // without it a firm's defendant draft matches here (and two defendants -> non-unique).
    Optional<DraftCaseDataEntity> findByCaseReferenceAndEventIdAndOrganisationIdAndPartyIdIsNull(
        long caseReference, EventId eventId, String organisationId);

    boolean existsByCaseReferenceAndEventIdAndOrganisationIdAndPartyIdIsNull(
        long caseReference, EventId eventId, String organisationId);

    void deleteByCaseReferenceAndEventIdAndOrganisationIdAndPartyIdIsNull(
        long caseReference, EventId eventId, String organisationId);

    void deleteByCaseReferenceAndEventIdAndIdamUserId(
        long caseReference, EventId eventId, String idamUserId);

    void deleteByCaseReferenceAndEventIdAndOrganisationIdAndPartyId(
        long caseReference, EventId eventId, String legalRepresentativeOrganisationId, UUID partyId);

    boolean existsByCaseReferenceAndEventIdAndOrganisationIdAndPartyId(
        long caseReference, EventId eventId, String legalRepresentativeOrganisationId, UUID partId);

    Optional<DraftCaseDataEntity> findByCaseReferenceAndEventIdAndIdamUserIdAndPartyIdIsNull(
        long caseReference, EventId eventId, String idamUserId);

    boolean existsByCaseReferenceAndEventIdAndIdamUserIdAndPartyIdIsNull(
        long caseReference, EventId eventId, String idamUserId);

    void deleteByCaseReferenceAndEventIdAndIdamUserIdAndPartyIdIsNull(
        long caseReference, EventId eventId, String idamUserId);

    Optional<DraftCaseDataEntity> findByCaseReferenceAndEventIdAndOrganisationIdAndPartyId(
        long caseReference, EventId eventId, String legalRepresentativeOrganisationId, UUID partId);

    @Modifying
    @Query("""
        DELETE FROM DraftCaseDataEntity d
        WHERE d.eventId = :eventId
          AND d.createdAt < :cutoff
        """)
    int deleteByEventIdAndCutoff(@Param("eventId") EventId eventId, @Param("cutoff") Instant cutoff);

}
