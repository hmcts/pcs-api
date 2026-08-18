package uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.ClaimPartyOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;

import java.util.List;
import java.util.UUID;

@Repository
public interface ClaimPartyOrganisationRepository
    extends JpaRepository<ClaimPartyOrganisationEntity, Integer> {

    @Query("""
        SELECT cpo
        FROM ClaimPartyOrganisationEntity cpo
        JOIN cpo.party p
        JOIN p.pcsCase pcsCase
        WHERE p.id = :partyId
        AND cpo.organisation.id = :organisationId
        AND pcsCase.caseReference = :caseReference
        AND cpo.active = 'YES'
        """)
    List<ClaimPartyOrganisationEntity> findAllActiveByPartyIdLegalRepresentativeOrganisationIdAndCase(
        @Param("partyId") UUID partyId,
        @Param("organisationId") Integer organisationId,
        @Param("caseReference") long caseReference
    );

    @Query("""
        SELECT COUNT(cpo)
        FROM ClaimPartyOrganisationEntity cpo
        JOIN cpo.party p
        JOIN p.claimParties cp
        JOIN cp.claim c
        JOIN c.pcsCase pcsCase
        WHERE pcsCase.caseReference = :caseReference
          AND cpo.organisation.id = :organisationId
          AND p.id <> :excludedPartyId
          AND cp.role = :role
          AND cpo.active = 'YES'
        """)
    long countOtherDefendantsRepresentedByOrganisation(
        @Param("organisationId") Integer organisationId,
        @Param("caseReference") long caseReference,
        @Param("excludedPartyId") UUID excludedPartyId,
        @Param("role") PartyRole role
    );
}



