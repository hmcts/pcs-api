package uk.gov.hmcts.reform.pcs.ccd.service.order;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.entity.DraftOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.IssuedOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.OrderReviewEntity;
import uk.gov.hmcts.reform.pcs.ccd.model.OrderDocumentTaskData;
import uk.gov.hmcts.reform.pcs.ccd.repository.IssuedOrderRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.task.OrderDocumentGenerationComponent.ORDER_DOCUMENT_TASK_DESCRIPTOR;

/**
 * Records an order as a caseworker issues it, and schedules the generation of its document, which
 * runs once the issuing event has been committed.
 */
@Service
@AllArgsConstructor
public class IssuedOrderService {

    private final IssuedOrderRepository issuedOrderRepository;
    private final SchedulerClient schedulerClient;
    private final Clock ukClock;

    public IssuedOrderEntity issue(DraftOrderEntity order, OrderReviewEntity review, String html, UUID caseworker) {
        IssuedOrderEntity issued = issuedOrderRepository.saveAndFlush(IssuedOrderEntity.builder()
            .pcsCase(order.getPcsCase())
            .draftOrder(order)
            .orderReview(review)
            .orderType(review.getOrderType())
            .orderHtml(OrderHtml.clean(html))
            .seal(review.getSeal())
            .finalOrder(Boolean.TRUE.equals(review.getFinalOrder()))
            .issuedByIdamUserId(caseworker)
            .issuedOn(LocalDate.now(ukClock))
            .build());
        String id = issued.getId().toString();
        schedulerClient.scheduleIfNotExists(ORDER_DOCUMENT_TASK_DESCRIPTOR
            .instance(id)
            .data(new OrderDocumentTaskData(id))
            .scheduledTo(Instant.now()));
        return issued;
    }
}
