package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.DraftOrderState;
import uk.gov.hmcts.reform.pcs.ccd.entity.DraftOrderEntity;

import java.util.Optional;
import java.util.UUID;

public interface DraftOrderRepository extends JpaRepository<DraftOrderEntity, UUID> {

    /** A judge has at most one draft per case, which the database enforces. */
    Optional<DraftOrderEntity> findByPcsCaseCaseReferenceAndAuthorIdamUserIdAndState(
        long caseReference, UUID authorIdamUserId, DraftOrderState state);

    Optional<DraftOrderEntity> findByIdAndPcsCaseCaseReferenceAndAuthorIdamUserIdAndState(
        UUID id, long caseReference, UUID authorIdamUserId, DraftOrderState state);
}
