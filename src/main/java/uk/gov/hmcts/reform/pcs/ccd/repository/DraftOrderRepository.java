package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.DraftOrderState;
import uk.gov.hmcts.reform.pcs.ccd.entity.DraftOrderEntity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DraftOrderRepository extends JpaRepository<DraftOrderEntity, UUID> {

    /**
     * A judge has at most one draft per case, and one per application on it, which the database
     * enforces. A null application finds the draft for the case.
     */
    Optional<DraftOrderEntity> findByPcsCaseCaseReferenceAndAuthorIdamUserIdAndStateAndGenAppId(
        long caseReference, UUID authorIdamUserId, DraftOrderState state, UUID genAppId);

    /** A judge's own order, while it is in one of the states that leave it theirs to change. */
    Optional<DraftOrderEntity> findByIdAndPcsCaseCaseReferenceAndAuthorIdamUserIdAndStateIn(
        UUID id, long caseReference, UUID authorIdamUserId, Collection<DraftOrderState> states);

    /** Every order on a case, whoever wrote it. */
    List<DraftOrderEntity> findByPcsCaseId(UUID caseId);

    Optional<DraftOrderEntity> findByIdAndPcsCaseCaseReferenceAndState(
        UUID id, long caseReference, DraftOrderState state);
}
