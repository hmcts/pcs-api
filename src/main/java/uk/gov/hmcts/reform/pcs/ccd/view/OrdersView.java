package uk.gov.hmcts.reform.pcs.ccd.view;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderState;
import uk.gov.hmcts.reform.pcs.ccd.entity.OrderStepEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.OrderStepRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The case's orders as the orders tab shows them: each order's type and where it is. The work on an
 * order is reached from the Work Allocation task that asks for it, not from here.
 */
@Component
@AllArgsConstructor
public class OrdersView {

    private static final Map<OrderState, String> STATES = Map.of(
        OrderState.DRAFT, "Draft",
        OrderState.SUBMITTED_FOR_REVIEW, "Waiting for review",
        OrderState.RETURNED_TO_JUDGE, "Returned to judge",
        OrderState.ISSUED, "Issued"
    );

    private final OrderStepRepository orderStepRepository;

    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        List<OrderStepEntity> orders = orderStepRepository.findLatestByCaseId(pcsCaseEntity.getId());
        pcsCase.setOrdersMarkdown(orders.isEmpty() ? "There are no orders on this case." : table(orders));
    }

    private static String table(List<OrderStepEntity> orders) {
        String rows = orders.stream().map(OrdersView::row).collect(Collectors.joining());
        return """
            <table class="govuk-table">
            <thead class="govuk-table__head"><tr class="govuk-table__row">
            <th scope="col" class="govuk-table__header">Order</th>
            <th scope="col" class="govuk-table__header">Status</th>
            </tr></thead>
            <tbody class="govuk-table__body">%s</tbody>
            </table>
            """.formatted(rows);
    }

    private static String row(OrderStepEntity order) {
        return """
            <tr class="govuk-table__row">
            <td class="govuk-table__cell">%s</td>
            <td class="govuk-table__cell">%s</td>
            </tr>""".formatted(order.getOrderType().getLabel(), STATES.get(order.getState()));
    }
}
