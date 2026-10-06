package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.ccd.entity.IssuedOrderEntity;

import java.util.UUID;

public interface IssuedOrderRepository extends JpaRepository<IssuedOrderEntity, UUID> {
}
