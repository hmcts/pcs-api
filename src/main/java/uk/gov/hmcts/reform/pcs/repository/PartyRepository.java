package uk.gov.hmcts.reform.pcs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.pcs.entity.Party;

import java.util.List;
import java.util.UUID;

@Repository
public interface PartyRepository extends JpaRepository<Party, UUID> {

    @Query("""
        select new uk.gov.hmcts.reform.pcs.ccd.domain.Party(p.id, p.forename, p.surname, p.active)
        from Party p
        where p.pcsCase.caseReference = :caseReference
        """)
    List<uk.gov.hmcts.reform.pcs.ccd.domain.Party> findAllDtoByCaseReference(
        @Param("caseReference") long caseReference);

    @Query("""
        select new uk.gov.hmcts.reform.pcs.ccd.domain.Party(p.id, p.forename, p.surname, p.active)
        from Party p
        where p.pcsCase.caseReference = :caseReference and p.active = :active
        """)
    List<uk.gov.hmcts.reform.pcs.ccd.domain.Party> findAllDtoByCaseReference(@Param("caseReference") long caseReference,
                                                                             @Param("active") boolean active);

    @Modifying
    @Query("""
        update Party p
        set p.active = :active
        where p.id in :ids
        """)
    void setPartiesActive(@Param("ids") List<UUID> ids, @Param("active") boolean active);

}
