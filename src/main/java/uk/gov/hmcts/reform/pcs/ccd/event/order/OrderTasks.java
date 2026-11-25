package uk.gov.hmcts.reform.pcs.ccd.event.order;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.api.external.ExternalRejection;
import uk.gov.hmcts.reform.pcs.camunda.CamundaService;
import uk.gov.hmcts.reform.pcs.camunda.TaskType;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.ApplicationDecision;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderType;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.OrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.OrderStepEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.OrderRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.workallocation.TaskDescriptionService;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.List;
import java.util.Locale;

/**
 * The Work Allocation tasks an order passes between court staff and its judge, and the link that starts a judge's
 * order deciding a general application. What either side writes about the order is in its messages. XUI fills in
 * each task link: {@code ${[EXPECTED_SUB]}} with the user's IDAM id, which the frontend's handover checks, and
 * {@code ${[id]}} with the task's id, which the next step sends back so the task can be completed.
 */
@Service
public class OrderTasks {

    private static final String A_JUDGE = "A judge";
    private static final String COURT_STAFF = "Court staff";

    private final CamundaService camundaService;
    private final TaskDescriptionService taskDescriptionService;
    private final SecurityContextService securityContextService;
    private final OrderRepository orderRepository;
    private final String frontendUrl;

    public OrderTasks(CamundaService camundaService,
                      TaskDescriptionService taskDescriptionService,
                      SecurityContextService securityContextService,
                      OrderRepository orderRepository,
                      @Value("${frontend.url}") String frontendUrl) {
        this.camundaService = camundaService;
        this.taskDescriptionService = taskDescriptionService;
        this.securityContextService = securityContextService;
        this.orderRepository = orderRepository;
        this.frontendUrl = frontendUrl;
    }

    public void askStaffToReview(long caseReference, OrderStepEntity submitted, boolean resubmitted) {
        String description = taskDescriptionService.createConfirmOrderReviewDescription(
            currentUserName(A_JUDGE), inWords(submitted.getOrderType()),
            eventUrl(caseReference, "ext:confirmOrderReview", submitted.getOrder()),
            resubmitted);
        camundaService.createTask(caseReference, TaskType.CONFIRM_ORDER_REVIEW, description);
    }

    public void askJudgeToChange(long caseReference, OrderStepEntity returned) {
        OrderEntity order = returned.getOrder();
        String description = taskDescriptionService.createReviewOrderQueryDescription(
            currentUserName(COURT_STAFF), inWords(returned.getOrderType()),
            eventUrl(caseReference, "ext:makeOrder", order));
        camundaService.createTask(caseReference, TaskType.REVIEW_ORDER_QUERY, description,
            order.getAuthorIdamUserId());
    }

    /** Whether a judge can decide the application: it is open, and no issued order has decided it. */
    public boolean canDecide(GenAppEntity genApp) {
        return genApp.isOpen() && !orderRepository.isGenAppDecided(genApp.getId());
    }

    /**
     * Refuses an order deciding an application that is not the case's, or that has closed or another order has decided
     * since the page was opened, saying why and what the user does next, rather than leaving the database to refuse it.
     */
    public void requireDecidable(PcsCaseEntity pcsCase, List<ApplicationDecision> decisions, String nextStep) {
        for (ApplicationDecision decided : decisions) {
            GenAppEntity genApp = pcsCase.findGenApp(decided.genAppId())
                .orElseThrow(() -> ExternalRejection.because("The application is not on this case"));
            if (!genApp.isOpen()) {
                throw ExternalRejection.because(
                    "Application GA%d is no longer open. %s".formatted(genApp.getRank(), nextStep));
            }
            if (orderRepository.isGenAppDecided(genApp.getId())) {
                throw ExternalRejection.because(
                    "Another order has decided application GA%d. %s".formatted(genApp.getRank(), nextStep));
            }
        }
    }

    /** Whether an issued order has listed the application for a hearing, where it is still to be decided. */
    public boolean isListed(GenAppEntity genApp) {
        return orderRepository.isGenAppListed(genApp.getId());
    }

    /** The task link that starts the judge's order deciding the application. */
    public String decideUrl(long caseReference, GenAppEntity genApp) {
        return "%s/cases/%d/event/ext:makeOrder?expected_sub=${[EXPECTED_SUB]}&genAppId=%s&taskId=${[id]}"
            .formatted(frontendUrl, caseReference, genApp.getId());
    }

    private String eventUrl(long caseReference, String event, OrderEntity order) {
        return "%s/cases/%d/event/%s?expected_sub=${[EXPECTED_SUB]}&orderId=%s&taskId=${[id]}"
            .formatted(frontendUrl, caseReference, event, order.getId());
    }

    static String inWords(OrderType orderType) {
        return orderType.getLabel().toLowerCase(Locale.UK);
    }

    String currentUserName(String otherwise) {
        UserInfo user = securityContextService.getCurrentUserDetails();
        return user == null || user.getName() == null || user.getName().isBlank() ? otherwise : user.getName();
    }
}
