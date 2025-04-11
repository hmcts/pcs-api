package uk.gov.hmcts.reform.pcs.dashboard.service;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.dashboard.model.Task;
import uk.gov.hmcts.reform.pcs.dashboard.model.TaskGroup;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class DashboardTaskService {

    private static final long CASE_REF_WITH_NO_TASKS = 8888L;
    private static final long UNKNOWN_CASE_REF = 9999L;

    /**
     * Returns the active tasks for the specified case reference.
     *
     * @param caseReference The CCD case reference
     * @return Some mock tasks for now
     * @throws CaseNotFoundException - if the case cannot be found
     */
    public List<TaskGroup> getTasks(long caseReference) {


        if (caseReference == UNKNOWN_CASE_REF) {
            throw new CaseNotFoundException(caseReference);
        }

        if (caseReference == CASE_REF_WITH_NO_TASKS) {
            return List.of();
        }

        // Define tasks with dummy data
        Task task1 = Task.builder()
            .templateId("Task.AAA6.Claim.ViewClaim")
            .groupId("CLAIM")   // Grouped under "CLAIM"
            .status("AVAILABLE")
            .templateValues(Map.of(
                "dueDate", LocalDate.of(2025, 5, 20),
                "amount", BigDecimal.valueOf(76.00),
                "location", "London",
                "appointmentTime", LocalDateTime.of(2025, 5, 20, 10, 30, 0, 0)
            ))
            .build();

        Task task2 = Task.builder()
            .templateId("Task.AAA6.Hearing.UploadDocuments")
            .groupId("HEARING")  // Grouped under "HEARING"
            .status("ACTION_NEEDED")
            .templateValues(Map.of(
                "deadline", LocalDate.of(2025, 5, 20)
            ))
            .build();

        // Now grouping tasks under TaskGroup
        var taskGroup1 = new TaskGroup();
        taskGroup1.setGroupId("CLAIM");
        taskGroup1.setStatus("AVAILABLE");
        taskGroup1.setTasks(task1);

        var taskGroup2 = new TaskGroup();
        taskGroup2.setGroupId("HEARING");
        taskGroup2.setStatus("ACTION_NEEDED");
        taskGroup2.setTasks(task2);

        return List.of(taskGroup1, taskGroup2);
    }
}
