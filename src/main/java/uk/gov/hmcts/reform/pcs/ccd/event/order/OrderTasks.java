package uk.gov.hmcts.reform.pcs.ccd.event.order;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.camunda.CamundaService;
import uk.gov.hmcts.reform.pcs.camunda.TaskType;
import uk.gov.hmcts.reform.pcs.ccd.entity.DraftOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.workallocation.TaskDescriptionService;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

/**
 * The Work Allocation tasks an order passes between court staff and its judge, one of each per order.
 * Submitting an order asks court staff to review it; returning it with a query asks the judge who
 * wrote it to change it, assigning them the task. Each task's link starts an event in pcs-frontend on
 * the order, as whoever follows it: XUI puts their IDAM id in place of {@code ${[EXPECTED_SUB]}},
 * which the handover checks. That work happens in pcs-frontend rather than XUI, so XUI can't complete
 * the task as the event ends; the next step on the order closes it instead.
 */
@Service
public class OrderTasks {

    private static final String A_JUDGE = "A judge";
    private static final String COURT_STAFF = "Court staff";

    private final CamundaService camundaService;
    private final TaskDescriptionService taskDescriptionService;
    private final SecurityContextService securityContextService;
    private final String frontendUrl;

    public OrderTasks(CamundaService camundaService,
                      TaskDescriptionService taskDescriptionService,
                      SecurityContextService securityContextService,
                      @Value("${frontend.url}") String frontendUrl) {
        this.camundaService = camundaService;
        this.taskDescriptionService = taskDescriptionService;
        this.securityContextService = securityContextService;
        this.frontendUrl = frontendUrl;
    }

    /** Asks court staff to review the order the judge has just submitted, again if it had been returned to them. */
    public void askStaffToReview(long caseReference, DraftOrderEntity order, boolean resubmitted) {
        String description = taskDescriptionService.createConfirmOrderReviewDescription(
            currentUserName(A_JUDGE), orderType(order), eventUrl(caseReference, "ext:confirmOrderReview", order),
            resubmitted);
        camundaService.createTask(caseReference, TaskType.CONFIRM_ORDER_REVIEW, description, order.getId());
    }

    /** Closes the staff review task once the order has been returned to the judge or issued. */
    public void closeStaffReview(long caseReference, DraftOrderEntity order) {
        camundaService.cancelTask(caseReference, TaskType.CONFIRM_ORDER_REVIEW, order.getId());
    }

    /** Asks the judge who wrote the order to answer the query court staff returned it with. */
    public void askJudgeToChange(long caseReference, DraftOrderEntity order, String query) {
        String description = taskDescriptionService.createReviewOrderQueryDescription(
            currentUserName(COURT_STAFF), orderType(order), query, eventUrl(caseReference, "ext:makeOrder", order));
        camundaService.createTask(caseReference, TaskType.REVIEW_ORDER_QUERY, description, order.getId(),
            order.getAuthorIdamUserId());
    }

    /** Closes the judge's query task once they submit the order for review again. */
    public void closeJudgeQuery(long caseReference, DraftOrderEntity order) {
        camundaService.cancelTask(caseReference, TaskType.REVIEW_ORDER_QUERY, order.getId());
    }

    private String eventUrl(long caseReference, String event, DraftOrderEntity order) {
        return "%s/cases/%d/event/%s?expected_sub=${[EXPECTED_SUB]}&orderId=%s"
            .formatted(frontendUrl, caseReference, event, order.getId());
    }

    private static String orderType(DraftOrderEntity order) {
        return order.getOrderType().replace('_', ' ').toLowerCase();
    }

    private String currentUserName(String otherwise) {
        UserInfo user = securityContextService.getCurrentUserDetails();
        return user == null || user.getName() == null || user.getName().isBlank() ? otherwise : user.getName();
    }
}
