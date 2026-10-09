package uk.gov.hmcts.reform.pcs.config;

import com.github.kagkarlsson.scheduler.Scheduler;
import org.junit.jupiter.api.AfterEach;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.reform.ccd.document.am.feign.CaseDocumentClientApi;
import uk.gov.hmcts.reform.pcs.camunda.SendMessageRequest;
import uk.gov.hmcts.reform.pcs.camunda.TaskManagementApi;
import uk.gov.hmcts.reform.pcs.camunda.TaskType;
import uk.gov.hmcts.reform.pcs.camunda.WorkAllocationWorkflowApi;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest.OrderChange;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderStart.Order;
import uk.gov.hmcts.reform.pcs.document.service.DocAssemblyService;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest.Action.SAVE_DRAFT;

/**
 * The order events' tests, sharing one application context: each stubs the same services outside
 * pcs-api, so there is one scheduler running the jobs the events leave behind, not one per context.
 */
@PcsCcdEventTest
public abstract class OrderEventIT extends AbstractPostgresContainerIT {

    @MockitoBean
    protected WorkAllocationWorkflowApi workAllocationWorkflowApi;
    @MockitoBean
    protected TaskManagementApi taskManagementApi;
    @MockitoBean
    protected DocAssemblyService docAssemblyService;
    @MockitoBean
    protected CaseDocumentClientApi caseDocumentClientApi;
    @Autowired
    protected Scheduler scheduler;
    @Autowired
    protected JdbcTemplate jdbcTemplate;

    /** The jobs a test's events left for the scheduler are its own, not the next test's to run against its stubs. */
    @AfterEach
    void dropLeftoverJobs() {
        jdbcTemplate.update("DELETE FROM scheduled_tasks WHERE task_name IN"
            + " ('camunda-request-task', 'order-document-generation')");
    }

    /** The scheduler sends the Camunda requests the events left behind, as it would within seconds. */
    protected void camundaRequestsRun() {
        jdbcTemplate.update(
            "UPDATE scheduled_tasks SET execution_time = now() WHERE task_name = 'camunda-request-task'");
        scheduler.triggerCheckForDueExecutions();
        await(() -> jdbcTemplate.queryForObject("SELECT count(*) FROM scheduled_tasks"
            + " WHERE task_name = 'camunda-request-task' AND coalesce(consecutive_failures, 0) = 0",
            Integer.class) == 0);
    }

    /** Waits up to 15 seconds for the scheduler to have done something; whether it did is the test's to check. */
    protected static boolean await(BooleanSupplier done) {
        Instant until = Instant.now().plus(Duration.ofSeconds(15));
        while (!done.getAsBoolean()) {
            if (Instant.now().isAfter(until)) {
                return false;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return true;
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

    /** Every message sent to Camunda so far. */
    protected List<SendMessageRequest> camundaMessages() {
        ArgumentCaptor<SendMessageRequest> messages = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(workAllocationWorkflowApi, atLeast(0)).sendMessage(any(), messages.capture());
        return messages.getAllValues();
    }

    /** The request to create a task of a type, as Camunda was sent it, in a test that creates one. */
    protected SendMessageRequest created(TaskType taskType) {
        return camundaMessages().stream()
            .filter(message -> taskType.getId().equals(message.getProcessVariables().get("taskId").getValue()))
            .reduce((first, second) -> second)
            .orElseThrow(() -> new AssertionError("No " + taskType.getId() + " task was created"));
    }

    /** The descriptions of every task of a type Camunda was asked to create, in no particular order. */
    protected List<String> createdDescriptions(TaskType taskType) {
        return camundaMessages().stream()
            .filter(message -> taskType.getId().equals(message.getProcessVariables().get("taskId").getValue()))
            .map(message -> (String) message.getProcessVariables().get("taskDescription").getValue())
            .toList();
    }
}
