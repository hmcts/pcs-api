package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.ccd.entity.bundle.CaseBundleEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CaseBundleRepository extends JpaRepository<CaseBundleEntity, UUID> {

    /** A case's bundles, latest first. */
    List<CaseBundleEntity> findByCaseIdOrderByCaseRevisionDesc(UUID caseId);

    /** A case's latest bundle. */
    Optional<CaseBundleEntity> findFirstByCaseIdOrderByCaseRevisionDesc(UUID caseId);
}
