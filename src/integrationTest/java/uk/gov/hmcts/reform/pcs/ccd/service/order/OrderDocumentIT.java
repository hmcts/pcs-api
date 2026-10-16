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
import uk.gov.hmcts.reform.pcs.ccd.model.OrderDocumentTaskData;
import uk.gov.hmcts.reform.pcs.config.IssuedCases;
import uk.gov.hmcts.reform.pcs.config.OrderEventIT;
import uk.gov.hmcts.reform.pcs.document.model.order.OrderDocumentPayload;
import uk.gov.hmcts.reform.pcs.document.model.order.OrderDocumentPayload.OrderParty;

import java.util.List;
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
            new OrderChange(null, 0, OrderType.OUTRIGHT_POSSESSION, Map.of(), null), null));
        Order draft = asJudge.start().order();
        asJudge.submitExpectingSuccess(new MakeOrderRequest(SUBMIT_FOR_REVIEW, new OrderChange(
            draft.id(), draft.version(), draft.orderType(), draft.formData(), null), null));
        var asCaseworker = events.external(caseReference, CONFIRM_ORDER_REVIEW).as(caseworker)
            .withClientContext(Map.of("orderId", draft.id().toString()));
        Order submitted = asCaseworker.start().order();
        Document order = events.documents().store("Order for possession.pdf");
        when(docAssemblyService.generateDocument(any(), anyString(), any(), anyString())).thenReturn(order.getUrl());

        asCaseworker.submitExpectingSuccess(new ConfirmOrderReviewRequest(ISSUE, submitted.id(),
            submitted.version(), "staff-review", null,
            new Issue(
                new IssuedOrder(OrderType.OUTRIGHT_POSSESSION, Map.of(), null, "the caseworker's wording"),
                List.of(), COUNTY_COURT)));
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
