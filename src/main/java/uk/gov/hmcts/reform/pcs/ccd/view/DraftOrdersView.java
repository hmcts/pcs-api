package uk.gov.hmcts.reform.pcs.ccd.view;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.DraftOrderState;
import uk.gov.hmcts.reform.pcs.ccd.entity.DraftOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.DraftOrderRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The case's orders as the draft orders tab shows them: each order's type and where it is, with a link
 * to review each order awaiting a caseworker's review in pcs-frontend's confirm order review journey.
 */
@Component
public class DraftOrdersView {

    private static final Map<DraftOrderState, String> STATES = Map.of(
        DraftOrderState.SUBMITTED_FOR_REVIEW, "Waiting for review",
        DraftOrderState.RETURNED_TO_JUDGE, "Returned to judge",
        DraftOrderState.DRAFT, "Draft",
        DraftOrderState.ISSUED, "Issued"
    );

    private final DraftOrderRepository draftOrderRepository;
    private final String frontendUrl;

    public DraftOrdersView(DraftOrderRepository draftOrderRepository, @Value("${frontend.url}") String frontendUrl) {
        this.draftOrderRepository = draftOrderRepository;
        this.frontendUrl = frontendUrl;
    }

    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        List<DraftOrderEntity> orders = draftOrderRepository.findByPcsCaseId(pcsCaseEntity.getId());
        pcsCase.setDraftOrdersMarkdown(orders.isEmpty() ? "There are no orders on this case." : table(orders));
    }

    private String table(List<DraftOrderEntity> orders) {
        String rows = orders.stream()
            // Orders waiting for review first, as the tab is where caseworkers pick one up.
            .sorted(Comparator.comparing(order -> order.getState() != DraftOrderState.SUBMITTED_FOR_REVIEW))
            .map(this::row)
            .collect(Collectors.joining());
        return """
            <table class="govuk-table">
            <thead class="govuk-table__head"><tr class="govuk-table__row">
            <th scope="col" class="govuk-table__header">Order</th>
            <th scope="col" class="govuk-table__header">Status</th>
            <th scope="col" class="govuk-table__header"><span class="govuk-visually-hidden">Action</span></th>
            </tr></thead>
            <tbody class="govuk-table__body">%s</tbody>
            </table>
            """.formatted(rows);
    }

    private String row(DraftOrderEntity order) {
        String action = order.getState() == DraftOrderState.SUBMITTED_FOR_REVIEW
            ? "<a class=\"govuk-link\" href=\"%s/case/${[CASE_REFERENCE]}/confirm-order-review?orderId=%s\">Review order</a>"
                .formatted(frontendUrl, order.getId())
            : "";
        return """
            <tr class="govuk-table__row">
            <td class="govuk-table__cell">%s</td>
            <td class="govuk-table__cell">%s</td>
            <td class="govuk-table__cell">%s</td>
            </tr>""".formatted(orderType(order.getOrderType()), STATES.get(order.getState()), action);
    }

    /** The order type as the frontend named it, such as OUTRIGHT_POSSESSION, in words. */
    private static String orderType(String orderType) {
        String words = orderType.replace('_', ' ').toLowerCase();
        return HtmlUtils.htmlEscape(Character.toUpperCase(words.charAt(0)) + words.substring(1));
    }
}
