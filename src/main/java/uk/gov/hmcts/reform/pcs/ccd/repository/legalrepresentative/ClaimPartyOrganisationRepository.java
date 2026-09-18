package uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative;

import feign.Param;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.ClaimPartyOrganisationEntity;

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
}
