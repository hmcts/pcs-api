package uk.gov.hmcts.reform.pcs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.pcs.entity.GeneralApplicationEntity;

import java.util.List;
import java.util.UUID;

@Repository
public interface GeneralApplicationRepository extends JpaRepository<GeneralApplicationEntity, UUID> {

    @Query("""
            SELECT g FROM GeneralApplicationEntity g
            WHERE g.pcsCase.caseReference = :caseReference
        """)
    List<GeneralApplicationEntity> findByCaseReference(long caseReference);

}
