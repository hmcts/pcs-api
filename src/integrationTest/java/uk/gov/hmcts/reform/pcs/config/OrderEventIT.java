package uk.gov.hmcts.reform.pcs.config;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.reform.ccd.document.am.feign.CaseDocumentClientApi;
import uk.gov.hmcts.reform.pcs.camunda.CamundaService;
import uk.gov.hmcts.reform.pcs.camunda.TaskManagementApi;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest.OrderChange;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderStart.Order;
import uk.gov.hmcts.reform.pcs.document.service.DocAssemblyService;

import java.util.Map;

import static uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest.Action.SAVE_DRAFT;

/**
 * The order events' tests, sharing one application context: each stubs the same services outside
 * pcs-api, so there is one scheduler running the jobs the events leave behind, not one per context.
 * Work Allocation tasks are checked as the events ask for them; sending them to Camunda is
 * CamundaService's own concern.
 */
@PcsCcdEventTest
public abstract class OrderEventIT extends AbstractPostgresContainerIT {

    @MockitoBean
    protected CamundaService camundaService;
    @MockitoBean
    protected TaskManagementApi taskManagementApi;
    @MockitoBean
    protected DocAssemblyService docAssemblyService;
    @MockitoBean
    protected CaseDocumentClientApi caseDocumentClientApi;
    @Autowired
    protected JdbcTemplate jdbcTemplate;

    /** The documents a test's issued orders left for the scheduler are its own, not the next test's. */
    @AfterEach
    void dropLeftoverJobs() {
        jdbcTemplate.update("DELETE FROM scheduled_tasks WHERE task_name = 'order-document-generation'");
    }

    /** A judge saves a first draft, which has no id yet. */
    protected static MakeOrderRequest startDraft(String notes) {
        return new MakeOrderRequest(SAVE_DRAFT,
            new OrderChange(null, 0, "OUTRIGHT_POSSESSION", Map.of("notes", notes), null), null);
    }

    /** The judge acts on the order as they last saw it, with a new note. */
    protected static MakeOrderRequest change(MakeOrderRequest.Action action, Order order, String notes) {
        return change(action, order, notes, null);
    }

    /** The judge acts on the order as they last saw it, from the task they followed to it. */
    protected static MakeOrderRequest change(MakeOrderRequest.Action action, Order order, String notes,
                                             String taskId) {
        return new MakeOrderRequest(action, new OrderChange(
            order.id(), order.version(), order.orderType(), Map.of("notes", notes), order.docweaveSnapshot()), taskId);
    }
}
