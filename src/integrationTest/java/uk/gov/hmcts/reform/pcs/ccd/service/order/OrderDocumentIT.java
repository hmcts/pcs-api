package uk.gov.hmcts.reform.pcs.ccd.service.order;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.serializer.JacksonSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.ccd.sdk.testing.CcdEventTestSupport;
import uk.gov.hmcts.ccd.sdk.testing.CcdEventTestSupport.Actor;
import uk.gov.hmcts.ccd.sdk.testing.ExternalEvent;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.docassembly.domain.OutputType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.ConfirmOrderReviewRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.ConfirmOrderReviewRequest.Issue;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.ConfirmOrderReviewRequest.IssuedOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest.OrderChange;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderStart;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderStart.Order;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderType;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.model.OrderDocumentTaskData;
import uk.gov.hmcts.reform.pcs.config.IssuedCases;
import uk.gov.hmcts.reform.pcs.config.OrderEventIT;
import uk.gov.hmcts.reform.pcs.document.model.order.OrderDocumentPayload;
import uk.gov.hmcts.reform.pcs.document.model.order.OrderDocumentPayload.OrderParty;

import java.util.List;
import java.util.UUID;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.ConfirmOrderReviewRequest.Action.ISSUE;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest.Action.SAVE_DRAFT;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest.Action.SUBMIT_FOR_REVIEW;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderSeal.COUNTY_COURT;
import static uk.gov.hmcts.reform.pcs.ccd.event.order.ConfirmOrderReview.CONFIRM_ORDER_REVIEW;
import static uk.gov.hmcts.reform.pcs.ccd.event.order.MakeOrder.MAKE_ORDER;
import static uk.gov.hmcts.reform.pcs.ccd.task.OrderDocumentGenerationComponent.ORDER_DOCUMENT_TASK_DESCRIPTOR;

/** An issued order's document: fixed as it is issued, rendered by the job its issue schedules, added to the case. */
@DisplayName("Issued order document")
class OrderDocumentIT extends OrderEventIT {

    @Autowired
    private CcdEventTestSupport<PCSCase, State> events;
    @Autowired
    private IssuedCases cases;
    @Autowired
    private OrderDocumentService orderDocumentService;
    @Autowired
    private SchedulerClient schedulerClient;

    @Test
    @DisplayName("renders the issued order's wording with the case, its parties and the judge, and adds it to the case")
    void rendersAndAttachesTheIssuedOrder() {
        long caseReference = cases.issue();
        Actor judge = events.registerActor("Sarah", "Hughes", "caseworker-pcs");
        Actor caseworker = events.registerActor("Tom", "Baker", "caseworker-pcs");
        ExternalEvent<OrderStart, MakeOrderRequest> asJudge = events.external(caseReference, MAKE_ORDER).as(judge);
        asJudge.submitExpectingSuccess(new MakeOrderRequest(SAVE_DRAFT,
            new OrderChange(null, 0, OrderType.OUTRIGHT_POSSESSION, Map.of(), WORDING), null));
        Order draft = asJudge.start().order();
        asJudge.submitExpectingSuccess(new MakeOrderRequest(SUBMIT_FOR_REVIEW, new OrderChange(
            draft.id(), draft.version(), draft.orderType(), draft.formData(), WORDING), null));
        var asCaseworker = events.external(caseReference, CONFIRM_ORDER_REVIEW).as(caseworker)
            .withClientContext(Map.of("orderId", draft.id().toString()));
        Order submitted = asCaseworker.start().order();
        Document order = events.documents().store("Order for possession.pdf");
        when(docAssemblyService.generateDocument(any(), anyString(), any(), anyString())).thenReturn(order.getUrl());

        asCaseworker.submitExpectingSuccess(new ConfirmOrderReviewRequest(ISSUE, submitted.id(),
            submitted.version(), "staff-review", null,
            new Issue(
                new IssuedOrder(OrderType.OUTRIGHT_POSSESSION, Map.of(), WORDING, "the caseworker's wording"),
                List.of(), COUNTY_COURT, true, List.of())));
        OrderDocumentTaskData job = scheduledGeneration();
        assertThat(job).extracting(OrderDocumentTaskData::getOrderId, OrderDocumentTaskData::getCaseReference,
                OrderDocumentTaskData::getCaseworkerId, OrderDocumentTaskData::getCaseworkerFirstName,
                OrderDocumentTaskData::getCaseworkerLastName)
            .containsExactly(submitted.id(), caseReference, caseworker.uid(), "Tom", "Baker");
        OrderDocumentPayload issued = job.getPayload();
        assertThat(issued.getJudgeName()).isEqualTo("Sarah Hughes");
        assertThat(issued.isCountyCourtSeal()).isTrue();
        assertThat(issued.isHighCourtSeal()).isFalse();
        assertThat(issued.getOrderHtml()).isEqualTo("the caseworker's wording");
        assertThat(issued.getParties()).contains(new OrderParty(IssuedCases.DEFENDANT, "Defendant"));

        orderDocumentService.generateAndAttach(job);

        verify(docAssemblyService).generateDocument(
            issued, "CV-PCS-ORD-ENG-Order.docx", OutputType.PDF, "Order for possession");
        assertThat(documents(caseReference, caseworker))
            .extracting(Document::getUrl, Document::getFilename, Document::getCategoryId)
            .containsExactly(tuple(order.getUrl(), "Order for possession.pdf", "ordersAndNoticeOfHearings"));
        var history = events.history(caseReference).getLast();
        assertThat(history.eventId()).isEqualTo("orderDocumentGenerated");
        assertThat(history.userId()).isEqualTo(caseworker.uid());

        // A re-run, as a retried job is, renders nothing and adds no second document.
        orderDocumentService.generateAndAttach(job);
        verify(docAssemblyService).generateDocument(any(), anyString(), any(), anyString());
        assertThat(documents(caseReference, caseworker)).hasSize(1);
    }

    @Test
    @DisplayName("issues an order served on all parties as an order every party sees")
    void servesAnOrderOnAllParties() {
        long caseReference = cases.issue();
        issueAndAttach(caseReference, true, List.of());

        assertThat(documentType(caseReference)).isEqualTo("ORDER");
        assertThat(documents(caseReference, party(caseReference, IssuedCases.CLAIMANT))).hasSize(1);
        assertThat(documents(caseReference, party(caseReference, "Jane"))).hasSize(1);
    }

    @Test
    @DisplayName("issues an order served on some parties without notice as a without notice order only they and "
        + "court staff see")
    void servesAnOrderOnSomePartiesWithoutNotice() {
        long caseReference = cases.issue();
        cases.update(caseReference, pcsCase -> pcsCase.addParty(
            PartyEntity.builder().firstName("John").lastName("Smith").build()));
        issueAndAttach(caseReference, false,
            List.of(partyId(caseReference, IssuedCases.CLAIMANT), partyId(caseReference, "Jane")));

        assertThat(documentType(caseReference)).isEqualTo("WITHOUT_NOTICE_ORDER");
        assertThat(documents(caseReference, party(caseReference, IssuedCases.CLAIMANT))).hasSize(1);
        assertThat(documents(caseReference, party(caseReference, "Jane"))).hasSize(1);
        assertThat(documents(caseReference, events.registerActor("Court", "Staff", "hearing-centre-admin")))
            .hasSize(1);
        assertThat(documents(caseReference, party(caseReference, "John"))).isEmpty();
    }

    @Test
    @DisplayName("refuses to issue an order served without notice on no one, or on someone not a party to the case")
    void refusesToServeAnOrderOnNoParty() {
        long caseReference = cases.issue();
        var review = submittedForReview(caseReference);

        assertThat(review.asCaseworker().submitExpectingRejection(issue(review.order(), false, List.of())).errors())
            .containsExactly("Select who to serve the order on");
        assertThat(review.asCaseworker().submitExpectingRejection(issue(review.order(), false,
            List.of(partyId(caseReference, "Jane"), UUID.randomUUID()))).errors())
            .containsExactly("Select who to serve the order on");
    }

    /** A judge's order sent for review, as the caseworker reviewing it has it. */
    private Review submittedForReview(long caseReference) {
        Actor judge = events.registerActor("Sarah", "Hughes", "caseworker-pcs");
        Actor caseworker = events.registerActor("Tom", "Baker", "caseworker-pcs");
        var asJudge = events.external(caseReference, MAKE_ORDER).as(judge);
        asJudge.submitExpectingSuccess(new MakeOrderRequest(SUBMIT_FOR_REVIEW,
            new OrderChange(null, 0, OrderType.FREE_FORM, Map.of(), WORDING), null));
        UUID orderId = jdbcTemplate.queryForObject("SELECT o.id FROM orders o JOIN pcs_case c ON c.id = o.case_id "
            + "WHERE c.case_reference = ?", UUID.class, caseReference);
        var asCaseworker = events.external(caseReference, CONFIRM_ORDER_REVIEW).as(caseworker)
            .withClientContext(Map.of("orderId", orderId.toString()));
        return new Review(asCaseworker, asCaseworker.start().order());
    }

    private record Review(ExternalEvent<OrderStart, ConfirmOrderReviewRequest> asCaseworker, Order order) {
    }

    private static ConfirmOrderReviewRequest issue(Order order, boolean serveAllParties, List<UUID> partiesToServe) {
        return new ConfirmOrderReviewRequest(ISSUE, order.id(), order.version(), null, null, new Issue(
            new IssuedOrder(order.orderType(), order.formData(), WORDING, "the order"), List.of(), COUNTY_COURT,
            serveAllParties, partiesToServe));
    }

    /** Issues the case's order, served as court staff chose, and runs the job that adds its document. */
    private void issueAndAttach(long caseReference, boolean serveAllParties, List<UUID> partiesToServe) {
        var review = submittedForReview(caseReference);
        when(docAssemblyService.generateDocument(any(), anyString(), any(), anyString()))
            .thenReturn(events.documents().store("Order.pdf").getUrl());
        review.asCaseworker().submitExpectingSuccess(issue(review.order(), serveAllParties, partiesToServe));
        orderDocumentService.generateAndAttach(scheduledGeneration());
    }

    /** A citizen signed in as the party on the case with that organisation or first name. */
    private Actor party(long caseReference, String name) {
        Actor actor = events.registerActor(name, "Citizen", "citizen");
        cases.update(caseReference, pcsCase -> partyOf(pcsCase, name).setIdamId(actor.uid()));
        return actor;
    }

    private UUID partyId(long caseReference, String name) {
        UUID[] id = new UUID[1];
        cases.update(caseReference, pcsCase -> id[0] = partyOf(pcsCase, name).getId());
        return id[0];
    }

    private static PartyEntity partyOf(PcsCaseEntity pcsCase, String name) {
        return pcsCase.getParties().stream()
            .filter(party -> name.equals(party.getOrgName()) || name.equals(party.getFirstName()))
            .findFirst().orElseThrow();
    }

    private String documentType(long caseReference) {
        return jdbcTemplate.queryForObject("SELECT type FROM document WHERE case_id = (SELECT id FROM pcs_case "
            + "WHERE case_reference = ?)", String.class, caseReference);
    }

    /** The document generation the issuing event left for the scheduler. */
    private OrderDocumentTaskData scheduledGeneration() {
        byte[] data = schedulerClient.getScheduledExecutionsForTask(ORDER_DOCUMENT_TASK_DESCRIPTOR.getTaskName())
            .getFirst().getRawData();
        return new JacksonSerializer().deserialize(OrderDocumentTaskData.class, data);
    }

    /** The documents on the case, as the case file view lists them. */
    private List<Document> documents(long caseReference, Actor viewer) {
        return events.view(caseReference, viewer).getAllDocuments().stream().map(ListValue::getValue).toList();
    }
}
