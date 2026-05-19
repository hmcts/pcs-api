package uk.gov.hmcts.reform.pcs.noc.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.noc.entity.PartyRepresentationEntity;
import uk.gov.hmcts.reform.pcs.noc.entity.PartyRepresentationStatus;

public interface PartyRepresentationRepository extends JpaRepository<PartyRepresentationEntity, UUID> {

    Optional<PartyRepresentationEntity> findByCaseReferenceAndPartyIdAndStatus(
        long caseReference,
        UUID partyId,
        PartyRepresentationStatus status
    );

    boolean existsByCaseReferenceAndPartyIdAndOrganisationIdAndStatus(
        long caseReference,
        UUID partyId,
        String organisationId,
        PartyRepresentationStatus status
    );

    List<PartyRepresentationEntity> findByCaseReferenceAndOrganisationIdAndCaseRoleAndStatus(
        long caseReference,
        String organisationId,
        String caseRole,
        PartyRepresentationStatus status
    );
}
