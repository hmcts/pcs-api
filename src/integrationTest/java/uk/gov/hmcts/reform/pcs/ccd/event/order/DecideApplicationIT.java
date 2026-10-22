package uk.gov.hmcts.reform.pcs.ccd.event.order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.serializer.JacksonSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.ccd.sdk.testing.CcdEventTestSupport;
import uk.gov.hmcts.ccd.sdk.testing.CcdEventTestSupport.Actor;
import uk.gov.hmcts.ccd.sdk.testing.ExternalEvent;
import uk.gov.hmcts.reform.pcs.camunda.TaskType;
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
import uk.gov.hmcts.reform.pcs.ccd.model.OrderDocumentTaskData;
import uk.gov.hmcts.reform.pcs.ccd.service.order.OrderDocumentService;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppState.GEN_APP_ISSUED;
import static uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType.ADJOURN;
import static uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType.SOMETHING_ELSE;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.ConfirmOrderReviewRequest.Action.ISSUE;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.ConfirmOrderReviewRequest.Action.RETURN_TO_JUDGE;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest.Action.SAVE_DRAFT;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest.Action.SUBMIT_FOR_REVIEW;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderSeal.HIGH_COURT;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderType.APPLICATION_DECISION;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderType.NO_ORDER_ON_CLAIM;
import static uk.gov.hmcts.reform.pcs.ccd.event.order.ConfirmOrderReview.CONFIRM_ORDER_REVIEW;
import static uk.gov.hmcts.reform.pcs.ccd.event.order.MakeOrder.MAKE_ORDER;
import static uk.gov.hmcts.reform.pcs.ccd.task.OrderDocumentGenerationComponent.ORDER_DOCUMENT_TASK_DESCRIPTOR;

/**
 * A judge decides a general application with an order on it, from the link in a message about it: the order
 * starts from the application.
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
    @Autowired
    private OrderDocumentService orderDocumentService;
    @Autowired
    private SchedulerClient schedulerClient;

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
    @DisplayName("shows the judge the application they are deciding and what was asked for")
    void showsTheJudgeTheApplication() {
        OrderStart start = asJudgeDeciding(genAppId).start();

        Application application = start.caseContext().application();
        assertThat(application.id()).isEqualTo(genAppId);
        assertThat(application.warrantSuspension()).isFalse();
        assertThat(application.reference()).isEqualTo("GA1");
        assertThat(application.type()).isEqualTo("Adjourn the hearing");
        assertThat(application.applicant()).isEqualTo(IssuedCases.DEFENDANT + " (Defendant 1)");
        assertThat(application.submittedOn()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(application.within14Days()).isEqualTo(VerticalYesNo.YES);
        assertThat(application.whatOrderWanted()).isEqualTo(WHAT_ORDER_WANTED);
        assertThat(application.documents()).extracting(ApplicationDocument::id, ApplicationDocument::fileName)
            .containsExactly(
                tuple(FORM_ID, "application-form.pdf"),
                tuple(WITNESS_STATEMENT_ID, "witness-statement.pdf"));
    }

    @Test
    @DisplayName("sends every open application with an order on the case, oldest first, flagging a warrant suspension")
    void sendsEveryOpenApplication() {
        cases.update(caseReference, pcsCase -> {
            PartyEntity defendant = pcsCase.getParties().stream()
                .filter(party -> party.getOrgName() == null).findFirst().orElseThrow();
            pcsCase.addGenApp(GenAppEntity.builder().party(defendant).type(SOMETHING_ELSE).state(GEN_APP_ISSUED)
                .somethingElseDetails("Suspend the warrant of possession")
                .applicationSubmittedDate(LocalDateTime.of(2026, 10, 9, 14, 0)).build());
        });

        OrderStart start = events.external(caseReference, MAKE_ORDER).as(judge).start();

        assertThat(start.caseContext().application()).isNull();
        assertThat(start.caseContext().applications())
            .extracting(Application::reference, Application::warrantSuspension)
            .containsExactly(tuple("GA1", false), tuple("GA2", true));
        assertThat(start.caseContext().applications()).extracting(Application::id).doesNotContainNull()
            .first().isEqualTo(genAppId);
    }

    @Test
    @DisplayName("keeps the judge's order on the application apart from their draft for the case and its hearing")
    void keepsTheHearingWithTheCaseDraft() {
        int hearingId = cases.hearing(caseReference, LocalDateTime.of(2026, 10, 14, 10, 0), false);
        var forHearing = events.external(caseReference, MAKE_ORDER).as(judge)
            .withClientContext(Map.of("hearingId", Integer.toString(hearingId)));
        MakeOrderRequest draft = startDraft("hearing draft");
        forHearing.submitExpectingSuccess(new MakeOrderRequest(draft.action(), draft.order(), null,
            new MakeOrderRequest.HearingChange(hearingId, null, null, null)));

        var forApplication = asJudgeDeciding(genAppId);
        forApplication.submitExpectingSuccess(decision(SAVE_DRAFT, Order.none(genAppId), "grant", null));

        OrderStart application = forApplication.start();
        assertThat(application.order().genAppId()).isEqualTo(genAppId);
        assertThat(application.caseContext().application()).isNotNull();
        assertThat(application.hearing()).isNull();
        OrderStart hearing = events.external(caseReference, MAKE_ORDER).as(judge).start();
        assertThat(hearing.order().formData()).containsEntry("notes", "hearing draft");
        assertThat(hearing.order().genAppId()).isNull();
        assertThat(hearing.hearing().id()).isEqualTo(hearingId);
        assertThat(hearing.caseContext().application()).isNull();
    }

    @Test
    @DisplayName("completes the judge's task when they send their order on the application for review")
    void completesTheTaskOnSubmission() {
        Order submitted = judgeDecides(genAppId, "grant");

        assertThat(submitted.genAppId()).isNotNull();
        verify(taskManagementApi).completeTask(any(), any(), eq("decide-task"), any());
        verify(camundaService).createTask(eq(caseReference), eq(TaskType.CONFIRM_ORDER_REVIEW),
            contains("has submitted an order for review"));
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

        assertThat(asOtherJudge.submitExpectingRejection(decision(SUBMIT_FOR_REVIEW, asOtherJudge.start().order(),
            "dismiss", null)).errors()).containsExactly(MakeOrder.ALREADY_DECIDED);

        issue(listed);
        Order granted = judgeDecides(genAppId, "grant");
        issue(granted);
        assertThat(events.external(caseReference, MAKE_ORDER).as(judge)
            .withClientContext(Map.of("genAppId", genAppId.toString())).startExpectingRejection())
            .containsExactly(MakeOrder.ALREADY_DECIDED);
    }

    @Test
    @DisplayName("refuses an order on the application once an order made at a hearing decides it")
    void refusesAnOrderOnAnApplicationDecidedAtAHearing() {
        events.external(caseReference, MAKE_ORDER).as(judge).submitExpectingSuccess(new MakeOrderRequest(
            SUBMIT_FOR_REVIEW, new OrderChange(null, 0, NO_ORDER_ON_CLAIM,
                Map.of("app-" + genAppId + "-decision", "grant"), WORDING, null), null));

        assertThat(asJudgeDeciding(genAppId).startExpectingRejection()).containsExactly(MakeOrder.ALREADY_DECIDED);
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

    @Test
    @DisplayName("files the issued order's document with the application, so it is seen only by those who may see "
        + "the application")
    void filesTheOrderDocumentWithTheApplication() {
        Order dismissed = judgeDecides(genAppId, "dismiss");
        when(docAssemblyService.generateDocument(any(), anyString(), any(), anyString()))
            .thenReturn(events.documents().store("Order application.pdf").getUrl());
        issue(dismissed);

        orderDocumentService.generateAndAttach(new JacksonSerializer().deserialize(OrderDocumentTaskData.class,
            schedulerClient.getScheduledExecutionsForTask(ORDER_DOCUMENT_TASK_DESCRIPTOR.getTaskName())
                .getFirst().getRawData()));

        assertThat(jdbcTemplate.queryForObject("SELECT d.general_application_id FROM orders o JOIN document d "
            + "ON d.id = o.document_id WHERE o.id = ?", UUID.class, dismissed.id())).isEqualTo(genAppId);
    }

    private void issue(Order order) {
        asCaseworkerReviewing(order.id()).submitExpectingSuccess(new ConfirmOrderReviewRequest(ISSUE, order.id(),
            order.version(), null, null, new Issue(new IssuedOrder(order.orderType(), order.formData(), WORDING,
                "the order"), List.of(), HIGH_COURT)));
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
            Map.of("app-" + order.genAppId() + "-decision", decision), WORDING, order.genAppId()), taskId);
    }

    private static DocumentEntity document(PcsCaseEntity pcsCase, GenAppEntity genApp, UUID documentId,
                                           String fileName) {
        return DocumentEntity.builder().pcsCase(pcsCase).generalApplication(genApp).documentId(documentId)
            .url("http://dm-store/documents/" + documentId).fileName(fileName).build();
    }

}
