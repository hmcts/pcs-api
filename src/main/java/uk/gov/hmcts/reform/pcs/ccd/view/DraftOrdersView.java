package uk.gov.hmcts.reform.pcs.ccd.view;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.DraftOrderState;
import uk.gov.hmcts.reform.pcs.ccd.entity.DraftOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.DraftOrderRepository;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The case's orders as the draft orders tab shows them: each order's type and where it is, with a
 * link for the judge who wrote it to resume their draft, or to change an order a caseworker returned
 * to them. The tab is rendered a second time for court staff, with a link to review each order
 * awaiting review in pcs-frontend's confirm order review journey. CCD shows a user the rendering
 * their roles have, so no one's roles are looked up as the case loads.
 * A link names the user the tab is shown to, as XUI does when it hands an event over to pcs-frontend,
 * so the event runs as them and not as whoever pcs-frontend last had signed in.
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
    private final SecurityContextService securityContextService;
    private final String frontendUrl;

    public DraftOrdersView(DraftOrderRepository draftOrderRepository,
                           SecurityContextService securityContextService,
                           @Value("${frontend.url}") String frontendUrl) {
        this.draftOrderRepository = draftOrderRepository;
        this.securityContextService = securityContextService;
        this.frontendUrl = frontendUrl;
    }

    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        List<DraftOrderEntity> orders = draftOrderRepository.findByPcsCaseId(pcsCaseEntity.getId());
        UUID viewer = viewer();
        pcsCase.setDraftOrdersMarkdown(table(orders, order -> authorAction(order, viewer)));
        pcsCase.setDraftOrdersReviewMarkdown(table(orders, order -> reviewAction(order, viewer)));
    }

    /** The user the tab is shown to; the system user is shown no links. */
    private UUID viewer() {
        return securityContextService.isSystemUser() ? null : securityContextService.getCurrentUserId();
    }

    private String table(List<DraftOrderEntity> orders, Function<DraftOrderEntity, String> action) {
        if (orders.isEmpty()) {
            return "There are no orders on this case.";
        }
        String rows = orders.stream()
            // Orders waiting for review first, as the tab is where caseworkers pick one up.
            .sorted(Comparator.comparing(order -> order.getState() != DraftOrderState.SUBMITTED_FOR_REVIEW))
            .map(order -> row(order, action.apply(order)))
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

    /** Only the judge who wrote an order can go on with their draft, or change it once it is returned. */
    private String authorAction(DraftOrderEntity order, UUID viewer) {
        if (!order.getAuthorIdamUserId().equals(viewer)) {
            return "";
        }
        return switch (order.getState()) {
            case DRAFT -> link("ext:makeOrder", "Resume draft", order, viewer);
            case RETURNED_TO_JUDGE -> link("ext:makeOrder", "Change order", order, viewer);
            default -> "";
        };
    }

    /** Court staff review an order that is waiting for it. */
    private String reviewAction(DraftOrderEntity order, UUID viewer) {
        return order.getState() == DraftOrderState.SUBMITTED_FOR_REVIEW && viewer != null
            ? link("ext:confirmOrderReview", "Review order", order, viewer)
            : "";
    }

    private String row(DraftOrderEntity order, String action) {
        return """
            <tr class="govuk-table__row">
            <td class="govuk-table__cell">%s</td>
            <td class="govuk-table__cell">%s</td>
            <td class="govuk-table__cell">%s</td>
            </tr>""".formatted(orderType(order.getOrderType()), STATES.get(order.getState()), action);
    }

    /**
     * A link to one of pcs-frontend's order events for this order. Its event handover signs the viewer
     * in if it has someone else signed in, then starts the event on the order.
     */
    private String link(String event, String text, DraftOrderEntity order, UUID viewer) {
        return ("<a class=\"govuk-link\" href=\"%s/cases/${[CASE_REFERENCE]}/event/%s"
            + "?expected_sub=%s&amp;orderId=%s\">%s</a>").formatted(frontendUrl, event, viewer, order.getId(), text);
    }

    /** The order type as the frontend named it, such as OUTRIGHT_POSSESSION, in words. */
    private static String orderType(String orderType) {
        String words = orderType.replace('_', ' ').toLowerCase();
        return HtmlUtils.htmlEscape(Character.toUpperCase(words.charAt(0)) + words.substring(1));
    }
}
