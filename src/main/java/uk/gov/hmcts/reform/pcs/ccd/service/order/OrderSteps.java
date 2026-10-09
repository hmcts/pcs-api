package uk.gov.hmcts.reform.pcs.ccd.service.order;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.external.ExternalRejection;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderState;
import uk.gov.hmcts.reform.pcs.ccd.entity.OrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.OrderStepEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.OrderRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.OrderStepRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Every change to an order is a step appended to it, leaving the order in a state. The latest step
 * is the order as it stands, and its id is the order's version: the frontend sends it back with a
 * change, so a change made from an older copy of the order is refused. CCD runs one event on a case
 * at a time, so two changes cannot race each other here.
 */
@Component
@AllArgsConstructor
public class OrderSteps {

    private final OrderRepository orderRepository;
    private final OrderStepRepository orderStepRepository;
    private final Clock utcClock;

    /** The order as it stands. */
    public OrderStepEntity latest(OrderEntity order) {
        return orderStepRepository.findFirstByOrderIdOrderByIdDesc(order.getId()).orElseThrow();
    }

    /** The order as it stands, if that is the version the user last saw. */
    public OrderStepEntity latest(OrderEntity order, long versionSeen) {
        OrderStepEntity latest = latest(order);
        if (latest.getId() != versionSeen) {
            throw ExternalRejection.because("The order has been updated by another user. Reload it and try again");
        }
        return latest;
    }

    /** The query the order was last returned to its judge with. */
    public Optional<String> query(OrderEntity order) {
        return orderStepRepository.findFirstByOrderIdAndNoteIsNotNullOrderByIdDesc(order.getId())
            .map(returned -> returned.getNote().getNote());
    }

    public OrderStepEntity append(OrderEntity order, OrderState to, UUID actor,
                                  OrderStepEntity.OrderStepEntityBuilder step) {
        order.setState(to);
        orderRepository.save(order);
        return orderStepRepository.saveAndFlush(step
            .order(order)
            .state(to)
            .actorIdamUserId(actor)
            .createdAt(Instant.now(utcClock))
            .build());
    }
}
