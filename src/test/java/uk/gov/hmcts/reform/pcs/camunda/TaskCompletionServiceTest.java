package uk.gov.hmcts.reform.pcs.camunda;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.pcs.camunda.TaskManagementApi.SearchParameter;
import uk.gov.hmcts.reform.pcs.camunda.TaskManagementApi.Task;
import uk.gov.hmcts.reform.pcs.camunda.TaskManagementApi.TaskSearchRequest;
import uk.gov.hmcts.reform.pcs.camunda.TaskManagementApi.TaskSearchResponse;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskCompletionServiceTest {

    private static final long CASE_REFERENCE = 1234L;
    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final String USER = "Bearer user";
    private static final String SERVICE = "Bearer service";

    @Mock
    private TaskManagementApi taskManagementApi;
    @Mock
    private AuthTokenGenerator authTokenGenerator;
    @Mock
    private SecurityContextService securityContextService;
    @Mock
    private FeatureToggleService featureToggleService;

    @InjectMocks
    private TaskCompletionService underTest;

    @BeforeEach
    void setUp() {
        when(featureToggleService.isEnabled(FeatureFlag.CASEWORKER_WA)).thenReturn(true);
    }

    @Test
    void completesTheUsersOpenTasksAboutTheOrderAsThem() {
        signedIn();
        ArgumentCaptor<TaskSearchRequest> search = ArgumentCaptor.forClass(TaskSearchRequest.class);
        when(taskManagementApi.searchTasks(eq(USER), eq(SERVICE), eq(0), eq(50), search.capture()))
            .thenReturn(new TaskSearchResponse(List.of(
                new Task("task-for-the-order", "ConfirmOrderReview", Map.of("orderId", ORDER_ID.toString())),
                new Task("task-for-another-order", "ConfirmOrderReview",
                         Map.of("orderId", UUID.randomUUID().toString())),
                new Task("task-without-an-order", "ConfirmOrderReview", null))));

        boolean completed = underTest.completeOrderTasks(CASE_REFERENCE, TaskType.CONFIRM_ORDER_REVIEW, ORDER_ID);

        assertThat(completed).isTrue();
        verify(taskManagementApi).completeTask(USER, SERVICE, "task-for-the-order", Map.of());
        verify(taskManagementApi, never()).completeTask(any(), any(), eq("task-for-another-order"), any());
        verify(taskManagementApi, never()).completeTask(any(), any(), eq("task-without-an-order"), any());
        assertThat(search.getValue().searchParameters()).containsExactly(
            new SearchParameter("jurisdiction", "IN", List.of("PCS")),
            new SearchParameter("case_id", "IN", List.of("1234")),
            new SearchParameter("task_type", "IN", List.of("ConfirmOrderReview")),
            new SearchParameter("state", "IN", List.of("UNASSIGNED", "ASSIGNED")));
    }

    @Test
    void saysWhenThereWasNoTaskToComplete() {
        signedIn();
        when(taskManagementApi.searchTasks(anyString(), anyString(), eq(0), eq(50), any()))
            .thenReturn(new TaskSearchResponse(List.of()));

        assertThat(underTest.completeOrderTasks(CASE_REFERENCE, TaskType.REVIEW_ORDER_QUERY, ORDER_ID)).isFalse();
        verify(taskManagementApi, never()).completeTask(any(), any(), any(), any());
    }

    @Test
    void leavesTheTaskToBeCancelledWhenTaskManagementFails() {
        signedIn();
        when(taskManagementApi.searchTasks(anyString(), anyString(), eq(0), eq(50), any()))
            .thenReturn(new TaskSearchResponse(List.of(
                new Task("task-for-the-order", "ConfirmOrderReview", Map.of("orderId", ORDER_ID.toString())))));
        doThrow(new IllegalStateException("task management is down"))
            .when(taskManagementApi).completeTask(any(), any(), any(), any());

        assertThat(underTest.completeOrderTasks(CASE_REFERENCE, TaskType.CONFIRM_ORDER_REVIEW, ORDER_ID)).isFalse();
    }

    @Test
    void doesNothingWithoutWorkAllocation() {
        when(featureToggleService.isEnabled(FeatureFlag.CASEWORKER_WA)).thenReturn(false);

        assertThat(underTest.completeOrderTasks(CASE_REFERENCE, TaskType.CONFIRM_ORDER_REVIEW, ORDER_ID)).isTrue();
        verifyNoInteractions(taskManagementApi);
    }

    private void signedIn() {
        when(securityContextService.getCurrentUserAuthToken()).thenReturn(USER);
        when(authTokenGenerator.generate()).thenReturn(SERVICE);
    }
}
