package uk.gov.hmcts.reform.pcs.ccd.service.order;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.ccd.sdk.testing.CcdEventTestSupport;
import uk.gov.hmcts.ccd.sdk.testing.CcdEventTestSupport.Actor;
import uk.gov.hmcts.ccd.sdk.testing.ExternalEvent;
import uk.gov.hmcts.reform.docassembly.domain.FormPayload;
import uk.gov.hmcts.reform.docassembly.domain.OutputType;
import uk.gov.hmcts.reform.pcs.camunda.CamundaService;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.ConfirmOrderReviewRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.ConfirmOrderReviewRequest.Issue;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.ConfirmOrderReviewRequest.IssuedOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest.OrderChange;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderStart;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderStart.Order;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentImportService;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.config.IssuedCases;
import uk.gov.hmcts.reform.pcs.config.PcsCcdEventTest;
import uk.gov.hmcts.reform.pcs.document.model.order.OrderDocumentPayload;
import uk.gov.hmcts.reform.pcs.document.model.order.OrderDocumentPayload.OrderParty;
import uk.gov.hmcts.reform.pcs.document.service.DocAssemblyService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.ConfirmOrderReviewRequest.Action.ISSUE;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest.Action.SAVE_DRAFT;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest.Action.SUBMIT_FOR_REVIEW;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderSeal.COUNTY_COURT;
import static uk.gov.hmcts.reform.pcs.ccd.event.order.ConfirmOrderReview.CONFIRM_ORDER_REVIEW;
import static uk.gov.hmcts.reform.pcs.ccd.event.order.MakeOrder.MAKE_ORDER;

/**
 * The document of an order a caseworker issued: rendered from its wording into the order template, with the case,
 * its parties and the judge who made it, and added to the case.
 */
@PcsCcdEventTest
@DisplayName("Issued order document")
class OrderDocumentIT extends AbstractPostgresContainerIT {

    private static final String WORDING = "<p>IT IS ORDERED THAT:</p><ol><li><p>Possession.</p></li></ol>";
    private static final String DM_STORE_URL = "http://dm-store/documents/" + UUID.randomUUID();

    @Autowired
    private CcdEventTestSupport<PCSCase, State> events;
    @Autowired
    private IssuedCases cases;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private OrderDocumentService orderDocumentService;
    @MockitoBean
    private CamundaService camundaService;
    @MockitoBean
    private DocAssemblyService docAssemblyService;
    @MockitoBean
    private DocumentImportService documentImportService;

    @Test
    @DisplayName("renders the issued order's wording with the case, its parties and the judge, and adds it to the case")
    void rendersAndAttachesTheIssuedOrder() {
        long caseReference = cases.issue();
        Actor judge = events.registerActor("Sarah", "Hughes", "caseworker-pcs");
        Actor caseworker = events.registerActor("Tom", "Baker", "caseworker-pcs");
        ExternalEvent<OrderStart, MakeOrderRequest> asJudge = events.external(caseReference, MAKE_ORDER).as(judge);
        asJudge.submitExpectingSuccess(new MakeOrderRequest(SAVE_DRAFT,
            new OrderChange(null, 0, "OUTRIGHT_POSSESSION", Map.of(), null)));
        Order draft = asJudge.start().order();
        Order submitted = asJudge.submitExpectingSuccess(new MakeOrderRequest(SUBMIT_FOR_REVIEW, new OrderChange(
                draft.id(), draft.version(), draft.orderType(), draft.formData(), null)))
            .changed("draft_orders", Order.class);
        events.external(caseReference, CONFIRM_ORDER_REVIEW).as(caseworker)
            .withClientContext(Map.of("orderId", submitted.id().toString()))
            .submitExpectingSuccess(new ConfirmOrderReviewRequest(ISSUE, submitted.id(), submitted.version(), null,
                new Issue(new IssuedOrder("OUTRIGHT_POSSESSION", Map.of(), null, WORDING),
                    List.of(), true, true, true, List.of(), COUNTY_COURT)));
        UUID issuedOrderId = jdbcTemplate.queryForObject(
            "SELECT id FROM issued_orders WHERE draft_order_id = ?", UUID.class, submitted.id());
        when(docAssemblyService.generateDocument(any(), anyString(), any(), anyString())).thenReturn(DM_STORE_URL);
        when(documentImportService.addDocumentToCase(any(PcsCaseEntity.class), eq(DM_STORE_URL), any()))
            .thenAnswer(invocation -> DocumentEntity.builder()
                .pcsCase(invocation.getArgument(0))
                .fileName("Order for possession.pdf")
                .build());

        orderDocumentService.generateAndAttach(issuedOrderId);

        ArgumentCaptor<FormPayload> payload = ArgumentCaptor.forClass(FormPayload.class);
        verify(docAssemblyService, atLeastOnce()).generateDocument(payload.capture(),
            eq("CV-PCS-ORD-ENG-Order.docx"), eq(OutputType.PDF), eq("Order for possession"));
        OrderDocumentPayload rendered = (OrderDocumentPayload) payload.getValue();
        assertThat(rendered.getJudgeName()).isEqualTo("Sarah Hughes");
        assertThat(rendered.getOrderHtml()).isEqualTo(WORDING);
        assertThat(rendered.getParties()).contains(new OrderParty(IssuedCases.DEFENDANT, "Defendant"));
        assertThat(rendered.getCaseNumber()).isEqualTo(String.valueOf(caseReference)
            .replaceAll("(\\d{4})(\\d{4})(\\d{4})(\\d{4})", "$1-$2-$3-$4"));
        assertThat(jdbcTemplate.queryForObject("""
            SELECT d.type FROM issued_orders o JOIN document d ON d.id = o.document_id WHERE o.id = ?""",
            String.class, issuedOrderId)).isEqualTo("ORDER");
    }
}
