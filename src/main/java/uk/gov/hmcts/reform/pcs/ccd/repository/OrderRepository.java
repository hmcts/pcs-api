package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderState;
import uk.gov.hmcts.reform.pcs.ccd.entity.OrderEntity;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {

    /** A judge's draft for the case, or for one of its applications; a null application finds the case's. */
    Optional<OrderEntity> findByPcsCaseCaseReferenceAndAuthorIdamUserIdAndStateAndGenAppId(
        long caseReference, String authorIdamUserId, OrderState state, UUID genAppId);

    Optional<OrderEntity> findByIdAndPcsCaseCaseReferenceAndAuthorIdamUserIdAndStateIn(
        UUID id, long caseReference, String authorIdamUserId, Collection<OrderState> states);

    Optional<OrderEntity> findByIdAndPcsCaseCaseReferenceAndState(UUID id, long caseReference, OrderState state);

    boolean existsByIdAndDocumentIsNotNull(UUID id);
}
