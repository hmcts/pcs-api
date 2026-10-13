package uk.gov.hmcts.reform.pcs.ccd.event.order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.ccd.sdk.testing.CcdEventTestSupport;
import uk.gov.hmcts.ccd.sdk.testing.CcdEventTestSupport.Actor;
import uk.gov.hmcts.ccd.sdk.testing.ExternalEvent;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.camunda.TaskType;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseNote;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppState.GEN_APP_ISSUED;
import static uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType.ADJOURN;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.ConfirmOrderReviewRequest.Action.ISSUE;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest.Action.SAVE_DRAFT;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest.Action.SUBMIT_FOR_REVIEW;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderSeal.COUNTY_COURT;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderType.APPLICATION_DECISION;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.referGenAppToJudge;
import static uk.gov.hmcts.reform.pcs.ccd.event.order.ConfirmOrderReview.CONFIRM_ORDER_REVIEW;
import static uk.gov.hmcts.reform.pcs.ccd.event.order.MakeOrder.MAKE_ORDER;

/**
 * Court staff refer a general application to a judge, who decides it with an order on it: the order
 * starts from the application, and issuing it records what it decided.
 */
@DisplayName("Decide an application")
class DecideApplicationIT extends OrderEventIT {

    private static final String WHAT_ORDER_WANTED = "The hearing on 14 October is adjourned to the first open date.";
    private static final String NOTE = "The hearing is next week";
    private static final UUID FORM_ID = UUID.randomUUID();
    private static final UUID WITNESS_STATEMENT_ID = UUID.randomUUID();

    @Autowired
    private CcdEventTestSupport<PCSCase, State> events;
    @Autowired
    private IssuedCases cases;

    private long caseReference;
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
        judge = events.registerActor("A", "Judge", "caseworker-pcs");
        caseworker = events.registerActor("A", "Caseworker", "caseworker-pcs");
    }

    @Test
    @DisplayName("refers an open application to a judge with a note, giving the judges a task to decide it")
    void refersTheApplicationToAJudge() {
        UUID genAppId = refer();

        verify(camundaService).createTask(eq(caseReference), eq(TaskType.DECIDE_GEN_APP), contains(
            "ext:makeOrder?expected_sub=${[EXPECTED_SUB]}&amp;genAppId=" + genAppId + "&amp;taskId=${[id]}"));
        verify(camundaService).createTask(eq(caseReference), eq(TaskType.DECIDE_GEN_APP), contains(NOTE));
        assertThat(events.view(caseReference, caseworker).getCaseNotes()).extracting(ListValue::getValue)
            .extracting(CaseNote::getNote).containsExactly(NOTE);
    }

    @Test
    @DisplayName("shows the judge the application they are deciding, what was asked for and the referral note")
    void showsTheJudgeTheApplication() {
        OrderStart start = asJudgeDeciding(refer()).start();

        Application application = start.caseContext().application();
        assertThat(application.reference()).isEqualTo("GA1");
        assertThat(application.type()).isEqualTo("Adjourn the hearing");
        assertThat(application.applicant()).isEqualTo(IssuedCases.DEFENDANT + " (Defendant 1)");
        assertThat(application.submittedOn()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(application.within14Days()).isEqualTo(VerticalYesNo.YES);
        assertThat(application.whatOrderWanted()).isEqualTo(WHAT_ORDER_WANTED);
        assertThat(application.referralNote()).isEqualTo(NOTE);
        assertThat(application.referredOn()).isNotNull();
        assertThat(application.documents()).extracting(ApplicationDocument::id, ApplicationDocument::fileName)
            .containsExactly(
                tuple(FORM_ID, "application-form.pdf"),
                tuple(WITNESS_STATEMENT_ID, "witness-statement.pdf"));
    }

    @Test
    @DisplayName("keeps the judge's order on the application apart from their draft for the case")
    void keepsTheApplicationDraftApartFromTheCaseDraft() {
        UUID genAppId = refer();
        events.external(caseReference, MAKE_ORDER).as(judge).submitExpectingSuccess(startDraft("case draft"));
        var asJudge = asJudgeDeciding(genAppId);
        asJudge.submitExpectingSuccess(decision(SAVE_DRAFT, Order.none(genAppId), "grant", null));

        assertThat(asJudge.start().order().formData()).containsEntry("application-decision", "grant");
        assertThat(events.external(caseReference, MAKE_ORDER).as(judge).start().order().formData())
            .containsEntry("notes", "case draft");
    }

    @Test
    @DisplayName("completes the judge's task when they send their order on the application for review")
    void completesTheTaskOnSubmission() {
        Order submitted = judgeDecides(refer(), "grant");

        assertThat(submitted.genAppId()).isNotNull();
        verify(taskManagementApi).completeTask(any(), any(), eq("decide-task"), any());
        verify(camundaService).createTask(eq(caseReference), eq(TaskType.CONFIRM_ORDER_REVIEW),
            contains("has submitted an order for review"));
    }

    @Test
    @DisplayName("records the decision when court staff issue the order, which closes a granted application")
    void recordsTheDecisionOnIssue() {
        UUID genAppId = refer();
        Order submitted = judgeDecides(genAppId, "grant");
        var asCaseworker = asCaseworkerReviewing(submitted.id());
        assertThat(asCaseworker.start().caseContext().application().reference()).isEqualTo("GA1");

        asCaseworker.submitExpectingSuccess(issue(submitted));

        assertThat(outcome(genAppId)).isEqualTo("GRANTED");
        assertThat(asJudgeDeciding(genAppId).startExpectingRejection())
            .containsExactly("The application has already been decided");
        assertThat(events.external(caseReference, MAKE_ORDER).as(judge).start().caseContext().openApplication())
            .isFalse();
        assertThat(referrable()).isEmpty();
    }

    @Test
    @DisplayName("leaves an application listed for a hearing open, to refer and decide again")
    void leavesAListedApplicationOpen() {
        UUID genAppId = refer();
        Order submitted = judgeDecides(genAppId, "list");
        asCaseworkerReviewing(submitted.id()).submitExpectingSuccess(issue(submitted));

        assertThat(outcome(genAppId)).isEqualTo("LISTED");
        assertThat(referrable()).extracting(DynamicListElement::getCode).containsExactly(genAppId);
        assertThat(asJudgeDeciding(genAppId).start().order().id()).isNull();
    }

    /** Court staff refer the case's application to a judge, with a note; the list names it in the task. */
    private UUID refer() {
        List<DynamicListElement> applications = referrable();
        assertThat(applications).extracting(DynamicListElement::getLabel)
            .containsExactly("GA1: Adjourn the hearing, by " + IssuedCases.DEFENDANT);
        events.start(caseReference, referGenAppToJudge.name()).as(caseworker).startExpectingSuccess()
            .edit(pcsCase -> {
                pcsCase.setReferGenAppList(DynamicList.builder()
                    .value(applications.getFirst()).listItems(applications).build());
                pcsCase.setReferGenAppNote(NOTE);
            })
            .as(caseworker).submitExpectingSuccess();
        return applications.getFirst().getCode();
    }

    /** The applications court staff can refer to a judge. */
    private List<DynamicListElement> referrable() {
        return events.start(caseReference, referGenAppToJudge.name()).as(caseworker).startExpectingSuccess()
            .caseData().getReferGenAppList().getListItems();
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
            .changed("orders", Order.class).id();
        return asCaseworkerReviewing(orderId).start().order();
    }

    private ExternalEvent<OrderStart, ConfirmOrderReviewRequest> asCaseworkerReviewing(UUID orderId) {
        return events.external(caseReference, CONFIRM_ORDER_REVIEW).as(caseworker)
            .withClientContext(Map.of("orderId", orderId.toString()));
    }

    private static MakeOrderRequest decision(MakeOrderRequest.Action action, Order order, String decision,
                                             String taskId) {
        return new MakeOrderRequest(action, new OrderChange(order.id(), order.version(), APPLICATION_DECISION,
            Map.of("application-decision", decision), null, order.genAppId()), taskId);
    }

    private static ConfirmOrderReviewRequest issue(Order submitted) {
        return new ConfirmOrderReviewRequest(ISSUE, submitted.id(), submitted.version(), "staff-review", null,
            new Issue(new IssuedOrder(APPLICATION_DECISION, submitted.formData(), null, "the order"), List.of(),
                COUNTY_COURT));
    }

    private static DocumentEntity document(PcsCaseEntity pcsCase, GenAppEntity genApp, UUID documentId,
                                           String fileName) {
        return DocumentEntity.builder().pcsCase(pcsCase).generalApplication(genApp).documentId(documentId)
            .url("http://dm-store/documents/" + documentId).fileName(fileName).build();
    }

    private String outcome(UUID genAppId) {
        return jdbcTemplate.queryForObject("SELECT outcome FROM general_application WHERE id = ?", String.class,
            genAppId);
    }
}
