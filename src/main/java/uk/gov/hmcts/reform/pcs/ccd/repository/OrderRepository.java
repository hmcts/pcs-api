package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderState;
import uk.gov.hmcts.reform.pcs.ccd.entity.OrderEntity;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {

    Optional<OrderEntity> findByPcsCaseCaseReferenceAndAuthorIdamUserIdAndState(
        long caseReference, UUID authorIdamUserId, OrderState state);

    Optional<OrderEntity> findByIdAndPcsCaseCaseReferenceAndAuthorIdamUserIdAndStateIn(
        UUID id, long caseReference, UUID authorIdamUserId, Collection<OrderState> states);

    Optional<OrderEntity> findByIdAndPcsCaseCaseReferenceAndState(UUID id, long caseReference, OrderState state);
}
