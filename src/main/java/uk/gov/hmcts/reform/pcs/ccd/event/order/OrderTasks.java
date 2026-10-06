package uk.gov.hmcts.reform.pcs.ccd.event.order;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import uk.gov.hmcts.reform.pcs.camunda.CamundaService;
import uk.gov.hmcts.reform.pcs.camunda.TaskCompletionService;
import uk.gov.hmcts.reform.pcs.camunda.TaskSubject;
import uk.gov.hmcts.reform.pcs.camunda.TaskType;
import uk.gov.hmcts.reform.pcs.ccd.entity.DraftOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.workallocation.TaskDescriptionService;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.time.Duration;
import java.util.UUID;

/**
 * The Work Allocation tasks an order passes between court staff and its judge, one of each per order,
 * and the task asking a judge to decide a general application with an order, one per application.
 * Submitting an order asks court staff to review it; returning it with a query asks the judge who
 * wrote it to change it, assigning them the task. Each task's link starts an event in pcs-frontend on
 * the order, as whoever follows it: XUI puts their IDAM id in place of {@code ${[EXPECTED_SUB]}},
 * which the handover checks. That work happens in pcs-frontend rather than XUI, so XUI can't complete
 * the task as the event ends; the next step on the order closes it instead.
 *
 * <p>Closing a task is recorded with the event, as a request to cancel it that Camunda gets a little
 * later, so it happens even if nothing else does. Once the event is saved, the task is completed in
 * task management as the user, before the event returns, so their task lists are up to date when
 * they are back in XUI; the cancel then finds nothing left to do.
 */
@Service
public class OrderTasks {

    private static final String A_JUDGE = "A judge";
    private static final String COURT_STAFF = "Court staff";
    /** Long enough for the task to be completed first, so the cancel doesn't race it. */
    static final Duration FALLBACK_DELAY = Duration.ofSeconds(30);

    private final CamundaService camundaService;
    private final TaskCompletionService taskCompletionService;
    private final TaskDescriptionService taskDescriptionService;
    private final SecurityContextService securityContextService;
    private final String frontendUrl;

    public OrderTasks(CamundaService camundaService,
                      TaskCompletionService taskCompletionService,
                      TaskDescriptionService taskDescriptionService,
                      SecurityContextService securityContextService,
                      @Value("${frontend.url}") String frontendUrl) {
        this.camundaService = camundaService;
        this.taskCompletionService = taskCompletionService;
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
        close(caseReference, TaskType.CONFIRM_ORDER_REVIEW, order);
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
        close(caseReference, TaskType.REVIEW_ORDER_QUERY, order);
    }

    /**
     * Asks the judges to decide a general application court staff have referred, by making an order on
     * it: the task's link starts the judge's order on the application. A referral replaces any earlier
     * task to decide the application.
     */
    public void askJudgeToDecide(long caseReference, GenAppEntity genApp, String applicant) {
        TaskSubject subject = TaskSubject.genApp(genApp.getId());
        camundaService.cancelTask(caseReference, TaskType.DECIDE_GEN_APP, subject, Duration.ZERO);
        String decideUrl = "%s/cases/%d/event/ext:makeOrder?expected_sub=${[EXPECTED_SUB]}&genAppId=%s"
            .formatted(frontendUrl, caseReference, genApp.getId());
        String description = taskDescriptionService.createDecideGenAppDescription(currentUserName(COURT_STAFF),
            applicant, "GA" + genApp.getRank(), genApp.getType().getShortName(), genApp.getReferralNote(), decideUrl);
        camundaService.createTask(caseReference, TaskType.DECIDE_GEN_APP, description, subject, null);
    }

    /** Closes the judge's task to decide an application once they send their order on it for review. */
    public void closeDecideApplication(long caseReference, UUID genAppId) {
        TaskSubject subject = TaskSubject.genApp(genAppId);
        camundaService.cancelTask(caseReference, TaskType.DECIDE_GEN_APP, subject, FALLBACK_DELAY);
        afterCommit(() -> taskCompletionService.completeTasks(caseReference, TaskType.DECIDE_GEN_APP, subject));
    }

    private void close(long caseReference, TaskType taskType, DraftOrderEntity order) {
        camundaService.cancelTask(caseReference, taskType, order.getId(), FALLBACK_DELAY);
        afterCommit(() -> taskCompletionService.completeOrderTasks(caseReference, taskType, order.getId()));
    }

    private static void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
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
