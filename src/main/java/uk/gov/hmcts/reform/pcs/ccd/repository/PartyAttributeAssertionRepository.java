package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PartyAttributeAssertionStatus;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.PartyAttributeAssertationEntity;

import java.util.List;
import java.util.UUID;

@Repository
public interface PartyAttributeAssertionRepository extends JpaRepository<PartyAttributeAssertationEntity, UUID> {

    List<PartyAttributeAssertationEntity> findByPartyIdAndStatusOrderByCreatedAtAsc(
        UUID partyId,
        PartyAttributeAssertionStatus status
    );
}