package uk.gov.hmcts.reform.pcs.ccd.event.order;

import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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
import uk.gov.hmcts.ccd.sdk.bankholidays.BankHolidays;
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
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CaseworkerRoles.CASEWORKER_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest.Action.SUBMIT_FOR_REVIEW;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderState.DRAFT;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderState.RETURNED_TO_JUDGE;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderState.SUBMITTED_FOR_REVIEW;

/**
 * A judge makes an order in pcs-frontend. Each judge has one working draft on the case, which a change with no id
 * starts and every task or link opens, until they send it for review. The judge chooses which of the
 * case's hearings the order is made at, or none, and can change it until they send it; a link's hearing is only where
 * a new order starts. An order a caseworker returned is the judge's to change and resubmit beside their draft,
 * opened from the task its return gave them. An order
 * made for a hearing records the case's facts and the judge's note for that hearing whenever the judge saves it.
 * The task that asks a judge to decide an application names it, for the page to put first and require.
 */
@Component
@AllArgsConstructor
public class MakeOrder implements CCDConfig<PCSCase, State, UserRole> {

    public static final ExternalEventId<OrderStart, MakeOrderRequest> MAKE_ORDER =
        ExternalEventId.of("ext:makeOrder", OrderStart.class, MakeOrderRequest.class);

    static final String NOT_AVAILABLE = "Making orders is not available";
    static final String ALREADY_DECIDED =
        "Another order has decided this application";
    static final String NOT_OPEN = "The application is not open";

    /** The states that leave an order its author's to change. */
    private static final Set<OrderState> CHANGEABLE = Set.of(DRAFT, RETURNED_TO_JUDGE);
    private static final String NO_LONGER_CHANGEABLE = "The order is no longer waiting for you to change it";
    static final String NO_WORDING = "The order's wording did not load. Reload the page and send the order again";
    static final String ALREADY_DRAFTING = "You already have a draft order on this case, saved since this page was "
        + "opened. Nothing on this page was saved. Copy anything you want to keep, then reload the page to open "
        + "your draft";

    private final OrderRepository orderRepository;
    private final OrderSteps orderSteps;
    private final PcsCaseRepository pcsCaseRepository;
    private final OrderCaseContext orderCaseContext;
    private final SecurityContextService securityContextService;
    private final OrderTasks orderTasks;
    private final TaskCompletionService taskCompletionService;
    private final MessageService messageService;
    private final HearingRecords hearingRecords;
    private final FeatureToggleService featureToggleService;
    private final BankHolidays bankHolidays;

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
        requireMakeOrder(featureToggleService);
        long caseReference = start.caseReference();
        String judge = IdamUserIds.normalise(start.user().id());
        PcsCaseEntity pcsCase = findCase(caseReference);
        OrderLink link = start.clientContext().as(OrderLink.class).orElse(OrderLink.NONE);
        // The judge's one draft on the case, wherever it is made, unless the link names an order of theirs.
        Optional<OrderEntity> order = link.orderId() != null
            ? Optional.of(chosenOrder(link.orderId(), caseReference, judge))
            : draft(caseReference, judge);
        GenAppEntity task = link.genAppId() == null ? null : genApp(pcsCase, link.genAppId());
        // A judge reopens their order whatever has become of the application since; issuing the order refuses it.
        if (task != null && !orderTasks.canDecide(task)) {
            if (order.isEmpty()) {
                throw ExternalRejection.because(task.isOpen() ? ALREADY_DECIDED : NOT_OPEN);
            }
            task = null;
        }
        // A saved order stays where it is made, even if its hearing was cancelled since, whichever hearing the link
        // names: a link's is only where a new order starts. The judge chooses another on the page, which has them all.
        HearingEntity hearing = order.isPresent() ? order.get().getHearing() : madeAt(pcsCase, link.hearingId());
        List<HearingEntity> choices = Stream.concat(Stream.ofNullable(hearing), pcsCase.getHearings().stream()
                .filter(HearingRecords::canHaveOrder)
                .filter(other -> hearing == null || !other.getId().equals(hearing.getId()))
                .sorted(Comparator.comparing(HearingEntity::getHearingDate)))
            .toList();
        List<OrderStart.Hearing> started = hearingRecords.start(pcsCase, choices);
        int others = hearing == null ? 0 : 1;
        OrderStart.Order workingOrder = order
            .map(o -> OrderStart.Order.of(o, orderSteps.latest(o),
                link.orderId() != null ? messageService.lastQuery(o).orElse(null) : null))
            .orElse(OrderStart.Order.NONE);
        return ExternalStartResponse.started(new OrderStart(workingOrder, orderCaseContext.of(pcsCase),
            hearing == null ? null : started.getFirst(), started.subList(others, started.size()),
            task == null ? null : task.getId(),
            OrderStart.judge(securityContextService.getCurrentUserDetails().getName()),
            OrderStart.bankHolidays(bankHolidays)));
    }

    /** One of the case's hearings, or none for an order made without a hearing. */
    private HearingEntity madeAt(PcsCaseEntity pcsCase, Integer hearingId) {
        return hearingId == null ? null : hearingRecords.hearing(pcsCase, hearingId);
    }

    /** The judge's draft on the case, of which they have one. */
    private Optional<OrderEntity> draft(long caseReference, String judge) {
        return orderRepository.findByPcsCaseCaseReferenceAndAuthorIdamUserIdAndState(caseReference, judge, DRAFT);
    }

    /** The judge's draft, or an order returned to them, that their link names. */
    private OrderEntity chosenOrder(UUID orderId, long caseReference, String judge) {
        return orderRepository
            .findByIdAndPcsCaseCaseReferenceAndAuthorIdamUserIdAndStateIn(orderId, caseReference, judge, CHANGEABLE)
            .orElseThrow(() -> ExternalRejection.because(NO_LONGER_CHANGEABLE));
    }

    private ExternalSubmitResponse<State> submit(ExternalSubmitRequest<MakeOrderRequest> submit) {
        requireMakeOrder(featureToggleService);
        MakeOrderRequest request = submit.payload();
        if (request.action() == null) {
            throw ExternalRejection.because("Select whether to save the draft or send the order for review");
        }
        OrderChange change = request.order();
        // Its page posts the wording its script built; an order without it could not be reviewed or issued.
        if (request.action() == SUBMIT_FOR_REVIEW
            && (change.docweaveSnapshot() == null || change.docweaveSnapshot().isNull())) {
            throw ExternalRejection.because(NO_WORDING);
        }
        long caseReference = submit.caseReference();
        String judge = IdamUserIds.normalise(submit.user().id());
        // A page opened before the judge's draft was saved elsewhere starts no second one, and replaces nothing.
        if (change.id() == null && draft(caseReference, judge).isPresent()) {
            throw ExternalRejection.because(ALREADY_DRAFTING);
        }
        OrderEntity order = change.id() == null
            ? newDraft(caseReference, judge, request.hearing())
            : workingDraft(caseReference, judge, change);
        final List<GenAppEntity> decided = order.getPcsCase().getGenApps().stream()
            .filter(genApp -> change.applicationDecisions().stream()
                .anyMatch(decision -> decision.genAppId().equals(genApp.getId())))
            .toList();
        // The judge chooses the hearing the order is made at, or none, whatever brought them to it.
        Integer hearingId = order.getHearing() == null ? null : order.getHearing().getId();
        Integer chosenHearingId = request.hearing() == null ? null : request.hearing().id();
        HearingEntity chosen = Objects.equals(hearingId, chosenHearingId) ? order.getHearing()
            : madeAt(order.getPcsCase(), chosenHearingId);
        order.setHearing(chosen);
        // The order holds no copy of the hearing's facts and note, which are recorded only when the judge changed
        // them, so another judge's are not refused or overwritten by a save that left them alone.
        if (request.hearing() != null && request.hearing().caseFacts() != null) {
            hearingRecords.record(order.getPcsCase(), order.getHearing(), request.hearing());
        }
        final boolean resubmitted = order.getState() == RETURNED_TO_JUDGE;
        OrderState to = request.action() == SUBMIT_FOR_REVIEW ? SUBMITTED_FOR_REVIEW : order.getState();
        final OrderStepEntity step;
        try {
            step = orderSteps.append(order, to, judge, OrderStepEntity.builder()
                .orderType(change.orderType())
                .formData(change.formData())
                .docweaveSnapshot(change.docweaveSnapshot()));
        } catch (DataIntegrityViolationException e) {
            // Two pages saving the judge's first draft at the same moment both pass the check above.
            throw change.id() == null ? ExternalRejection.because(ALREADY_DRAFTING) : e;
        }
        if (request.action() != SUBMIT_FOR_REVIEW) {
            return ExternalSubmitResponse.accepted("Order draft saved", "Saved an order as a draft");
        }
        // The task the judge opened the order from: its return, a hearing's, or the application it decides.
        submit.afterCommit(taskCompletionService.complete(request.taskId()));
        messageService.closeAnsweredByOrder(order, decided, judge, orderTasks.currentUserName("Judge"));
        orderTasks.askStaffToReview(caseReference, step, resubmitted);
        return ExternalSubmitResponse.accepted(
            "Order submitted for review", "Submitted an order for caseworker review");
    }

    /** Refuses the order events while making orders is off, as their show condition only hides them in XUI. */
    static void requireMakeOrder(FeatureToggleService featureToggleService) {
        if (!featureToggleService.isEnabled(FeatureFlag.MAKE_ORDER)) {
            throw ExternalRejection.because(NOT_AVAILABLE);
        }
    }

    /** A judge's new draft, at the hearing the change is for, if it is for one. */
    private OrderEntity newDraft(long caseReference, String judge, HearingChange hearing) {
        PcsCaseEntity pcsCase = findCase(caseReference);
        return OrderEntity.builder()
            .pcsCase(pcsCase)
            .hearing(hearing == null ? null : hearingRecords.hearing(pcsCase, hearing.id()))
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

    private static GenAppEntity genApp(PcsCaseEntity pcsCase, UUID genAppId) {
        return pcsCase.findGenApp(genAppId)
            .orElseThrow(() -> ExternalRejection.because("The application is not on this case"));
    }

    private PcsCaseEntity findCase(long caseReference) {
        return pcsCaseRepository.findByCaseReference(caseReference)
            .orElseThrow(() -> new CaseNotFoundException(caseReference));
    }
}
