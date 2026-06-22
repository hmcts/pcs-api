package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<DocumentEntity, UUID> {

    Optional<DocumentEntity> findByDocumentId(UUID documentId);

    @Query("""
        SELECT document
        FROM DocumentEntity document
        JOIN FETCH document.generalApplication generalApplication
        WHERE generalApplication.id IN :genAppIds
        """)
    List<DocumentEntity> findAllByGeneralApplicationIds(@Param("genAppIds") Set<UUID> genAppIds);

}
