package uk.gov.hmcts.reform.pcs.dashboard.endpoint;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatusCode;
import uk.gov.hmcts.reform.pcs.dashboard.model.Task;
import uk.gov.hmcts.reform.pcs.dashboard.model.TaskGroup;
import uk.gov.hmcts.reform.pcs.dashboard.service.DashboardNotificationService;
import uk.gov.hmcts.reform.pcs.dashboard.service.DashboardTaskService;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    private DashboardController underTest;

    @Mock
    private DashboardNotificationService dashboardNotificationService;

    @Mock
    private DashboardTaskService dashboardTaskService;

    @BeforeEach
    void setUp() {
        underTest = new DashboardController(dashboardNotificationService, dashboardTaskService);
    }

    @Test
    void shouldReturnEmptyNotificationsList() {
        // Given
        when(dashboardNotificationService.getNotifications(1234L)).thenReturn(List.of());

        // When
        var responseEntity = underTest.getNotificationsForCase(1234L, null);

        // Then
        assertThat(responseEntity.getBody()).isEmpty();
    }

    @Test
    void shouldReturnEmptyListWhenNoTasksFound() {
        // Given
        var caseReference = 9999L;
        var authHeader = "Bearer token";
        when(dashboardTaskService.getTasks(caseReference)).thenReturn(List.of());

        // When
        var response = underTest.getTasksForCase(authHeader, caseReference);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(200));
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void shouldReturnTaskGroupsForCase() {
        // Given
        var caseReference = 5678L;
        var authHeader = "Bearer token";
        
        // Create simplified task groups that match the service structure
        var claimGroup = TaskGroup.builder()
            .groupId("CLAIM")
            .tasks(List.of(
                Task.builder()
                    .templateId("Task.AAA6.Claim.ViewClaim")
                    .templateValues(Collections.emptyMap())
                    .status("AVAILABLE")
                    .build()
            ))
            .build();
            
        var hearingGroup = TaskGroup.builder()
            .groupId("HEARING")
            .tasks(List.of(
                Task.builder()
                    .templateId("Task.AAA6.Hearing.ViewHearing")
                    .templateValues(Collections.emptyMap())
                    .status("AVAILABLE")
                    .build()
            ))
            .build();
            
        var expectedTasks = List.of(claimGroup, hearingGroup);

        when(dashboardTaskService.getTasks(caseReference)).thenReturn(expectedTasks);

        // When
        var response = underTest.getTasksForCase(authHeader, caseReference);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(200));
        assertThat(response.getBody()).isEqualTo(expectedTasks);
    }
}
