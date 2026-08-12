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
     * The parties on a case that an organisation owns, which is what a draft on that case belongs to.
     *
     * <p>The "no claim link" arm is not redundant: {@code PartyService.initialiseClaimant} creates the
     * claimant with an organisation and no claim link, and that is the whole draft phase - matching on
     * {@code ClaimPartyEntity.role} alone would find nothing until submit, which is exactly when a
     * shared draft is needed.</p>
     *
     * <p>Returns a list rather than an Optional because that arm is role-blind: a party in the draft
     * phase has no role to check, so it matches any organisation-bearing party without a claim link.
     * That yields one row only while the claimant is the sole party ever given an organisation.
     * Notice of change removes that assumption, so callers assert the single result rather than
     * letting a second party silently pick a winner.</p>
     */
    @Query("""
        select party.id from PcsCaseEntity pcsCase
        join pcsCase.parties party
        where pcsCase.caseReference = :caseReference
          and party.organisationId is not null
          and (party.claimParties is empty
               or exists (select claimParty from ClaimPartyEntity claimParty
                          where claimParty.party = party and claimParty.role = :claimantRole))
        """)
    List<UUID> findOrganisationOwnedPartyIds(@Param("caseReference") long caseReference,
                                             @Param("claimantRole") PartyRole claimantRole);

}
