package uk.gov.hmcts.reform.pcs.camunda;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.TaskInstanceId;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Removes a Camunda request that db-scheduler is still holding, for the requests that are scheduled for a
 * later date and so can be withdrawn before they are sent.
 *
 * <p>{@link SchedulerClient#cancel} cannot do this safely. It reads the row, refuses with
 * {@code TaskInstanceCurrentlyExecutingException} if another node has picked it, and otherwise deletes it by
 * the version it read — which fails with an {@code ExecutionException} if a node picked the row in between.
 * Either way a scheduler on another node decides whether a submit succeeds. One conditional delete answers
 * the same question atomically: it takes the row, or it takes nothing because another node is already
 * sending the request.</p>
 *
 * <p>The statement runs on the transaction-aware DataSource, so it joins the submit's transaction just as
 * scheduling does and a rollback puts the request back.</p>
 */
@Component
public class PendingCamundaRequests {

    private static final String DELETE_UNPICKED_REQUEST = """
        DELETE FROM scheduled_tasks
        WHERE task_name = ? AND task_instance = ? AND picked = false
        """;

    private final JdbcTemplate jdbcTemplate;

    public PendingCamundaRequests(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Withdraws the request if it has not been sent.
     *
     * @return true if the request was withdrawn, false if it is gone already or a node is running it now
     */
    public boolean withdraw(TaskInstanceId request) {
        return jdbcTemplate.update(DELETE_UNPICKED_REQUEST, request.getTaskName(), request.getId()) > 0;
    }

}
