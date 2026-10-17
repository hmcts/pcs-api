package uk.gov.hmcts.reform.pcs.enforcement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.enforcement.entity.BailiffNoteEntity;

import java.util.List;
import java.util.UUID;

public interface BailiffNoteRepository extends JpaRepository<BailiffNoteEntity, UUID> {

    /**
     * The enforcement's notes, newest first.
     */
    List<BailiffNoteEntity> findByEnforcementOrderIdOrderByCreatedAtDesc(UUID enforcementOrderId);
}
