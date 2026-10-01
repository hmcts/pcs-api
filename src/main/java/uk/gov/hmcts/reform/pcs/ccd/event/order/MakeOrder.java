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
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.DraftOrderState;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest.OrderChange;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderStart;
import uk.gov.hmcts.reform.pcs.ccd.entity.DraftOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.OrderReviewEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.DraftOrderRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.OrderReviewRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.util.ClientContext;
import uk.gov.hmcts.reform.pcs.ccd.util.ClientContextRetriever;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest.Action.SUBMIT_FOR_REVIEW;

/**
 * A judge makes an order through pcs-frontend's make order journey. Starting the event sends them
 * their working order and the facts of the case; each submission saves or submits for review their
 * draft. A judge has one working draft per case, and a change is made from the version they last
 * saw; a change with no id starts their draft. An order a caseworker returned to the judge is theirs
 * to change again: they choose it on the case's draft orders tab, see the caseworker's query, and
 * submit it for review again.
 */
@Component
@AllArgsConstructor
public class MakeOrder implements CCDConfig<PCSCase, State, UserRole> {

    public static final ExternalEventId<OrderStart, MakeOrderRequest> MAKE_ORDER =
        ExternalEventId.of("ext:makeOrder", OrderStart.class, MakeOrderRequest.class);

    /** The states that leave an order its author's to change. */
    private static final Set<DraftOrderState> CHANGEABLE =
        Set.of(DraftOrderState.DRAFT, DraftOrderState.RETURNED_TO_JUDGE);

    private final DraftOrderRepository draftOrderRepository;
    private final OrderReviewRepository orderReviewRepository;
    private final PcsCaseRepository pcsCaseRepository;
    private final OrderCaseContext orderCaseContext;
    private final ClientContextRetriever clientContextRetriever;

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
            .onStart(this::start);
    }

    private ExternalStartResponse<OrderStart> start(ExternalStartRequest start) {
        long caseReference = start.caseReference();
        UUID judge = UUID.fromString(start.user().id());
        OrderStart.Order workingOrder = chosenOrderId()
            .map(orderId -> chosenOrder(orderId, caseReference, judge))
            .orElseGet(() -> findDraft(caseReference, judge)
                .map(OrderStart.Order::of)
                .orElseGet(() -> new OrderStart.Order(null, DraftOrderState.DRAFT, 0, null, Map.of(), null, null)));
        return ExternalStartResponse.started(
            new OrderStart(workingOrder, orderCaseContext.of(findCase(caseReference))));
    }

    /**
     * The order the judge chose on the case's draft orders tab, which the frontend names in the
     * Client-Context header: one a caseworker returned to them, with the query it came back with.
     */
    private OrderStart.Order chosenOrder(UUID orderId, long caseReference, UUID judge) {
        DraftOrderEntity order = draftOrderRepository
            .findByIdAndPcsCaseCaseReferenceAndAuthorIdamUserIdAndStateIn(orderId, caseReference, judge, CHANGEABLE)
            .orElseThrow(() -> ExternalRejection.because("The order is no longer waiting for you to change it"));
        String query = order.getState() == DraftOrderState.RETURNED_TO_JUDGE
            ? orderReviewRepository.findFirstByDraftOrderIdOrderByReviewedVersionDesc(order.getId())
                .map(OrderReviewEntity::getQueryToJudge)
                .orElse(null)
            : null;
        return OrderStart.Order.of(order, query);
    }

    private Optional<UUID> chosenOrderId() {
        ClientContext clientContext = clientContextRetriever.getClientContext();
        String orderId = clientContext == null ? null : clientContext.getOrderId();
        try {
            return Optional.ofNullable(orderId).map(UUID::fromString);
        } catch (IllegalArgumentException e) {
            throw ExternalRejection.because("The order is no longer waiting for you to change it");
        }
    }

    private ExternalSubmitResponse<State> submit(ExternalSubmitRequest<MakeOrderRequest> submit) {
        MakeOrderRequest request = submit.payload();
        OrderChange change = request.order();
        long caseReference = submit.caseReference();
        UUID judge = UUID.fromString(submit.user().id());
        DraftOrderEntity draft = change.id() == null
            ? newDraft(caseReference, judge)
            : workingDraft(caseReference, judge, change);
        draft.setOrderType(change.orderType());
        draft.setFormData(change.formData());
        draft.setDocweaveSnapshot(change.docweaveSnapshot());
        if (request.action() == SUBMIT_FOR_REVIEW) {
            draft.setState(DraftOrderState.SUBMITTED_FOR_REVIEW);
        }
        draftOrderRepository.saveAndFlush(draft);
        return request.action() == SUBMIT_FOR_REVIEW
            ? ExternalSubmitResponse.accepted("Order submitted for review", "Submitted an order for caseworker review")
            : ExternalSubmitResponse.accepted("Order draft saved", "Saved an order as a draft");
    }

    /** A judge has one draft per case, which the database also enforces. */
    private DraftOrderEntity newDraft(long caseReference, UUID judge) {
        if (findDraft(caseReference, judge).isPresent()) {
            throw ExternalRejection.because("You already have an order draft for this case");
        }
        return DraftOrderEntity.builder()
            .pcsCase(findCase(caseReference))
            .authorIdamUserId(judge)
            .state(DraftOrderState.DRAFT)
            .build();
    }

    /** The judge's own draft, or order returned to them, this change is for, at the version they last saw. */
    private DraftOrderEntity workingDraft(long caseReference, UUID judge, OrderChange change) {
        DraftOrderEntity draft = draftOrderRepository
            .findByIdAndPcsCaseCaseReferenceAndAuthorIdamUserIdAndStateIn(
                change.id(), caseReference, judge, CHANGEABLE)
            .orElseThrow(() -> ExternalRejection.because("The order draft does not exist for this case"));
        if (draft.getVersion() != change.version()) {
            throw ExternalRejection.because(
                "The order draft has been updated by another user. Reload it and try again");
        }
        return draft;
    }

    private Optional<DraftOrderEntity> findDraft(long caseReference, UUID judge) {
        return draftOrderRepository.findByPcsCaseCaseReferenceAndAuthorIdamUserIdAndState(
            caseReference, judge, DraftOrderState.DRAFT);
    }

    private PcsCaseEntity findCase(long caseReference) {
        return pcsCaseRepository.findByCaseReference(caseReference)
            .orElseThrow(() -> new CaseNotFoundException(caseReference));
    }
}
