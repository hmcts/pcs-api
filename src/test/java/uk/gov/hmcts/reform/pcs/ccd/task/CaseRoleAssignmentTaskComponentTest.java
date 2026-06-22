package uk.gov.hmcts.reform.pcs.ccd.task;

import com.github.kagkarlsson.scheduler.task.CompletionHandler;
import com.github.kagkarlsson.scheduler.task.Execution;
import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.CustomTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.model.RoleAssignmentTaskData;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.CcdCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseRoleAssignmentService;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.task.CaseRoleAssignmentTaskComponent.ROLE_ASSIGNMENT_TASK_DESCRIPTOR;

@ExtendWith(MockitoExtension.class)
class CaseRoleAssignmentTaskComponentTest {

    private CaseRoleAssignmentTaskComponent caseRoleAssignmentTaskComponent;

    @Mock
    private CaseRoleAssignmentService caseRoleAssignmentService;
    @Mock
    private PcsCaseRepository pcsCaseRepository;
    @Mock
    private CcdCaseDataService ccdCaseDataService;

    @Mock
    private TaskInstance<RoleAssignmentTaskData> taskInstance;

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private Execution execution;

    private final Duration backoffDelay = Duration.ofSeconds(3);

    @BeforeEach
    void setUp() {
        int maxRetries = 5;
        caseRoleAssignmentTaskComponent = new CaseRoleAssignmentTaskComponent(
            caseRoleAssignmentService,
            pcsCaseRepository,
            ccdCaseDataService,
            maxRetries,
            backoffDelay
        );
    }

    @Test
    @DisplayName("Should create task descriptor with correct name and type")
    void shouldCreateTaskDescriptorWithCorrectNameAndType() {
        assertThat(ROLE_ASSIGNMENT_TASK_DESCRIPTOR.getTaskName())
            .isEqualTo("role-assignment-task");
        assertThat(ROLE_ASSIGNMENT_TASK_DESCRIPTOR.getDataClass())
            .isEqualTo(RoleAssignmentTaskData.class);
    }

    @Test
    @DisplayName("Should assign claimant solicitor role and revoke creator role on execution")
    void shouldExecuteTaskAndCallCaseRoleAssignmentService() {
        // Given
        RoleAssignmentTaskData data = RoleAssignmentTaskData.builder()
            .caseReference("1234")
            .userId("user-abc")
            .build();

        when(taskInstance.getData()).thenReturn(data);
        when(ccdCaseDataService.isCaseDeletedOrMissing(1234L)).thenReturn(false);
        when(pcsCaseRepository.findByCaseReference(1234L)).thenReturn(Optional.of(mock(PcsCaseEntity.class)));
        CustomTask<RoleAssignmentTaskData> task = caseRoleAssignmentTaskComponent.roleAssignmentTask();

        // When
        CompletionHandler<RoleAssignmentTaskData> result = task.execute(taskInstance, executionContext);

        // Then
        verify(caseRoleAssignmentService).assignRasRole(1234L, "user-abc", UserRole.CLAIMANT_SOLICITOR);
        verify(caseRoleAssignmentService).revokeRasRole(1234L, "user-abc", UserRole.CREATOR);
        assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
    }

    @Test
    @DisplayName("Should skip role assignment when case has already been deleted")
    void shouldSkipRoleAssignmentWhenCaseHasAlreadyBeenDeleted() {
        // Given
        RoleAssignmentTaskData data = RoleAssignmentTaskData.builder()
            .caseReference("1234")
            .userId("user-abc")
            .build();

        when(taskInstance.getData()).thenReturn(data);
        when(ccdCaseDataService.isCaseDeletedOrMissing(1234L)).thenReturn(true);

        CustomTask<RoleAssignmentTaskData> task = caseRoleAssignmentTaskComponent.roleAssignmentTask();

        // When
        CompletionHandler<RoleAssignmentTaskData> result = task.execute(taskInstance, executionContext);

        // Then
        verify(caseRoleAssignmentService, never()).assignRasRole(1234L, "user-abc", UserRole.CLAIMANT_SOLICITOR);
        verify(caseRoleAssignmentService, never()).revokeRasRole(1234L, "user-abc", UserRole.CREATOR);
        assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
    }

    @Test
    @DisplayName("Should rethrow exception when assign role fails")
    void shouldRethrowExceptionWhenAssignRoleFails() {
        // Given
        RoleAssignmentTaskData data = RoleAssignmentTaskData.builder()
            .caseReference("1234")
            .userId("user-abc")
            .build();

        when(taskInstance.getData()).thenReturn(data);
        when(ccdCaseDataService.isCaseDeletedOrMissing(1234L)).thenReturn(false);
        when(pcsCaseRepository.findByCaseReference(1234L)).thenReturn(Optional.of(mock(PcsCaseEntity.class)));
        when(executionContext.getExecution()).thenReturn(execution);
        doThrow(mock(RuntimeException.class)).when(caseRoleAssignmentService)
            .assignRasRole(1234L, "user-abc", UserRole.CLAIMANT_SOLICITOR);

        CustomTask<RoleAssignmentTaskData> task = caseRoleAssignmentTaskComponent.roleAssignmentTask();

        // When / Then
        assertThatThrownBy(() -> task.execute(taskInstance, executionContext))
            .isInstanceOf(RuntimeException.class);
    }
}
