package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.ccd.entity.OrderReviewEntity;

import java.util.UUID;

public interface OrderReviewRepository extends JpaRepository<OrderReviewEntity, UUID> {
}
