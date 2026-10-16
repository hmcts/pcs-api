package uk.gov.hmcts.reform.pcs.config;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.reform.pcs.camunda.CamundaService;
import uk.gov.hmcts.reform.pcs.camunda.TaskManagementApi;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest.OrderChange;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderStart.Order;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderType;
import uk.gov.hmcts.reform.pcs.document.service.DocAssemblyService;

import java.util.Map;

import static uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest.Action.SAVE_DRAFT;

/**
 * The order events' tests share one application context, stubbing the same services outside pcs-api.
 * No scheduler runs the jobs an event schedules; a test runs them itself, so it knows when they ran.
 */
@PcsCcdEventTest
@TestPropertySource(properties = "db-scheduler.executor-enabled=false")
public abstract class OrderEventIT extends AbstractPostgresContainerIT {

    @MockitoBean
    protected CamundaService camundaService;
    @MockitoBean
    protected TaskManagementApi taskManagementApi;
    @MockitoBean
    protected DocAssemblyService docAssemblyService;
    @Autowired
    protected JdbcTemplate jdbcTemplate;

    /** The jobs a test's events scheduled are its own, not the next test's. */
    @AfterEach
    void dropLeftoverJobs() {
        jdbcTemplate.update("DELETE FROM scheduled_tasks WHERE task_name = 'order-document-generation'");
    }

    /** A judge saves a first draft, which has no id yet. */
    protected static MakeOrderRequest startDraft(String notes) {
        return new MakeOrderRequest(SAVE_DRAFT,
            new OrderChange(null, 0, OrderType.OUTRIGHT_POSSESSION, Map.of("notes", notes), null), null);
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
