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
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.model.DeleteDraftClaimTaskData;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseRoleAssignmentService;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftClaimDeletionService;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.task.DeleteDraftClaimTaskComponent.DELETE_DRAFT_CLAIM_TASK_DESCRIPTOR;

@ExtendWith(MockitoExtension.class)
class DeleteDraftClaimTaskComponentTest {

    private DeleteDraftClaimTaskComponent underTest;

    @Mock
    private CaseRoleAssignmentService caseRoleAssignmentService;
    @Mock
    private DraftClaimDeletionService draftClaimDeletionService;
    @Mock
    private TaskInstance<DeleteDraftClaimTaskData> taskInstance;
    @Mock
    private ExecutionContext executionContext;
    @Mock
    private Execution execution;

    @BeforeEach
    void setUp() {
        underTest = new DeleteDraftClaimTaskComponent(
            caseRoleAssignmentService,
            draftClaimDeletionService,
            5,
            Duration.ofSeconds(3)
        );
    }

    @Test
    void shouldCreateTaskDescriptorWithCorrectNameAndType() {
        assertThat(DELETE_DRAFT_CLAIM_TASK_DESCRIPTOR.getTaskName())
            .isEqualTo("delete-draft-claim-task");
        assertThat(DELETE_DRAFT_CLAIM_TASK_DESCRIPTOR.getDataClass())
            .isEqualTo(DeleteDraftClaimTaskData.class);
    }

    @Test
    void shouldDeleteDraftClaim() {
        DeleteDraftClaimTaskData data = DeleteDraftClaimTaskData.builder()
            .caseReference("1234")
            .userId("user-abc")
            .build();

        when(taskInstance.getData()).thenReturn(data);

        CustomTask<DeleteDraftClaimTaskData> task = underTest.deleteDraftClaimTask();
        CompletionHandler<DeleteDraftClaimTaskData> result = task.execute(taskInstance, executionContext);

        InOrder inOrder = inOrder(caseRoleAssignmentService, draftClaimDeletionService);
        inOrder.verify(caseRoleAssignmentService).revokeRasRole(1234L, "user-abc", UserRole.CLAIMANT_SOLICITOR);
        inOrder.verify(caseRoleAssignmentService).revokeRasRole(1234L, "user-abc", UserRole.CREATOR);
        inOrder.verify(draftClaimDeletionService).deleteDraftClaim(1234L);
        assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
    }

    @Test
    void shouldRethrowExceptionWhenDraftClaimDeletionFails() {
        DeleteDraftClaimTaskData data = DeleteDraftClaimTaskData.builder()
            .caseReference("1234")
            .userId("user-abc")
            .build();

        when(taskInstance.getData()).thenReturn(data);
        when(executionContext.getExecution()).thenReturn(execution);
        doThrow(mock(RuntimeException.class)).when(draftClaimDeletionService).deleteDraftClaim(1234L);

        CustomTask<DeleteDraftClaimTaskData> task = underTest.deleteDraftClaimTask();

        assertThatThrownBy(() -> task.execute(taskInstance, executionContext))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldStillDeleteDraftClaimWhenCaseNoLongerExistsDuringRoleRevocation() {
        DeleteDraftClaimTaskData data = DeleteDraftClaimTaskData.builder()
            .caseReference("1234")
            .userId("user-abc")
            .build();

        when(taskInstance.getData()).thenReturn(data);
        doThrow(mock(FeignException.NotFound.class)).when(caseRoleAssignmentService)
            .revokeRasRole(1234L, "user-abc", UserRole.CLAIMANT_SOLICITOR);

        CustomTask<DeleteDraftClaimTaskData> task = underTest.deleteDraftClaimTask();
        CompletionHandler<DeleteDraftClaimTaskData> result = task.execute(taskInstance, executionContext);

        verify(caseRoleAssignmentService, never()).revokeRasRole(1234L, "user-abc", UserRole.CREATOR);
        verify(draftClaimDeletionService).deleteDraftClaim(1234L);
        assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
    }
}
