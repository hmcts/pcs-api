package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.ccd.entity.JudicialNoteEntity;

public interface JudicialNoteRepository extends JpaRepository<JudicialNoteEntity, Integer> {
}
