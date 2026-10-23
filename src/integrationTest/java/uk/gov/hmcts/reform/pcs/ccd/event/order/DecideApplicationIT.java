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
import uk.gov.hmcts.reform.pcs.ccd.domain.order.ConfirmOrderReviewRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.ConfirmOrderReviewRequest.Issue;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.ConfirmOrderReviewRequest.IssuedOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest.OrderChange;
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
import static uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppState.GEN_APP_ISSUED;
import static uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType.ADJOURN;
import static uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType.SOMETHING_ELSE;
import static uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType.SUSPEND;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.ConfirmOrderReviewRequest.Action.ISSUE;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.ConfirmOrderReviewRequest.Action.RETURN_TO_JUDGE;
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

    private static final String WHAT_ORDER_WANTED = "The hearing on 14 October is adjourned to the first open date.";
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
        judge = events.registerActor("A", "Judge", "caseworker-pcs");
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
            .extracting(Application::reference, Application::warrantSuspension, Application::submittedOn)
            .containsExactly(tuple("GA1", false, LocalDate.of(2026, 10, 1)),
                tuple("GA2", true, LocalDate.of(2026, 10, 9)),
                // Entered by court staff from paper, it is dated when the court received it.
                tuple("GA3", false, LocalDate.of(2026, 10, 8)));
        Application application = start.caseContext().applications().getFirst();
        assertThat(application.id()).isEqualTo(genAppId);
        assertThat(application.type()).isEqualTo("Adjourn the hearing");
        assertThat(application.applicant()).isEqualTo(IssuedCases.DEFENDANT + " (Defendant 1)");
        assertThat(application.within14Days()).isEqualTo(VerticalYesNo.YES);
        assertThat(application.whatOrderWanted()).isEqualTo(WHAT_ORDER_WANTED);
        assertThat(application.documents()).extracting(ApplicationDocument::id, ApplicationDocument::fileName)
            .containsExactly(
                tuple(FORM_ID, "application-form.pdf"),
                tuple(WITNESS_STATEMENT_ID, "witness-statement.pdf"));
    }

    @Test
    @DisplayName("refuses another order on the application while one is under way, and once one decides it, "
        + "though listing it for a hearing leaves it to decide")
    void refusesAnotherOrderOnADecidedApplication() {
        Actor otherJudge = events.registerActor("Another", "Judge", "caseworker-pcs");
        var asOtherJudge = events.external(caseReference, MAKE_ORDER).as(otherJudge)
            .withClientContext(Map.of("genAppId", genAppId.toString()));
        // The other judge started a draft before the first sent theirs for review.
        asOtherJudge.submitExpectingSuccess(decision(SAVE_DRAFT, asOtherJudge.start().order(), "dismiss", null));
        Order listed = judgeDecides(genAppId, "list");
        // Court staff reviewing the order still see the application it decides.
        assertThat(asCaseworkerReviewing(listed.id()).start().caseContext().applications())
            .extracting(Application::id).containsExactly(genAppId);

        assertThat(asOtherJudge.submitExpectingRejection(decision(SUBMIT_FOR_REVIEW, asOtherJudge.start().order(),
            "dismiss", null)).errors()).containsExactly(MakeOrder.ALREADY_DECIDED);

        issue(listed);
        Order granted = judgeDecides(genAppId, "grant");
        issue(granted);
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
        issue(judgeDecides(genAppId, "grant"));

        assertThat(asCaseworkerReviewing(undecided.id()).submitExpectingRejection(new ConfirmOrderReviewRequest(
            ISSUE, undecided.id(), undecided.version(), null, null, new Issue(new IssuedOrder(
                undecided.orderType(), Map.of("app-" + genAppId + "-decision", "dismiss"), WORDING, "the order"),
                List.of(), HIGH_COURT, true, List.of()))).errors()).containsExactly(MakeOrder.ALREADY_DECIDED);
    }

    @Test
    @DisplayName("keeps the order's own thread open when the judge sends it back after court staff's query")
    void keepsTheOrdersThreadOpenWhenItIsResubmitted() {
        Order submitted = judgeDecides(genAppId, "grant");
        asCaseworkerReviewing(submitted.id()).submitExpectingSuccess(new ConfirmOrderReviewRequest(RETURN_TO_JUDGE,
            submitted.id(), submitted.version(), null, "Which date?", null));
        var asAuthor = events.external(caseReference, MAKE_ORDER).as(judge)
            .withClientContext(Map.of("orderId", submitted.id().toString()));
        asAuthor.submitExpectingSuccess(decision(SUBMIT_FOR_REVIEW, asAuthor.start().order(), "grant", null));

        assertThat(jdbcTemplate.queryForList("SELECT DISTINCT message.closed_at IS NULL AS open "
            + "FROM order_steps step JOIN case_message_entry entry ON entry.id = step.message_entry_id "
            + "JOIN case_message message ON message.id = entry.message_id WHERE step.order_id = ?",
            submitted.id())).extracting(row -> row.get("open")).containsExactly(true);
    }

    private void issue(Order order) {
        asCaseworkerReviewing(order.id()).submitExpectingSuccess(new ConfirmOrderReviewRequest(ISSUE, order.id(),
            order.version(), null, null, new Issue(new IssuedOrder(order.orderType(), order.formData(), WORDING,
                "the order"), List.of(), HIGH_COURT, true, List.of())));
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
            Map.of("app-" + genAppId + "-decision", decision), WORDING), taskId);
    }

    private static DocumentEntity document(PcsCaseEntity pcsCase, GenAppEntity genApp, UUID documentId,
                                           String fileName) {
        return DocumentEntity.builder().pcsCase(pcsCase).generalApplication(genApp).documentId(documentId)
            .url("http://dm-store/documents/" + documentId).fileName(fileName).build();
    }

}
