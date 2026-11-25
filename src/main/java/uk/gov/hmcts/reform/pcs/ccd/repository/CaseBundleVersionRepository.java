package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.ccd.entity.bundle.CaseBundleVersionEntity;

import java.util.List;
import java.util.UUID;

public interface CaseBundleVersionRepository extends JpaRepository<CaseBundleVersionEntity, UUID> {

    /** A case's bundle versions, latest first. */
    List<CaseBundleVersionEntity> findByCaseIdOrderByRevisionDescCreatedAtDesc(UUID caseId);

    boolean existsByDocumentId(UUID documentId);
}
