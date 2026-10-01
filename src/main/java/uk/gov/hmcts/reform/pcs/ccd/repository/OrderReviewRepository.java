package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.ccd.entity.OrderReviewEntity;

import java.util.Optional;
import java.util.UUID;

public interface OrderReviewRepository extends JpaRepository<OrderReviewEntity, UUID> {

    /** The latest review of an order: the one of the newest version of it that was reviewed. */
    Optional<OrderReviewEntity> findFirstByDraftOrderIdOrderByReviewedVersionDesc(UUID draftOrderId);
}
