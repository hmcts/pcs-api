package uk.gov.hmcts.reform.pcs.ccd.view;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.DraftOrderState;
import uk.gov.hmcts.reform.pcs.ccd.entity.DraftOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.DraftOrderRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.UserRoleService;
import uk.gov.hmcts.reform.pcs.ccd.service.UserRoles;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CaseworkerRoles.CASEWORKER_ROLES;

/**
 * The case's orders as the draft orders tab shows them: each order's type and where it is, with a link
 * for a caseworker to review each order awaiting review in pcs-frontend's confirm order review journey,
 * and for the judge who wrote it to change an order a caseworker returned to them. Anyone else sees
 * where each order is, without a link.
 * The link names the user the tab is shown to, as XUI does when it hands an event over to pcs-frontend,
 * so the review is made by them and not by whoever pcs-frontend last had signed in.
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
    private final UserRoleService userRoleService;
    private final String frontendUrl;

    public DraftOrdersView(DraftOrderRepository draftOrderRepository,
                           UserRoleService userRoleService,
                           @Value("${frontend.url}") String frontendUrl) {
        this.draftOrderRepository = draftOrderRepository;
        this.userRoleService = userRoleService;
        this.frontendUrl = frontendUrl;
    }

    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        List<DraftOrderEntity> orders = draftOrderRepository.findByPcsCaseId(pcsCaseEntity.getId());
        pcsCase.setDraftOrdersMarkdown(orders.isEmpty()
            ? "There are no orders on this case."
            : table(orders, userRoleService.getCurrentUserOrganisationalRoles()));
    }

    /** Whether the tab is shown to a caseworker, who are the users the confirm order review event is granted to. */
    private static boolean mayReview(UserRoles viewer) {
        return Stream.of(CASEWORKER_ROLES).map(UserRole::getRole).anyMatch(viewer.roles()::contains);
    }

    private String table(List<DraftOrderEntity> orders, UserRoles viewer) {
        String rows = orders.stream()
            // Orders waiting for review first, as the tab is where caseworkers pick one up.
            .sorted(Comparator.comparing(order -> order.getState() != DraftOrderState.SUBMITTED_FOR_REVIEW))
            .map(order -> row(order, viewer))
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

    private String row(DraftOrderEntity order, UserRoles viewer) {
        String action = "";
        if (order.getState() == DraftOrderState.SUBMITTED_FOR_REVIEW && mayReview(viewer)) {
            action = link("ext:confirmOrderReview", "Review order", order, viewer);
        } else if (order.getState() == DraftOrderState.RETURNED_TO_JUDGE
            && order.getAuthorIdamUserId().equals(viewer.userId())) {
            // Only the judge who wrote a returned order can change it.
            action = link("ext:makeOrder", "Change order", order, viewer);
        }
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
    private String link(String event, String text, DraftOrderEntity order, UserRoles viewer) {
        return ("<a class=\"govuk-link\" href=\"%s/cases/${[CASE_REFERENCE]}/event/%s"
            + "?expected_sub=%s&amp;orderId=%s\">%s</a>")
            .formatted(frontendUrl, event, viewer.userId(), order.getId(), text);
    }

    /** The order type as the frontend named it, such as OUTRIGHT_POSSESSION, in words. */
    private static String orderType(String orderType) {
        String words = orderType.replace('_', ' ').toLowerCase();
        return HtmlUtils.htmlEscape(Character.toUpperCase(words.charAt(0)) + words.substring(1));
    }
}
