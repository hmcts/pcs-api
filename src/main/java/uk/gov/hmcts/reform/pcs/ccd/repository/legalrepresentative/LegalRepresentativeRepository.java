package uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.LegalRepresentativeEntity;

import java.util.UUID;

@Repository
public interface LegalRepresentativeRepository extends JpaRepository<LegalRepresentativeEntity, UUID> {

    @Query("""
        SELECT COUNT(lr) > 0
        FROM LegalRepresentativeEntity lr
        JOIN lr.claimPartyLegalRepresentativeList cplr
        JOIN cplr.party p
        WHERE lr.idamId = :idamId
        AND p.id = :id
        AND cplr.active = 'YES'
        """)
    boolean isLegalRepresentativeLinkedToPartyAndActive(@Param("idamId") UUID idamId, @Param("id") UUID id);

}
