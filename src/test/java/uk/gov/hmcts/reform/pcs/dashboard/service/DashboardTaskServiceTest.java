package uk.gov.hmcts.reform.pcs.dashboard.service;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.dashboard.model.TaskGroup;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class DashboardTaskServiceTest {

    @InjectMocks
    private DashboardTaskService dashboardTaskService;

    private static final long CASE_REF_WITH_NO_TASKS = 8888L;
    private static final long CASE_REF_WITH_TASKS = 1234L;
    private static final long UNKNOWN_CASE_REF = 9999L;

    @Test
    void testGetTasksForCaseWithTasksReturnsTaskGroups() {
        List<TaskGroup> result = dashboardTaskService.getTasks(CASE_REF_WITH_TASKS);

        // Expect 6 task groups now
        assertThat(result).hasSize(6);

        // Verify CLAIM group
        TaskGroup claimGroup = result.stream()
            .filter(group -> "CLAIM".equals(group.getGroupId()))
            .findFirst()
            .orElseThrow();
        assertThat(claimGroup.getTasks()).hasSizeGreaterThan(0);
        assertThat(claimGroup.getTasks().getFirst().getTemplateId()).startsWith("Task.AAA6.Claim.");

        // Verify HEARING group
        TaskGroup hearingGroup = result.stream()
            .filter(group -> "HEARING".equals(group.getGroupId()))
            .findFirst()
            .orElseThrow();
        assertThat(hearingGroup.getTasks()).hasSizeGreaterThan(0);

        // Check if a task with Upload Documents exists in HEARING group
//        assertThat(hearingGroup.getTasks().stream()
//            .anyMatch(task -> "Task.AAA6.Hearing.UploadDocuments".equals(task.getTemplateId())
//                && "ACTION_NEEDED".equals(task.getStatus()))).isTrue();
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
