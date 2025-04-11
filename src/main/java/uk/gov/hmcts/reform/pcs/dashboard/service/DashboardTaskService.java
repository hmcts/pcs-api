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

        var task1 = Task.builder()
            .templateId("Task.AAA6.Claim.ViewClaim")
            .status("AVAILABLE")
            .templateValues(Map.of(
                "dueDate", LocalDate.of(2025, 5, 20),
                "amount", BigDecimal.valueOf(76.00),
                "location", "London",
                "appointmentTime", LocalDateTime.of(2025, 5, 20, 10, 30, 0, 0)
            ))
            .build();

        var task2 = Task.builder()
            .templateId("Task.AAA6.Hearing.UploadDocuments")
            .status("ACTION_NEEDED")
            .templateValues(Map.of(
                "deadline", LocalDate.of(2025, 5, 20)
            ))
            .build();

        var taskGroup1 = TaskGroup.builder()
            .groupId("CLAIM")
            .task(task1)
            .build();

        var taskGroup2 = TaskGroup.builder()
            .groupId("HEARING")
            .task(task2)
            .build();

        return List.of(taskGroup1, taskGroup2);
    }
}
