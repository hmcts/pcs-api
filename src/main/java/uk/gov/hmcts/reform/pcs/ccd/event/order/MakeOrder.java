package uk.gov.hmcts.reform.pcs.ccd.event.order;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.external.ExternalEventId;
import uk.gov.hmcts.ccd.sdk.api.external.ExternalRejection;
import uk.gov.hmcts.ccd.sdk.api.external.ExternalStartRequest;
import uk.gov.hmcts.ccd.sdk.api.external.ExternalStartResponse;
import uk.gov.hmcts.ccd.sdk.api.external.ExternalSubmitRequest;
import uk.gov.hmcts.ccd.sdk.api.external.ExternalSubmitResponse;
import uk.gov.hmcts.reform.pcs.camunda.TaskCompletionService;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest.HearingChange;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest.OrderChange;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderLink;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderStart;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderState;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.OrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.OrderStepEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.hearing.HearingEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.OrderRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.message.MessageService;
import uk.gov.hmcts.reform.pcs.ccd.service.order.OrderSteps;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.idam.IdamUserIds;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CaseworkerRoles.CASEWORKER_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest.Action.SUBMIT_FOR_REVIEW;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderState.DRAFT;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderState.RETURNED_TO_JUDGE;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderState.SUBMITTED_FOR_REVIEW;

/**
 * A judge makes an order in pcs-frontend. Each judge has one working draft per case, which a change
 * with no id starts. An order a caseworker returned is the judge's to change and resubmit, opened from
 * the task its return gave them. An order made for a hearing records the case's facts and the judge's
 * note for that hearing whenever the judge saves it. An application has its own draft beside the case's.
 */
@Component
@AllArgsConstructor
public class MakeOrder implements CCDConfig<PCSCase, State, UserRole> {

    public static final ExternalEventId<OrderStart, MakeOrderRequest> MAKE_ORDER =
        ExternalEventId.of("ext:makeOrder", OrderStart.class, MakeOrderRequest.class);

    static final String ALREADY_DECIDED =
        "Another order on this application is with court staff, back with its judge, or has decided it";

    /** The states that leave an order its author's to change. */
    private static final Set<OrderState> CHANGEABLE = Set.of(DRAFT, RETURNED_TO_JUDGE);
    private static final String NO_LONGER_CHANGEABLE = "The order is no longer waiting for you to change it";
    static final String NO_WORDING = "The order's wording did not load. Reload the page and send the order again";

    private final OrderRepository orderRepository;
    private final OrderSteps orderSteps;
    private final PcsCaseRepository pcsCaseRepository;
    private final OrderCaseContext orderCaseContext;
    private final SecurityContextService securityContextService;
    private final OrderTasks orderTasks;
    private final TaskCompletionService taskCompletionService;
    private final MessageService messageService;
    private final HearingRecords hearingRecords;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .externalEvent(MAKE_ORDER, this::submit)
            .forStates(
                State.CASE_ISSUED,
                State.CASE_PROGRESSION,
                State.JUDICIAL_REFERRAL,
                State.HEARING_READINESS,
                State.PREPARE_FOR_HEARING_CONDUCT_HEARING,
                State.DECISION_OUTCOME
            )
            .name("Make an order")
            .showCondition(ShowConditions.featureFlagsEnabled(FeatureFlag.MAKE_ORDER))
            .grant(Permission.CRUD,
                UserRole.JUDGE,
                UserRole.FEE_PAID_JUDGE,
                UserRole.CIRCUIT_JUDGE,
                UserRole.LEADERSHIP_JUDGE)
            // Caseworkers review the orders judges submit, so they see them in the case history.
            .grant(Set.of(Permission.R), CASEWORKER_ROLES)
            .onStart(this::start);
    }

    private ExternalStartResponse<OrderStart> start(ExternalStartRequest start) {
        long caseReference = start.caseReference();
        String judge = IdamUserIds.normalise(start.user().id());
        PcsCaseEntity pcsCase = findCase(caseReference);
        OrderLink link = start.clientContext().as(OrderLink.class).orElse(OrderLink.NONE);
        Optional<UUID> chosenOrderId = Optional.ofNullable(link.orderId());
        UUID genAppId = chosenOrderId.isEmpty() ? Optional.ofNullable(link.genAppId())
            .map(id -> openGenApp(pcsCase, id).getId()).orElse(null) : null;
        Optional<OrderEntity> order = chosenOrderId.isPresent()
            ? Optional.of(chosenOrder(chosenOrderId.get(), caseReference, judge))
            : orderRepository.findByPcsCaseCaseReferenceAndAuthorIdamUserIdAndStateAndGenAppId(
                caseReference, judge, DRAFT, genAppId);
        if (order.isEmpty() && genAppId != null) {
            requireDecidable(pcsCase, genAppId);
        }
        OrderStart.Order workingOrder = order
            .map(o -> OrderStart.Order.of(o, orderSteps.latest(o),
                chosenOrderId.isPresent() ? messageService.lastOrderMessage(o, false).orElse(null) : null))
            .orElseGet(() -> OrderStart.Order.none(genAppId));
        // An order already under way stays with the hearing it was started for.
        HearingEntity hearing = order.map(OrderEntity::getHearing)
            .or(() -> Optional.ofNullable(link.hearingId()).map(id -> hearingRecords.hearing(pcsCase, id)))
            .orElse(null);
        return ExternalStartResponse.started(new OrderStart(workingOrder,
            orderCaseContext.of(pcsCase, workingOrder.id(), workingOrder.genAppId()),
            hearing == null ? null : hearingRecords.start(pcsCase, hearing)));
    }

    /** A returned order the judge's task links to. */
    private OrderEntity chosenOrder(UUID orderId, long caseReference, String judge) {
        return orderRepository
            .findByIdAndPcsCaseCaseReferenceAndAuthorIdamUserIdAndStateIn(orderId, caseReference, judge, CHANGEABLE)
            .orElseThrow(() -> ExternalRejection.because(NO_LONGER_CHANGEABLE));
    }

    private ExternalSubmitResponse<State> submit(ExternalSubmitRequest<MakeOrderRequest> submit) {
        MakeOrderRequest request = submit.payload();
        OrderChange change = request.order();
        // Its page posts the wording its script built; an order without it could not be reviewed or issued.
        if (request.action() == SUBMIT_FOR_REVIEW
            && (change.docweaveSnapshot() == null || change.docweaveSnapshot().isNull())) {
            throw ExternalRejection.because(NO_WORDING);
        }
        long caseReference = submit.caseReference();
        String judge = IdamUserIds.normalise(submit.user().id());
        OrderEntity order = change.id() == null
            ? newDraft(caseReference, judge, change.genAppId())
            : workingDraft(caseReference, judge, change);
        // Every application the order decides, whether made on it or at a hearing, must still be the judge's to decide.
        if (request.action() == SUBMIT_FOR_REVIEW && order.getPcsCase().getGenApps().stream()
            .anyMatch(genApp -> OrderTasks.decides(change.formData(), genApp)
                && !orderTasks.canDecide(genApp, order.getId()))) {
            throw ExternalRejection.because(ALREADY_DECIDED);
        }
        if (order.getHearing() != null && request.hearing() == null) {
            throw ExternalRejection.because("The order's hearing record is missing");
        }
        if (request.hearing() != null) {
            HearingEntity hearing = forHearing(order, request.hearing());
            // The order holds no copy of the hearing's facts and note, which are recorded only when the judge
            // changed them, so another judge's are not refused or overwritten by a save that left them alone.
            if (request.hearing().caseFacts() != null) {
                hearingRecords.record(order.getPcsCase(), hearing, request.hearing());
            }
        }
        final boolean resubmitted = order.getState() == RETURNED_TO_JUDGE;
        OrderState to = request.action() == SUBMIT_FOR_REVIEW ? SUBMITTED_FOR_REVIEW : order.getState();
        final OrderStepEntity step = orderSteps.append(order, to, judge, OrderStepEntity.builder()
            .orderType(change.orderType())
            .formData(change.formData())
            .docweaveSnapshot(change.docweaveSnapshot())
            .messageEntry(request.action() != SUBMIT_FOR_REVIEW ? null : staffMessage(change)
                .map(message -> messageService.recordOrderMessage(order, OrderTasks.inWords(change.orderType()),
                    order.getAuthorName(), true, message))
                .orElse(null)));
        if (request.action() != SUBMIT_FOR_REVIEW) {
            return ExternalSubmitResponse.accepted("Order draft saved", "Saved an order as a draft");
        }
        // The task the judge opened the order from: its return, or the application it decides.
        submit.afterCommit(taskCompletionService.complete(request.taskId()));
        if (order.getGenAppId() != null) {
            messageService.closeAnsweredByOrder(order, orderTasks.currentUserName("Judge"));
        }
        orderTasks.askStaffToReview(caseReference, step, resubmitted);
        return ExternalSubmitResponse.accepted(
            "Order submitted for review", "Submitted an order for caseworker review");
    }

    /** The judge's message for court staff with their order, if they wrote one, which the Messages tab keeps. */
    private static Optional<String> staffMessage(OrderChange change) {
        Map<String, Object> formData = change.formData() == null ? Map.of() : change.formData();
        Object ticked = formData.get("staff-message");
        boolean wanted = ticked instanceof Collection<?> values ? values.contains("yes") : "yes".equals(ticked);
        return Optional.ofNullable(formData.get("staff-message-text"))
            .map(Object::toString).map(String::trim).filter(text -> wanted && !text.isEmpty());
    }

    /** The hearing the order is for, which a change can give an order that has none. */
    private HearingEntity forHearing(OrderEntity order, HearingChange change) {
        HearingEntity hearing = hearingRecords.hearing(order.getPcsCase(), change.id());
        if (order.getHearing() == null) {
            order.setHearing(hearing);
        } else if (!order.getHearing().getId().equals(hearing.getId())) {
            throw ExternalRejection.because("The order is for a different hearing");
        }
        return hearing;
    }

    /**
     * A judge has one draft per case, and one per application on it, which the database enforces. A draft
     * on an application needs it to be open.
     */
    private OrderEntity newDraft(long caseReference, String judge, UUID genAppId) {
        PcsCaseEntity pcsCase = findCase(caseReference);
        return OrderEntity.builder()
            .pcsCase(pcsCase)
            .genAppId(genAppId == null ? null : openGenApp(pcsCase, genAppId).getId())
            .authorIdamUserId(judge)
            .authorName(securityContextService.getCurrentUserDetails().getName())
            .state(DRAFT)
            .build();
    }

    private OrderEntity workingDraft(long caseReference, String judge, OrderChange change) {
        OrderEntity draft = orderRepository
            .findByIdAndPcsCaseCaseReferenceAndAuthorIdamUserIdAndStateIn(
                change.id(), caseReference, judge, CHANGEABLE)
            .orElseThrow(() -> ExternalRejection.because("The order draft does not exist for this case"));
        orderSteps.latest(draft, change.version());
        return draft;
    }

    /** Another order on the application, under way or issued, already decides it. */
    private void requireDecidable(PcsCaseEntity pcsCase, UUID genAppId) {
        if (!orderTasks.canDecide(openGenApp(pcsCase, genAppId))) {
            throw ExternalRejection.because(ALREADY_DECIDED);
        }
    }

    /** The case's application, which an order can only be made on while it is open. */
    private static GenAppEntity openGenApp(PcsCaseEntity pcsCase, UUID genAppId) {
        GenAppEntity genApp = pcsCase.findGenApp(genAppId)
            .orElseThrow(() -> ExternalRejection.because("The application is not on this case"));
        if (!genApp.isOpen()) {
            throw ExternalRejection.because("The application is not open");
        }
        return genApp;
    }

    private PcsCaseEntity findCase(long caseReference) {
        return pcsCaseRepository.findByCaseReference(caseReference)
            .orElseThrow(() -> new CaseNotFoundException(caseReference));
    }
}
