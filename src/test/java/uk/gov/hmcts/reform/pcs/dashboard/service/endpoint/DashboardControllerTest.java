package uk.gov.hmcts.reform.pcs.dashboard.service.endpoint;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.pcs.dashboard.endpoint.DashboardController;
import uk.gov.hmcts.reform.pcs.dashboard.model.TaskGroup;
import uk.gov.hmcts.reform.pcs.dashboard.service.DashboardNotificationService;
import uk.gov.hmcts.reform.pcs.dashboard.service.DashboardTaskService;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(DashboardController.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardTaskService dashboardTaskService;

    @MockitoBean
    private DashboardNotificationService dashboardNotificationService;

    @Test
    @WithMockUser
    void testGetTasksForCaseWithTasksReturnsTaskGroups() throws Exception {
        var caseReference = 1234L;

        var taskGroup1 = TaskGroup.builder()
            .groupId("CLAIM")
            .task(null)
            .build();
        var taskGroup2 = TaskGroup.builder()
            .groupId("HEARING")
            .task(null)
            .build();

        List<TaskGroup> taskGroups = List.of(taskGroup1, taskGroup2);

        when(dashboardTaskService.getTasks(caseReference)).thenReturn(taskGroups);

        mockMvc.perform(get("/dashboard/{caseReference}/tasks", caseReference)
                            .header("Authorization", "Bearer valid_token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].groupId").value("CLAIM"))
            .andExpect(jsonPath("$[1].groupId").value("HEARING"));
    }

    @Test
    @WithMockUser
    void testGetTasksForCaseNoTasksReturnsEmptyList() throws Exception {
        var caseReference = 8888L;

        when(dashboardTaskService.getTasks(caseReference)).thenReturn(List.of());

        mockMvc.perform(get("/dashboard/{caseReference}/tasks", caseReference)
                            .header("Authorization", "Bearer valid_token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @WithMockUser
    void testGetTasksForCaseCaseNotFoundThrowsCaseNotFoundException() throws Exception {
        var caseReference = 9999L;

        when(dashboardTaskService.getTasks(caseReference)).thenThrow(new CaseNotFoundException(caseReference));

        mockMvc.perform(get("/dashboard/{caseReference}/tasks", caseReference)
                            .header("Authorization", "Bearer valid_token"))
            .andExpect(status().isNotFound());
    }
}
