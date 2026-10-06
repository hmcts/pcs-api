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
 * The Work Allocation task asking court staff to review an order a judge submitted, one per order.
 * Its link starts the confirm order review journey in pcs-frontend on that order, as whoever follows
 * it: XUI puts their IDAM id in place of {@code ${[EXPECTED_SUB]}}, which the handover checks. The
 * review happens in pcs-frontend rather than XUI, so XUI can't complete the task as the event ends;
 * returning or issuing the order closes it instead.
 */
@Service
public class ConfirmOrderReviewTask {

    private static final String SOMEONE = "A judge";

    private final CamundaService camundaService;
    private final TaskDescriptionService taskDescriptionService;
    private final SecurityContextService securityContextService;
    private final String frontendUrl;

    public ConfirmOrderReviewTask(CamundaService camundaService,
                                  TaskDescriptionService taskDescriptionService,
                                  SecurityContextService securityContextService,
                                  @Value("${frontend.url}") String frontendUrl) {
        this.camundaService = camundaService;
        this.taskDescriptionService = taskDescriptionService;
        this.securityContextService = securityContextService;
        this.frontendUrl = frontendUrl;
    }

    /** Asks court staff to review the order the judge has just submitted, again if it had been returned to them. */
    public void create(long caseReference, DraftOrderEntity order, boolean resubmitted) {
        String reviewUrl = "%s/cases/%d/event/ext:confirmOrderReview?expected_sub=${[EXPECTED_SUB]}&orderId=%s"
            .formatted(frontendUrl, caseReference, order.getId());
        String description = taskDescriptionService.createConfirmOrderReviewDescription(
            judgeName(), order.getOrderType().replace('_', ' ').toLowerCase(), reviewUrl, resubmitted);
        camundaService.createTask(caseReference, TaskType.CONFIRM_ORDER_REVIEW, description, order.getId());
    }

    /** Closes the order's task once it has been returned to the judge or issued. */
    public void close(long caseReference, DraftOrderEntity order) {
        camundaService.cancelTask(caseReference, TaskType.CONFIRM_ORDER_REVIEW, order.getId());
    }

    private String judgeName() {
        UserInfo judge = securityContextService.getCurrentUserDetails();
        return judge == null || judge.getName() == null || judge.getName().isBlank() ? SOMEONE : judge.getName();
    }
}
