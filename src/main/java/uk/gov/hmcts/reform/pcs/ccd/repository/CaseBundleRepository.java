package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseBundleEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CaseBundleRepository extends JpaRepository<CaseBundleEntity, UUID> {

    Optional<CaseBundleEntity> findByPcsCase_IdAndLatestTrue(UUID caseId);

    Optional<CaseBundleEntity> findByJobId(UUID jobId);

    List<CaseBundleEntity> findByPcsCase_IdOrderByDocumentsSelectedAtDesc(UUID caseId);

    @Query("select b.document.id from CaseBundleEntity b where b.pcsCase.id = :caseId and b.latest = true")
    Optional<UUID> findLatestDocumentId(UUID caseId);

}
