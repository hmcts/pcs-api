package uk.gov.hmcts.reform.pcs.ccd.task;

import com.github.kagkarlsson.scheduler.task.CompletionHandler;
import com.github.kagkarlsson.scheduler.task.Execution;
import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.CustomTask;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.model.DeleteDraftClaimRoleRevocationTaskData;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseRoleAssignmentService;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.task.DeleteDraftClaimRoleRevocationTaskComponent.DELETE_DRAFT_CLAIM_ROLE_REVOCATION_TASK_DESCRIPTOR;

@ExtendWith(MockitoExtension.class)
class DeleteDraftClaimRoleRevocationTaskComponentTest {

    private DeleteDraftClaimRoleRevocationTaskComponent underTest;

    @Mock
    private CaseRoleAssignmentService caseRoleAssignmentService;
    @Mock
    private TaskInstance<DeleteDraftClaimRoleRevocationTaskData> taskInstance;
    @Mock
    private ExecutionContext executionContext;
    @Mock
    private Execution execution;

    @BeforeEach
    void setUp() {
        underTest = new DeleteDraftClaimRoleRevocationTaskComponent(
            caseRoleAssignmentService,
            5,
            Duration.ofSeconds(3)
        );
    }

    @Test
    void shouldCreateTaskDescriptorWithCorrectNameAndType() {
        assertThat(DELETE_DRAFT_CLAIM_ROLE_REVOCATION_TASK_DESCRIPTOR.getTaskName())
            .isEqualTo("delete-draft-claim-role-revocation-task");
        assertThat(DELETE_DRAFT_CLAIM_ROLE_REVOCATION_TASK_DESCRIPTOR.getDataClass())
            .isEqualTo(DeleteDraftClaimRoleRevocationTaskData.class);
    }

    @Test
    void shouldRevokeDraftClaimCaseRoles() {
        DeleteDraftClaimRoleRevocationTaskData data = DeleteDraftClaimRoleRevocationTaskData.builder()
            .caseReference("1234")
            .userId("user-abc")
            .build();

        when(taskInstance.getData()).thenReturn(data);

        CustomTask<DeleteDraftClaimRoleRevocationTaskData> task = underTest.deleteDraftClaimRoleRevocationTask();
        CompletionHandler<DeleteDraftClaimRoleRevocationTaskData> result = task.execute(taskInstance, executionContext);

        verify(caseRoleAssignmentService).revokeRasRole(1234L, "user-abc", UserRole.CLAIMANT_SOLICITOR);
        verify(caseRoleAssignmentService).revokeRasRole(1234L, "user-abc", UserRole.CREATOR);
        assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
    }

    @Test
    void shouldRethrowExceptionWhenRoleRevocationFails() {
        DeleteDraftClaimRoleRevocationTaskData data = DeleteDraftClaimRoleRevocationTaskData.builder()
            .caseReference("1234")
            .userId("user-abc")
            .build();

        when(taskInstance.getData()).thenReturn(data);
        when(executionContext.getExecution()).thenReturn(execution);
        doThrow(mock(RuntimeException.class)).when(caseRoleAssignmentService)
            .revokeRasRole(1234L, "user-abc", UserRole.CLAIMANT_SOLICITOR);

        CustomTask<DeleteDraftClaimRoleRevocationTaskData> task = underTest.deleteDraftClaimRoleRevocationTask();

        assertThatThrownBy(() -> task.execute(taskInstance, executionContext))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldStillRevokeCreatorRoleWhenClaimantSolicitorRoleDoesNotExist() {
        DeleteDraftClaimRoleRevocationTaskData data = DeleteDraftClaimRoleRevocationTaskData.builder()
            .caseReference("1234")
            .userId("user-abc")
            .build();

        when(taskInstance.getData()).thenReturn(data);
        doThrow(mock(FeignException.NotFound.class)).when(caseRoleAssignmentService)
            .revokeRasRole(1234L, "user-abc", UserRole.CLAIMANT_SOLICITOR);

        CustomTask<DeleteDraftClaimRoleRevocationTaskData> task = underTest.deleteDraftClaimRoleRevocationTask();

        CompletionHandler<DeleteDraftClaimRoleRevocationTaskData> result = task.execute(taskInstance, executionContext);

        verify(caseRoleAssignmentService).revokeRasRole(1234L, "user-abc", UserRole.CREATOR);
        assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
    }
}
