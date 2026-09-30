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
import uk.gov.hmcts.reform.pcs.ccd.domain.ReviewDate;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.ConfirmOrderReviewRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.ConfirmOrderReviewRequest.Issue;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.ConfirmOrderReviewRequest.ReviewDateEntry;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.DraftOrderState;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderStart;
import uk.gov.hmcts.reform.pcs.ccd.entity.DraftOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.OrderReviewEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.DraftOrderRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.OrderReviewRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseReviewDateService;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CaseworkerRoles.CASEWORKER_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.ConfirmOrderReviewRequest.Action.RETURN_TO_JUDGE;

/**
 * A caseworker reviews an order a judge submitted, through pcs-frontend's confirm order review
 * journey. Starting the event sends them the order awaiting review and the case; submitting either
 * returns the order to the judge with a query, or records how it is to be issued and adds any review
 * dates to the case. Generating, sealing and serving the issued order are not built yet.
 */
@Component
@AllArgsConstructor
public class ConfirmOrderReview implements CCDConfig<PCSCase, State, UserRole> {

    public static final ExternalEventId<OrderStart, ConfirmOrderReviewRequest> CONFIRM_ORDER_REVIEW =
        ExternalEventId.of("ext:confirmOrderReview", OrderStart.class, ConfirmOrderReviewRequest.class);

    static final int MAX_QUERY_LENGTH = 30_000;
    static final int MAX_REVIEW_DATES = 10;
    static final int MAX_REVIEW_DESCRIPTION_LENGTH = 500;

    private final DraftOrderRepository draftOrderRepository;
    private final OrderReviewRepository orderReviewRepository;
    private final OrderCaseContext orderCaseContext;
    private final CaseReviewDateService caseReviewDateService;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .externalEvent(CONFIRM_ORDER_REVIEW, this::submit)
            .forStates(
                State.CASE_ISSUED,
                State.CASE_PROGRESSION,
                State.JUDICIAL_REFERRAL,
                State.HEARING_READINESS,
                State.PREPARE_FOR_HEARING_CONDUCT_HEARING,
                State.DECISION_OUTCOME
            )
            .name("Confirm order review")
            .showCondition(ShowConditions.featureFlagsEnabled(FeatureFlag.MAKE_ORDER))
            .grant(Permission.CRUD, CASEWORKER_ROLES)
            .onStart(this::start);
    }

    /**
     * Until the caseworker's work allocation task names the order it is for, the caseworker reviews
     * the first order awaiting review on the case.
     */
    private ExternalStartResponse<OrderStart> start(ExternalStartRequest start) {
        long caseReference = start.caseReference();
        return draftOrderRepository
            .findByPcsCaseCaseReferenceAndState(caseReference, DraftOrderState.SUBMITTED_FOR_REVIEW)
            .stream()
            .findFirst()
            .map(order -> ExternalStartResponse.started(new OrderStart(
                OrderStart.Order.of(order), orderCaseContext.of(order.getPcsCase()))))
            .orElseThrow(() -> ExternalRejection.because("There is no order waiting for review on this case"));
    }

    private ExternalSubmitResponse<State> submit(ExternalSubmitRequest<ConfirmOrderReviewRequest> submit) {
        ConfirmOrderReviewRequest request = submit.payload();
        long caseReference = submit.caseReference();
        DraftOrderEntity order = orderAwaitingReview(caseReference, request);
        OrderReviewEntity.OrderReviewEntityBuilder review = OrderReviewEntity.builder()
            .draftOrder(order)
            .reviewerIdamUserId(UUID.fromString(submit.user().id()));

        if (request.action() == RETURN_TO_JUDGE) {
            String query = validQuery(request.queryToJudge());
            order.setState(DraftOrderState.RETURNED_TO_JUDGE);
            review.outcome(DraftOrderState.RETURNED_TO_JUDGE).queryToJudge(query);
            save(order, review.build());
            return ExternalSubmitResponse.accepted("Order returned to judge", "Returned an order to the judge");
        }

        Issue issue = request.issue();
        validateIssue(issue, partyIds(order));
        order.setState(DraftOrderState.ISSUED);
        review.outcome(DraftOrderState.ISSUED)
            .nextStepsComplete(issue.nextStepsComplete())
            .finalOrder(issue.finalOrder())
            .serveAllParties(issue.serveAllParties())
            .partiesServed(issue.serveAllParties() ? null : issue.partiesToServe())
            .seal(issue.seal());
        save(order, review.build());
        if (!issue.reviewDates().isEmpty()) {
            caseReviewDateService.addCaseReviewDates(caseReference, issue.reviewDates().stream()
                .map(ConfirmOrderReview::toReviewDate)
                .toList());
        }
        return ExternalSubmitResponse.accepted("Order issued", "Reviewed and issued an order");
    }

    /** The order the caseworker reviewed, still awaiting review and at the version they saw. */
    private DraftOrderEntity orderAwaitingReview(long caseReference, ConfirmOrderReviewRequest request) {
        if (request.orderId() == null) {
            throw ExternalRejection.because("The order does not exist for this case");
        }
        DraftOrderEntity order = draftOrderRepository
            .findByIdAndPcsCaseCaseReferenceAndState(request.orderId(), caseReference,
                DraftOrderState.SUBMITTED_FOR_REVIEW)
            .orElseThrow(() -> ExternalRejection.because("The order is no longer waiting for review"));
        if (order.getVersion() != request.version()) {
            throw ExternalRejection.because("The order has been updated by another user. Reload it and try again");
        }
        return order;
    }

    private void save(DraftOrderEntity order, OrderReviewEntity review) {
        draftOrderRepository.saveAndFlush(order);
        orderReviewRepository.saveAndFlush(review);
    }

    private static String validQuery(String query) {
        if (query == null || query.isBlank()) {
            throw ExternalRejection.because("Enter a query for the judge");
        }
        if (query.length() > MAX_QUERY_LENGTH) {
            throw ExternalRejection.because("The query for the judge must be 30,000 characters or less");
        }
        return query;
    }

    private static void validateIssue(Issue issue, Set<String> partyIds) {
        if (issue == null) {
            throw ExternalRejection.because("Answer how the order is to be issued");
        }
        List<String> errors = new ArrayList<>();
        if (issue.reviewDates().size() > MAX_REVIEW_DATES) {
            errors.add("You can add up to 10 review dates");
        }
        for (ReviewDateEntry reviewDate : issue.reviewDates()) {
            if (reviewDate.date() == null || reviewDate.reason() == null
                || reviewDate.description() == null || reviewDate.description().isBlank()) {
                errors.add("Each review date needs a date, a reason and a description");
                break;
            }
            if (reviewDate.description().length() > MAX_REVIEW_DESCRIPTION_LENGTH) {
                errors.add("The description of a review must be 500 characters or less");
                break;
            }
        }
        if (!issue.serveAllParties() && issue.partiesToServe().isEmpty()) {
            errors.add("Select who to serve the order on");
        } else if (!partyIds.containsAll(issue.partiesToServe())) {
            errors.add("The order can only be served on parties to the case");
        }
        if (issue.seal() == null) {
            errors.add("Select which seal the order should have");
        }
        if (!errors.isEmpty()) {
            throw ExternalRejection.because(errors.toArray(String[]::new));
        }
    }

    /** The ids of the parties the caseworker was shown, whom the order may be served on. */
    private Set<String> partyIds(DraftOrderEntity order) {
        OrderStart.CaseContext caseContext = orderCaseContext.of(order.getPcsCase());
        return Stream.concat(caseContext.claimants().stream(), caseContext.defendants().stream())
            .map(OrderStart.Party::id)
            .collect(Collectors.toSet());
    }

    private static ReviewDate toReviewDate(ReviewDateEntry entry) {
        return ReviewDate.builder()
            .date(entry.date())
            .reason(entry.reason())
            .description(entry.description())
            .build();
    }
}
