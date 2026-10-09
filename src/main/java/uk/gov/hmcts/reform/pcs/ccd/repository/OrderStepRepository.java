package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import uk.gov.hmcts.reform.pcs.ccd.entity.OrderStepEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderStepRepository extends JpaRepository<OrderStepEntity, Long> {

    Optional<OrderStepEntity> findFirstByOrderIdOrderByIdDesc(UUID orderId);

    /** The step that last returned the order to its judge, with the query it was returned with. */
    Optional<OrderStepEntity> findFirstByOrderIdAndNoteIsNotNullOrderByIdDesc(UUID orderId);

    /** Each order on a case as it stands. */
    @Query("""
        select step from OrderStepEntity step join fetch step.order
        where step.order.pcsCase.id = :caseId
        and step.id = (select max(latest.id) from OrderStepEntity latest where latest.order = step.order)""")
    List<OrderStepEntity> findLatestByCaseId(UUID caseId);
}
