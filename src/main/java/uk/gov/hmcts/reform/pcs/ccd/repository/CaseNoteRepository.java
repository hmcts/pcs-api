package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseNoteEntity;

public interface CaseNoteRepository extends JpaRepository<CaseNoteEntity, Integer> {
}
