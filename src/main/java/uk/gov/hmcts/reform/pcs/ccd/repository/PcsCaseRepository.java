package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PcsCaseRepository extends JpaRepository<PcsCaseEntity, UUID> {

    @EntityGraph(attributePaths = {"propertyAddress", "parties", "parties.address"})
    Optional<PcsCaseEntity> findByCaseReference(long caseReference);

    /**
     * The organisations owning a case through its claimant. The "no claim link" arm covers the draft
     * phase, where the claimant has an organisation but no claim yet. Distinct, so two claimant
     * parties from one organisation give one owner rather than an ambiguity; callers assert the
     * single result rather than silently picking one, which leaves only genuinely different
     * organisations as an error.
     */
    @Query("""
        select distinct party.organisationId from PcsCaseEntity pcsCase
        join pcsCase.parties party
        where pcsCase.caseReference = :caseReference
          and party.organisationId is not null
          and (party.claimParties is empty
               or exists (select claimParty from ClaimPartyEntity claimParty
                          where claimParty.party = party and claimParty.role = :claimantRole))
        """)
    List<String> findOwningOrganisationIds(@Param("caseReference") long caseReference,
                                           @Param("claimantRole") PartyRole claimantRole);

}
