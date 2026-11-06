package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import uk.gov.hmcts.reform.pcs.ccd.entity.OrderStepEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderStepRepository extends JpaRepository<OrderStepEntity, Long> {

    Optional<OrderStepEntity> findFirstByOrderIdOrderByIdDesc(UUID orderId);

    /** Queries in either their original case-note form or their message form, latest first. */
    @Query("""
        select step from OrderStepEntity step
        where step.order.id = :orderId and (step.messageEntry is not null or step.note is not null)
        order by step.id desc""")
    List<OrderStepEntity> findQueriesByOrderId(UUID orderId);

    /** Each order on a case as it stands. */
    @Query("""
        select step from OrderStepEntity step join fetch step.order
        where step.order.pcsCase.id = :caseId
        and step.id = (select max(latest.id) from OrderStepEntity latest where latest.order = step.order)""")
    List<OrderStepEntity> findLatestByCaseId(UUID caseId);
}
