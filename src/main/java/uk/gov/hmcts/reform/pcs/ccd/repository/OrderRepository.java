package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderState;
import uk.gov.hmcts.reform.pcs.ccd.entity.OrderEntity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {

    /** A judge's draft for one of the case's hearings, or with a null hearing, their draft made without one. */
    Optional<OrderEntity> findByPcsCaseCaseReferenceAndAuthorIdamUserIdAndStateAndHearingId(
        long caseReference, String authorIdamUserId, OrderState state, Integer hearingId);

    Optional<OrderEntity> findByIdAndPcsCaseCaseReferenceAndAuthorIdamUserIdAndStateIn(
        UUID id, long caseReference, String authorIdamUserId, Collection<OrderState> states);

    Optional<OrderEntity> findByIdAndPcsCaseCaseReferenceAndState(UUID id, long caseReference, OrderState state);

    boolean existsByIdAndDocumentIsNotNull(UUID id);

    List<OrderEntity> findByPcsCaseIdAndStateIn(UUID caseId, Collection<OrderState> states);
}
