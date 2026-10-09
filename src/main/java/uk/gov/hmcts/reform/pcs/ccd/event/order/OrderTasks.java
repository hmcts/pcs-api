package uk.gov.hmcts.reform.pcs.ccd.event.order;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.camunda.CamundaService;
import uk.gov.hmcts.reform.pcs.camunda.TaskType;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderType;
import uk.gov.hmcts.reform.pcs.ccd.entity.OrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.OrderStepEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.workallocation.TaskDescriptionService;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.Locale;

/**
 * The Work Allocation tasks an order passes between court staff and its judge, one of each per order.
 * Submitting an order asks court staff to review it; returning it with a query asks the judge who
 * wrote it to change it, assigning them the task. Each task's link starts an event in pcs-frontend on
 * the order, as whoever follows it: XUI puts their IDAM id in place of {@code ${[EXPECTED_SUB]}}, which
 * the handover checks, and the task's id in place of {@code ${[id]}}, which the next step on the order
 * sends back so the task can be completed as that step returns.
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
    public void askStaffToReview(long caseReference, OrderStepEntity submitted, boolean resubmitted) {
        String description = taskDescriptionService.createConfirmOrderReviewDescription(
            currentUserName(A_JUDGE), inWords(submitted.getOrderType()),
            eventUrl(caseReference, "ext:confirmOrderReview", submitted.getOrder()),
            resubmitted);
        camundaService.createTask(caseReference, TaskType.CONFIRM_ORDER_REVIEW, description);
    }

    /** Asks the judge who wrote the order to answer the query court staff returned it with. */
    public void askJudgeToChange(long caseReference, OrderStepEntity returned, String query) {
        OrderEntity order = returned.getOrder();
        String description = taskDescriptionService.createReviewOrderQueryDescription(
            currentUserName(COURT_STAFF), inWords(returned.getOrderType()), query,
            eventUrl(caseReference, "ext:makeOrder", order));
        camundaService.createTask(caseReference, TaskType.REVIEW_ORDER_QUERY, description,
            order.getAuthorIdamUserId());
    }

    private String eventUrl(long caseReference, String event, OrderEntity order) {
        return "%s/cases/%d/event/%s?expected_sub=${[EXPECTED_SUB]}&orderId=%s&taskId=${[id]}"
            .formatted(frontendUrl, caseReference, event, order.getId());
    }

    private static String inWords(OrderType orderType) {
        return orderType.getLabel().toLowerCase(Locale.UK);
    }

    private String currentUserName(String otherwise) {
        UserInfo user = securityContextService.getCurrentUserDetails();
        return user == null || user.getName() == null || user.getName().isBlank() ? otherwise : user.getName();
    }
}
