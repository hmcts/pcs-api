package uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.ClaimPartyOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;

import java.util.UUID;

@Repository
public interface ClaimPartyOrganisationRepository
    extends JpaRepository<ClaimPartyOrganisationEntity, Integer> {

    @Query("""
        SELECT plro
        FROM ClaimPartyOrganisationEntity plro
        JOIN plro.party p
        JOIN p.pcsCase pcsCase
        WHERE p.id = :partyId
        AND plro.organisation.id = :legalRepresentativeOrganisationId
        AND pcsCase.caseReference = :caseReference
        AND plro.active = 'YES'
        """)
    List<ClaimPartyOrganisationEntity> findAllActiveByPartyIdLegalRepresentativeOrganisationIdAndCase(
        @Param("partyId") UUID partyId,
        @Param("legalRepresentativeOrganisationId") Integer legalRepresentativeOrganisationId,
        @Param("caseReference") long caseReference
    );

    @Query("""
        SELECT DISTINCT plro.party
        FROM ClaimPartyOrganisationEntity plro
        JOIN plro.party p
        JOIN p.claimParties claimParty
        WHERE p.pcsCase.caseReference = :caseReference
        AND claimParty.role = :role
        AND plro.organisation.organisationId = :organisationId
        AND plro.active = 'YES'
        """)
    List<PartyEntity> findActiveDefendantsRepresentedByOrganisation(
        @Param("caseReference") long caseReference,
        @Param("organisationId") String organisationId,
        @Param("role") PartyRole role
    );
}
