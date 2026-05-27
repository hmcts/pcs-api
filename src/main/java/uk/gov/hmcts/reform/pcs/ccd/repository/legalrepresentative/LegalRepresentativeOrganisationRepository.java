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
        SELECT COUNT(lr) > 0
        FROM LegalRepresentativeEntity lr
        JOIN lr.claimPartyLegalRepresentativeList cplr
        JOIN cplr.party p
        WHERE lr.idamId = :idamId
        AND p.id = :partyId
        AND cplr.active = 'YES'
        """)
    boolean isLegalRepresentativeOrganisationLinkedToPartyAndActive(@Param("idamId") UUID idamId,
                                                                    @Param("partyId") UUID partyId);

    @Query("""
        SELECT lr
        FROM LegalRepresentativeEntity lr
        JOIN lr.claimPartyLegalRepresentativeList cplr
        JOIN cplr.party p
        WHERE p.id = :partyId
        AND cplr.active = 'YES'
        """)
    Optional<LegalRepresentativeOrganisationEntity> findLegalRepresentativeOrganisationForParty(
        @Param("partyId") UUID partyId);


    Optional<LegalRepresentativeOrganisationEntity> findByIdamId(UUID idamUserId);

}
