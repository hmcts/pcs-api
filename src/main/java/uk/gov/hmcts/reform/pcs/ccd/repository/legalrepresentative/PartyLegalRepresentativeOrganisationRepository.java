package uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.LegalRepresentativeOrganisationPartyId;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.PartyLegalRepresentativeOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;

import java.util.List;
import java.util.UUID;

@Repository
public interface PartyLegalRepresentativeOrganisationRepository
    extends JpaRepository<PartyLegalRepresentativeOrganisationEntity, LegalRepresentativeOrganisationPartyId> {

    @Query("""
        SELECT plro
        FROM PartyLegalRepresentativeOrganisationEntity plro
        JOIN plro.party p
        JOIN plro.legalRepresentativeOrganisation lro
        JOIN p.pcsCase pcsCase
        WHERE p.id = :partyId
        AND lro.id = :legalRepresentativeOrganisationId
        AND pcsCase.caseReference = :caseReference
        AND plro.active = 'YES'
        """)
    List<PartyLegalRepresentativeOrganisationEntity> findAllActiveByPartyIdLegalRepresentativeOrganisationIdAndCase(
        @Param("partyId") UUID partyId,
        @Param("legalRepresentativeOrganisationId") UUID legalRepresentativeOrganisationId,
        @Param("caseReference") long caseReference
    );

    @Query("""
        SELECT COUNT(plro)
        FROM PartyLegalRepresentativeOrganisationEntity plro
        JOIN plro.party p
        JOIN p.claimParties cp
        JOIN cp.claim c
        JOIN c.pcsCase pcsCase
        WHERE pcsCase.caseReference = :caseReference
          AND plro.legalRepresentativeOrganisation.id = :lroId
          AND p.id <> :excludedPartyId
          AND cp.role = :role
          AND plro.active = 'YES'
        """)
    long countOtherDefendantsRepresentedByOrganisation(
        @Param("lroId") UUID legalRepresentativeOrganisationId,
        @Param("caseReference") long caseReference,
        @Param("excludedPartyId") UUID excludedPartyId,
        @Param("role") PartyRole role
    );
}



