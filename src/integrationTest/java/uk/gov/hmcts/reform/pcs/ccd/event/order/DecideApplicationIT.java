package uk.gov.hmcts.reform.pcs.ccd.event.order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.ccd.sdk.testing.CcdEventTestSupport;
import uk.gov.hmcts.ccd.sdk.testing.CcdEventTestSupport.Actor;
import uk.gov.hmcts.ccd.sdk.testing.ExternalEvent;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.ApplicationDecision;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.ClaimOutcome;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.GenAppDecision;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.ConfirmOrderReviewRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.ConfirmOrderReviewRequest.Issue;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.ConfirmOrderReviewRequest.IssuedOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest.HearingChange;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest.OrderChange;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderState;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderStart;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderStart.Application;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderStart.ApplicationDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderStart.Order;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.config.IssuedCases;
import uk.gov.hmcts.reform.pcs.config.OrderEventIT;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppState.GEN_APP_ISSUED;
import static uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppState.PENDING_GEN_APP_ISSUED;
import static uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType.ADJOURN;
import static uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType.SOMETHING_ELSE;
import static uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType.SUSPEND;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.ConfirmOrderReviewRequest.Action.ISSUE;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest.Action.SAVE_DRAFT;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest.Action.SUBMIT_FOR_REVIEW;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderSeal.HIGH_COURT;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderType.NO_ORDER_ON_CLAIM;
import static uk.gov.hmcts.reform.pcs.ccd.event.order.ConfirmOrderReview.CONFIRM_ORDER_REVIEW;
import static uk.gov.hmcts.reform.pcs.ccd.event.order.MakeOrder.MAKE_ORDER;

/**
 * A judge decides a general application with an order on it, from the link in a message about it, which names the
 * application for the page to put first, or in any other order on the case.
 */
@DisplayName("Decide an application")
class DecideApplicationIT extends OrderEventIT {

    private static final ClaimOutcome NO_ORDER = ClaimOutcome.of(ClaimOutcome.Kind.NO_ORDER);

    private static final String WHAT_ORDER_WANTED = "The hearing on 14 October is adjourned to the first open date.";
    private static final String DRAFT_ORDER_WORDING = "<ol><li>The hearing is <strong>adjourned</strong>.</li></ol>";
    private static final UUID FORM_ID = UUID.randomUUID();
    private static final UUID WITNESS_STATEMENT_ID = UUID.randomUUID();

    @Autowired
    private CcdEventTestSupport<PCSCase, State> events;
    @Autowired
    private IssuedCases cases;

    private long caseReference;
    private UUID genAppId;
    private Actor judge;
    private Actor caseworker;

    @BeforeEach
    void setUp() {
        caseReference = cases.issue();
        cases.update(caseReference, pcsCase -> {
            PartyEntity defendant = pcsCase.getParties().stream()
                .filter(party -> party.getOrgName() == null).findFirst().orElseThrow();
            GenAppEntity genApp = GenAppEntity.builder().party(defendant).type(ADJOURN).state(GEN_APP_ISSUED)
                .within14Days(VerticalYesNo.YES).otherPartiesAgreed(VerticalYesNo.NO)
                .withoutNotice(VerticalYesNo.NO).whatOrderWanted(WHAT_ORDER_WANTED)
                .draftOrderWording(DRAFT_ORDER_WORDING)
                .applicationSubmittedDate(LocalDateTime.of(2026, 10, 1, 9, 30)).build();
            // The application form is the submission document and filed with the application.
            DocumentEntity form = document(pcsCase, genApp, FORM_ID, "application-form.pdf");
            genApp.setSubmissionDocument(form);
            genApp.getDocuments().add(form);
            genApp.getDocuments().add(document(pcsCase, genApp, WITNESS_STATEMENT_ID, "witness-statement.pdf"));
            pcsCase.addGenApp(genApp);
        });
        genAppId = jdbcTemplate.queryForObject("SELECT ga.id FROM general_application ga JOIN pcs_case pc "
            + "ON pc.id = ga.case_id WHERE pc.case_reference = ?", UUID.class, caseReference);
        judge = events.registerActor("A", "Judge", "caseworker-pcs", "judiciary");
        caseworker = events.registerActor("A", "Caseworker", "caseworker-pcs");
    }

    @Test
    @DisplayName("names the application the judge's task asks them to decide among every open application, oldest "
        + "first, each as the judge reads it")
    void namesTheTasksApplicationAmongEveryOpenApplication() {
        cases.update(caseReference, pcsCase -> {
            PartyEntity defendant = pcsCase.getParties().stream()
                .filter(party -> party.getOrgName() == null).findFirst().orElseThrow();
            pcsCase.addGenApp(GenAppEntity.builder().party(defendant).type(SUSPEND).state(GEN_APP_ISSUED)
                .applicationSubmittedDate(LocalDateTime.of(2026, 10, 9, 14, 0)).build());
            pcsCase.addGenApp(GenAppEntity.builder().party(defendant).type(SOMETHING_ELSE).state(GEN_APP_ISSUED)
                // Only an application to suspend is decided as one, whatever else's details say.
                .somethingElseDetails("Lift the suspension of the warrant")
                .applicationSubmittedDate(LocalDateTime.of(2026, 10, 10, 14, 0))
                .applicationReceivedDate(LocalDate.of(2026, 10, 8)).build());
        });

        OrderStart start = asJudgeDeciding(genAppId).start();

        assertThat(start.taskGenAppId()).isEqualTo(genAppId);
        assertThat(events.external(caseReference, MAKE_ORDER).as(judge).start().taskGenAppId()).isNull();
        assertThat(start.caseContext().applications())
            .extracting(Application::reference, Application::enforcementSuspension, Application::adjournment,
                Application::submittedOn)
            .containsExactly(tuple("GA1", false, true, LocalDate.of(2026, 10, 1)),
                tuple("GA2", true, false, LocalDate.of(2026, 10, 9)),
                // Entered by court staff from paper, it is dated when the court received it.
                tuple("GA3", false, false, LocalDate.of(2026, 10, 8)));
        Application application = start.caseContext().applications().getFirst();
        assertThat(application.id()).isEqualTo(genAppId);
        assertThat(application.type()).isEqualTo("Adjourn the hearing");
        assertThat(application.applicant()).isEqualTo(IssuedCases.DEFENDANT + " (Defendant 1)");
        assertThat(application.within14Days()).isEqualTo(VerticalYesNo.YES);
        assertThat(application.whatOrderWanted()).isEqualTo(WHAT_ORDER_WANTED);
        assertThat(application.draftOrderWording()).isEqualTo(DRAFT_ORDER_WORDING);
        assertThat(start.caseContext().applications().get(1).draftOrderWording()).isNull();
        assertThat(application.documents()).extracting(ApplicationDocument::id, ApplicationDocument::fileName)
            .containsExactly(
                tuple(FORM_ID, "application-form.pdf"),
                tuple(WITNESS_STATEMENT_ID, "witness-statement.pdf"));
    }

    private static final String DECIDED_ELSEWHERE = "Another order has decided application GA1. ";

    @Test
    @DisplayName("refuses issuing a second order deciding the application, and offers it no more once one decides "
        + "it, though listing it for a hearing leaves it to decide")
    void refusesAnotherOrderOnADecidedApplication() {
        Actor otherJudge = events.registerActor("Another", "Judge", "caseworker-pcs", "judiciary");
        var asOtherJudge = events.external(caseReference, MAKE_ORDER).as(otherJudge)
            .withClientContext(Map.of("genAppId", genAppId.toString()));
        // The other judge sends theirs for review alongside the first judge's.
        asOtherJudge.submitExpectingSuccess(decision(SAVE_DRAFT, asOtherJudge.start().order(), "dismiss", null));
        final UUID dismissedId = asOtherJudge
            .submitExpectingSuccess(decision(SUBMIT_FOR_REVIEW, asOtherJudge.start().order(), "dismiss", null))
            .changed("orders", OrderRow.class).id();
        Order listed = judgeDecides(genAppId, "list");
        // Court staff reviewing the order still see the application it decides.
        assertThat(asCaseworkerReviewing(listed.id()).start().caseContext().applications())
            .extracting(Application::id).containsExactly(genAppId);

        issue(listed);
        Order freeform = judgeDecides(genAppId, "freeform");
        issue(freeform);

        Order dismissed = asCaseworkerReviewing(dismissedId).start().order();
        assertThat(asCaseworkerReviewing(dismissedId).submitExpectingRejection(issueRequest(dismissed)).errors())
            .containsExactly(DECIDED_ELSEWHERE + ConfirmOrderReview.RETURN_TO_JUDGE_TO_DECIDE);
        assertThat(jdbcTemplate.queryForList("SELECT decision FROM order_gen_app WHERE gen_app_id = ?",
            String.class, genAppId)).containsExactlyInAnyOrder("LISTED", "DISPOSED");
        assertThat(events.external(caseReference, MAKE_ORDER).as(judge)
            .withClientContext(Map.of("genAppId", genAppId.toString())).startExpectingRejection())
            .containsExactly(MakeOrder.ALREADY_DECIDED);
        // Nor does an order made at a hearing offer it again.
        assertThat(events.external(caseReference, MAKE_ORDER).as(judge).start().caseContext().applications())
            .isEmpty();
    }

    @Test
    @DisplayName("refuses court staff issuing an order they changed to decide an application another order decided")
    void refusesIssuingAnOrderChangedToDecideADecidedApplication() {
        Order undecided = judgeDecides(genAppId, "none");
        // Sent from the task to decide the application, the order completes it.
        verify(taskManagementApi).completeTask(any(), any(), eq("decide-task"), any());
        issue(judgeDecides(genAppId, "freeform"));

        ConfirmOrderReviewRequest changed = new ConfirmOrderReviewRequest(ISSUE, undecided.id(), undecided.version(),
            null, null, new Issue(new IssuedOrder(undecided.orderType(),
                Map.of("app-" + genAppId + "-decision", "dismiss"), WORDING, "the order",
                List.of(new ApplicationDecision(genAppId, GenAppDecision.DISPOSED))),
                List.of(), HIGH_COURT, true, List.of(), true, NO_ORDER));
        assertThat(asCaseworkerReviewing(undecided.id()).submitExpectingRejection(changed).errors())
            .containsExactly(DECIDED_ELSEWHERE + ConfirmOrderReview.RETURN_TO_JUDGE_TO_DECIDE);
    }

    @Test
    @DisplayName("offers an application an order listed for a hearing as listed, and one no order dealt with as not")
    void saysWhetherAnApplicationIsListed() {
        assertThat(asJudgeDeciding(genAppId).start().caseContext().applications()).extracting(Application::listed)
            .containsExactly(false);
        issue(judgeDecides(genAppId, "none"));
        assertThat(asJudgeDeciding(genAppId).start().caseContext().applications()).extracting(Application::listed)
            .containsExactly(false);

        issue(judgeDecides(genAppId, "list"));

        assertThat(asJudgeDeciding(genAppId).start().caseContext().applications()).extracting(Application::listed)
            .containsExactly(true);
    }

    @Test
    @DisplayName("decides the task's application, with the case's other open applications, at a hearing the judge "
        + "chooses, and issues the order as made that day")
    void decidesTheApplicationAtAChosenHearing() {
        int hearing = cases.hearing(caseReference, LocalDateTime.of(2026, 10, 14, 10, 0), false);
        var atHearing = asJudgeDeciding(genAppId);

        OrderStart start = atHearing.start();
        assertThat(start.taskGenAppId()).isEqualTo(genAppId);
        assertThat(start.hearing()).as("box work, until the judge chooses the hearing").isNull();
        assertThat(start.otherHearings()).singleElement().satisfies(choice -> {
            assertThat(choice.id()).isEqualTo(hearing);
            assertThat(choice.caseFacts()).as("the hearing's facts to record").isNotNull();
        });

        Order order = start.order();
        UUID orderId = atHearing.submitExpectingSuccess(new MakeOrderRequest(SUBMIT_FOR_REVIEW,
                new OrderChange(order.id(), order.version(), NO_ORDER_ON_CLAIM,
                    Map.of("app-" + genAppId + "-decision", "freeform"), WORDING, decided("freeform")), "decide-task",
                new HearingChange(hearing, null, null, null)))
            .changed("orders", OrderRow.class).id();
        verify(taskManagementApi).completeTask(any(), any(), eq("decide-task"), any());

        Order reviewed = asCaseworkerReviewing(orderId).start().order();
        assertThat(asCaseworkerReviewing(orderId).start().hearing().id()).isEqualTo(hearing);
        issue(reviewed);
        assertThat(jdbcTemplate.queryForObject("SELECT made_on FROM orders WHERE id = ?", LocalDate.class, orderId))
            .isEqualTo(LocalDate.of(2026, 10, 14));
        assertThat(jdbcTemplate.queryForList("SELECT decision FROM order_gen_app WHERE gen_app_id = ?",
            String.class, genAppId)).containsExactly("DISPOSED");
    }

    @Test
    @DisplayName("reopens the judge's draft deciding the application, saved at a hearing, from the application's link "
        + "and from the case, as it was saved, and keeps it one draft as they move it")
    void reopensADraftSavedAtAHearingFromTheApplicationsLink() {
        final int hearing = cases.hearing(caseReference, LocalDateTime.of(2026, 10, 14, 10, 0), false);
        Map<String, Object> answers = Map.of("app-" + genAppId + "-decision", "freeform",
            "order-notice", "without-notice", "without-notice-by-day", "21", "without-notice-by-month", "10",
            "without-notice-by-year", "2026");
        // The judge follows the application's link, chooses the hearing on the page and saves.
        assertThat(asJudgeDeciding(genAppId).start().hearing()).isNull();
        var choosingHearing = asJudgeDeciding(genAppId);
        UUID draftId = choosingHearing.submitExpectingSuccess(new MakeOrderRequest(SAVE_DRAFT,
                new OrderChange(null, 0, NO_ORDER_ON_CLAIM, answers, WORDING, decided("freeform")), "decide-task",
                new HearingChange(hearing, null, null, null)))
            .changed("orders", OrderRow.class).id();

        // The application's link, the case's own, and the draft's with the application all open it as saved.
        OrderStart fromApplication = asJudgeDeciding(genAppId).start();
        OrderStart fromCase = events.external(caseReference, MAKE_ORDER).as(judge).start();
        OrderStart byId = events.external(caseReference, MAKE_ORDER).as(judge).withClientContext(
            Map.of("orderId", draftId.toString(), "genAppId", genAppId.toString())).start();
        assertThat(List.of(fromApplication, fromCase, byId)).allSatisfy(reopened -> {
            assertThat(reopened.order().id()).isEqualTo(draftId);
            assertThat(reopened.order().state()).isEqualTo(OrderState.DRAFT);
            assertThat(reopened.order().formData()).isEqualTo(answers);
            assertThat(reopened.order().docweaveSnapshot()).isEqualTo(WORDING);
            assertThat(reopened.hearing().id()).isEqualTo(hearing);
        });
        assertThat(fromApplication.taskGenAppId()).isEqualTo(genAppId);
        assertThat(byId.taskGenAppId()).as("the application the order must decide").isEqualTo(genAppId);
        assertThat(fromCase.taskGenAppId()).isNull();

        // Moved to no hearing and back from the application's link, it is the same one draft.
        asJudgeDeciding(genAppId).submitExpectingSuccess(new MakeOrderRequest(SAVE_DRAFT, new OrderChange(draftId,
            fromApplication.order().version(), NO_ORDER_ON_CLAIM, answers, WORDING, decided("freeform")),
            "decide-task"));
        assertThat(asJudgeDeciding(genAppId).start().hearing()).isNull();
        choosingHearing.submitExpectingSuccess(new MakeOrderRequest(SAVE_DRAFT, new OrderChange(draftId,
            asJudgeDeciding(genAppId).start().order().version(), NO_ORDER_ON_CLAIM, answers, WORDING,
            decided("freeform")), "decide-task", new HearingChange(hearing, null, null, null)));
        assertThat(jdbcTemplate.queryForList("SELECT id, state, hearing_id FROM orders WHERE case_id = "
            + "(SELECT id FROM pcs_case WHERE case_reference = ?)", caseReference))
            .extracting(row -> row.get("id"), row -> row.get("state"), row -> row.get("hearing_id"))
            .containsExactly(tuple(draftId, "DRAFT", hearing));
        assertThat(asJudgeDeciding(genAppId).start().order().formData()).isEqualTo(answers);
        // Saving and moving a draft completes no task; sending it does.
        verifyNoInteractions(taskManagementApi);
        asJudgeDeciding(genAppId).submitExpectingSuccess(new MakeOrderRequest(SUBMIT_FOR_REVIEW,
            new OrderChange(draftId, asJudgeDeciding(genAppId).start().order().version(), NO_ORDER_ON_CLAIM, answers,
                WORDING, decided("freeform")), "decide-task", new HearingChange(hearing, null, null, null)));
        verify(taskManagementApi).completeTask(any(), any(), eq("decide-task"), any());
        assertThat(asJudgeDeciding(genAppId).start().order()).isEqualTo(Order.NONE);
    }

    @Test
    @DisplayName("reopens the judge's order from its link after the application it names has closed, and starts no "
        + "new order on the closed application")
    void reopensAnOrderAfterItsApplicationCloses() {
        var asJudge = asJudgeDeciding(genAppId);
        UUID draftId = asJudge.submitExpectingSuccess(decision(SAVE_DRAFT, asJudge.start().order(), "freeform", null))
            .changed("orders", OrderRow.class).id();
        cases.update(caseReference, pcsCase ->
            pcsCase.findGenApp(genAppId).orElseThrow().setState(PENDING_GEN_APP_ISSUED));

        OrderStart reopened = events.external(caseReference, MAKE_ORDER).as(judge).withClientContext(
            Map.of("orderId", draftId.toString(), "genAppId", genAppId.toString())).start();
        assertThat(reopened.order().id()).isEqualTo(draftId);
        assertThat(reopened.taskGenAppId()).isNull();
        // The judge can keep their draft, but not send court staff a decision they could not issue.
        assertThat(asJudge.submitExpectingRejection(decision(SUBMIT_FOR_REVIEW, reopened.order(), "freeform", null))
            .errors()).containsExactly("Application GA1 is no longer open. " + MakeOrder.RELOAD_TO_DECIDE);

        Actor otherJudge = events.registerActor("Other", "Judge", "caseworker-pcs", "judiciary");
        assertThat(events.external(caseReference, MAKE_ORDER).as(otherJudge)
            .withClientContext(Map.of("genAppId", genAppId.toString())).startExpectingRejection())
            .containsExactly(MakeOrder.NOT_OPEN);
    }

    private void issue(Order order) {
        asCaseworkerReviewing(order.id()).submitExpectingSuccess(issueRequest(order));
    }

    private ConfirmOrderReviewRequest issueRequest(Order order) {
        return new ConfirmOrderReviewRequest(ISSUE, order.id(), order.version(), null, null, new Issue(
            new IssuedOrder(order.orderType(), order.formData(), WORDING, "the order",
                decided(String.valueOf(order.formData().get("app-" + genAppId + "-decision")))),
            List.of(), HIGH_COURT, true, List.of(), true, NO_ORDER));
    }

    /** What the frontend sends for the make order form's decision on the application. */
    private List<ApplicationDecision> decided(String answer) {
        GenAppDecision decision = switch (answer) {
            case "none" -> null;
            case "list" -> GenAppDecision.LISTED;
            default -> GenAppDecision.DISPOSED;
        };
        return decision == null ? List.of() : List.of(new ApplicationDecision(genAppId, decision));
    }

    /** The judge following their task to decide the application, which names it. */
    private ExternalEvent<OrderStart, MakeOrderRequest> asJudgeDeciding(UUID genAppId) {
        return events.external(caseReference, MAKE_ORDER).as(judge)
            .withClientContext(Map.of("genAppId", genAppId.toString()));
    }

    /** The judge decides the application and sends their order for review; returns it as court staff see it. */
    private Order judgeDecides(UUID genAppId, String decision) {
        var asJudge = asJudgeDeciding(genAppId);
        asJudge.submitExpectingSuccess(decision(SAVE_DRAFT, asJudge.start().order(), decision, null));
        UUID orderId = asJudge
            .submitExpectingSuccess(decision(SUBMIT_FOR_REVIEW, asJudge.start().order(), decision, "decide-task"))
            .changed("orders", OrderRow.class).id();
        return asCaseworkerReviewing(orderId).start().order();
    }

    private ExternalEvent<OrderStart, ConfirmOrderReviewRequest> asCaseworkerReviewing(UUID orderId) {
        return events.external(caseReference, CONFIRM_ORDER_REVIEW).as(caseworker)
            .withClientContext(Map.of("orderId", orderId.toString()));
    }

    private MakeOrderRequest decision(MakeOrderRequest.Action action, Order order, String decision,
                                      String taskId) {
        return new MakeOrderRequest(action, new OrderChange(order.id(), order.version(), NO_ORDER_ON_CLAIM,
            Map.of("app-" + genAppId + "-decision", decision), WORDING, decided(decision)), taskId);
    }

    private static DocumentEntity document(PcsCaseEntity pcsCase, GenAppEntity genApp, UUID documentId,
                                           String fileName) {
        return DocumentEntity.builder().pcsCase(pcsCase).generalApplication(genApp).documentId(documentId)
            .url("http://dm-store/documents/" + documentId).fileName(fileName).build();
    }

}
