package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<DocumentEntity, UUID> {

    Optional<DocumentEntity> findByDocumentId(UUID documentId);

    boolean existsByCounterClaim_IdAndType(UUID counterClaimId, DocumentType type);

    boolean existsByHearing_IdAndType(Integer hearingId, DocumentType type);

    /** The case's documents as stored now, whatever a loaded case's document list says. */
    List<DocumentEntity> findByPcsCase_Id(UUID caseId);

}
