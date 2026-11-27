package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.ccd.entity.SubmittedDraftOrderEntity;

import java.util.List;
import java.util.UUID;

public interface SubmittedDraftOrderRepository extends JpaRepository<SubmittedDraftOrderEntity, UUID> {

    /** The case's filings, newest first. */
    List<SubmittedDraftOrderEntity> findByPartyPcsCaseIdOrderBySubmittedAtDesc(UUID caseId);
}
