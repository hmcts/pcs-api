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
 * The Work Allocation tasks an order passes between court staff and its judge. XUI fills in each task
 * link: {@code ${[EXPECTED_SUB]}} with the user's IDAM id, which the frontend's handover checks, and
 * {@code ${[id]}} with the task's id, which the next step sends back so the task can be completed.
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

    public void askStaffToReview(long caseReference, OrderStepEntity submitted, boolean resubmitted) {
        String description = taskDescriptionService.createConfirmOrderReviewDescription(
            currentUserName(A_JUDGE), inWords(submitted.getOrderType()),
            eventUrl(caseReference, "ext:confirmOrderReview", submitted.getOrder()),
            resubmitted);
        camundaService.createTask(caseReference, TaskType.CONFIRM_ORDER_REVIEW, description);
    }

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
