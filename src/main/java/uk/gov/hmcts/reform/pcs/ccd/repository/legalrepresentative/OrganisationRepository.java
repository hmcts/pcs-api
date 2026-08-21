package uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.OrganisationEntity;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrganisationRepository extends JpaRepository<OrganisationEntity, UUID> {

    @Query("""
        SELECT o
        FROM OrganisationEntity o
        JOIN o.claimPartyOrganisationList cpo
        JOIN cpo.party p
        WHERE p.id = :partyId
        AND cpo.active = 'YES'
        """)
    Optional<OrganisationEntity> findByPartyLinkedToOrganisationAndActive(
        @Param("partyId") UUID partyId);

    @Query("""
        SELECT COUNT(o) > 0
        FROM OrganisationEntity o
        JOIN o.claimPartyOrganisationList cpo
        JOIN cpo.party p
        WHERE o.organisationId = :organisationId
        AND p.id = :partyId
        AND cpo.active = 'YES'
        """)
    boolean isOrganisationLinkedToPartyAndActive(@Param("organisationId") String organisationId,
                                                 @Param("partyId") UUID partyId);

    @Query("""
        SELECT o
        FROM OrganisationEntity o
        JOIN o.claimPartyContactDetails contact
        JOIN contact.pcsCase pcsCase
        WHERE pcsCase.caseReference = :caseReference
        AND o.organisationId = :organisationId
        """)
    Optional<OrganisationEntity> findByOrganisationIdAndCaseReference(
        @Param("organisationId") String organisationId,
        @Param("caseReference") long caseReference
    );

    Optional<OrganisationEntity> findByOrganisationId(
        @Param("organisationId") String organisationId);


}
