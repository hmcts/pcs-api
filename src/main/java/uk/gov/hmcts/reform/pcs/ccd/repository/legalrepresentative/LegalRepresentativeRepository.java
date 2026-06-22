package uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.ClaimPartyLegalRepresentativeEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.LegalRepresentativeEntity;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface LegalRepresentativeRepository extends JpaRepository<LegalRepresentativeEntity, UUID> {

    @Query("""
        SELECT COUNT(lr) > 0
        FROM LegalRepresentativeEntity lr
        JOIN lr.claimPartyLegalRepresentativeList cplr
        JOIN cplr.party p
        WHERE lr.idamId = :idamId
        AND p.id = :partyId
        AND cplr.active = 'YES'
        """)
    boolean isLegalRepresentativeLinkedToPartyAndActive(@Param("idamId") UUID idamId, @Param("partyId") UUID partyId);

    @Query("""
        SELECT lr
        FROM LegalRepresentativeEntity lr
        JOIN lr.claimPartyLegalRepresentativeList cplr
        JOIN cplr.party p
        WHERE p.id = :partyId
        AND cplr.active = 'YES'
        """)
    Optional<LegalRepresentativeEntity> findByPartyLinkedToLegalRepresentativeAndActive(@Param("partyId") UUID partyId);

    @Query("""
        SELECT cplr
        FROM ClaimPartyLegalRepresentativeEntity cplr
        JOIN FETCH cplr.legalRepresentative lr
        LEFT JOIN FETCH lr.address
        WHERE cplr.id.partyId IN :partyIds
        AND cplr.active = 'YES'
        """)
    List<ClaimPartyLegalRepresentativeEntity> findActiveByPartyIds(@Param("partyIds") Set<UUID> partyIds);

    @Query("""
        SELECT COUNT(lr) > 0
        FROM LegalRepresentativeEntity lr
        JOIN lr.claimPartyLegalRepresentativeList cplr
        JOIN cplr.party p
        WHERE lr.organisationId = :organisationId
        AND p.id = :partyId
        AND cplr.active = 'YES'
        """)
    boolean isRepresentativeOrganisationLinkedToPartyAndActive(@Param("organisationId") String organisationId,
                                                               @Param("partyId") UUID partyId);

    @Query("""
        SELECT lr
        FROM LegalRepresentativeEntity lr
        JOIN lr.claimPartyLegalRepresentativeList cplr
        JOIN cplr.party p
        JOIN p.pcsCase pcsCase
        WHERE pcsCase.caseReference = :caseReference
        AND cplr.active = 'YES'
        AND lr.idamId = :idamId
        """)
    Optional<LegalRepresentativeEntity> findByIdamId(@Param("idamId") UUID idamId,
                                                     @Param("caseReference") long caseReference);

    @Query("""
        SELECT lr
        FROM LegalRepresentativeEntity lr
        JOIN lr.claimPartyLegalRepresentativeList cplr
        JOIN cplr.party p
        JOIN p.pcsCase pcsCase
        WHERE pcsCase.caseReference = :caseReference
        AND cplr.active = 'YES'
        AND lr.organisationId = :organisationId
        """)
    Optional<LegalRepresentativeEntity> findByOrganisationId(@Param("organisationId") String organisationId,
                                                             @Param("caseReference") long caseReference);


}
