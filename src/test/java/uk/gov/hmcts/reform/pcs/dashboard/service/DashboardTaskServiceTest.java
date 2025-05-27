package uk.gov.hmcts.reform.pcs.dashboard.service;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.dashboard.model.Task;
import uk.gov.hmcts.reform.pcs.dashboard.model.TaskGroup;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class DashboardTaskServiceTest {

    @InjectMocks
    private DashboardTaskService dashboardTaskService;

    private static final long CASE_REF_WITH_NO_TASKS = 8888L;
    private static final long CASE_REF_WITH_TASKS = 1234L;
    private static final long UNKNOWN_CASE_REF = 9999L;

    private Task task1;
    private Task task2;

    @BeforeEach
    void setUp() {
        task1 = Task.builder()
            .templateId("Task.AAA6.Claim.ViewClaim")
            .status("AVAILABLE")
            .templateValues(Map.of(
                "dueDate", LocalDate.of(2025, 5, 20),
                "amount", BigDecimal.valueOf(76.00),
                "location", "London",
                "appointmentTime", LocalDateTime.of(2025, 5, 20, 10, 30, 0, 0)
            ))
            .build();

        task2 = Task.builder()
            .templateId("Task.AAA6.Hearing.UploadDocuments")
            .status("ACTION_NEEDED")
            .templateValues(Map.of(
                "deadline", LocalDate.of(2025, 5, 20)
            ))
            .build();
    }

    @Test
    void testGetTasksForCaseWithTasksReturnsTaskGroups() {
        List<TaskGroup> result = dashboardTaskService.getTasks(CASE_REF_WITH_TASKS);

        assertThat(result).hasSize(2);

        TaskGroup taskGroup1 = result.getFirst();
        assertThat(taskGroup1.getGroupId()).isEqualTo("CLAIM");
        assertThat(taskGroup1.getTasks()).hasSize(1);
        assertThat(taskGroup1.getTasks().getFirst().getTemplateId()).isEqualTo(task1.getTemplateId());
        assertThat(taskGroup1.getTasks().getFirst().getStatus()).isEqualTo(task1.getStatus());

        TaskGroup taskGroup2 = result.get(1);
        assertThat(taskGroup2.getGroupId()).isEqualTo("HEARING");
        assertThat(taskGroup2.getTasks()).hasSize(1);
        assertThat(taskGroup2.getTasks().getFirst().getTemplateId()).isEqualTo(task2.getTemplateId());
        assertThat(taskGroup2.getTasks().getFirst().getStatus()).isEqualTo(task2.getStatus());
    }

    @Test
    void testGetTasksForCaseNoTasksReturnsEmptyList() {
        List<TaskGroup> result = dashboardTaskService.getTasks(CASE_REF_WITH_NO_TASKS);
        assertThat(result).isEmpty();
    }

    @Test
    void testGetTasksForCaseCaseNotFoundThrowsCaseNotFoundException() {
        assertThatThrownBy(() -> dashboardTaskService.getTasks(UNKNOWN_CASE_REF))
            .isInstanceOf(CaseNotFoundException.class)
            .hasMessageContaining("No case found with reference");
    }
}
