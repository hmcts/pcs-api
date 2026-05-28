package uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.LegalRepresentativeOrganisationEntity;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LegalRepresentativeOrganisationRepository extends JpaRepository<LegalRepresentativeOrganisationEntity,
    UUID> {

    @Query("""
        SELECT lro
        FROM LegalRepresentativeOrganisationEntity lro
        JOIN lro.partyLegalRepresentativeOrganisationList plro
        JOIN plro.party p
        WHERE p.id = :partyId
        AND plro.active = 'YES'
        """)
    Optional<LegalRepresentativeOrganisationEntity> findByPartyLinkedToLegalRepresentativeOrganisationAndActive(@Param("partyId") UUID partyId);

    @Query("""
        SELECT COUNT(lro) > 0
        FROM LegalRepresentativeOrganisationEntity lro
        JOIN lro.partyLegalRepresentativeOrganisationList plro
        JOIN plro.party p
        WHERE lro.organisationId = :organisationId
        AND p.id = :partyId
        AND plro.active = 'YES'
        """)
    boolean isRepresentativeOrganisationLinkedToPartyAndActive(@Param("organisationId") String organisationId,
                                                               @Param("partyId") UUID partyId);

    @Query("""
        SELECT lro
        FROM LegalRepresentativeOrganisationEntity lro
        JOIN lro.pcsCase pcsCase
        WHERE pcsCase.caseReference = :caseReference
        AND lro.organisationId = :organisationId
        """)
    Optional<LegalRepresentativeOrganisationEntity> findByOrganisationIdAndCaseReference(@Param("organisationId") String organisationId,
                                                             @Param("caseReference") long caseReference);


}
