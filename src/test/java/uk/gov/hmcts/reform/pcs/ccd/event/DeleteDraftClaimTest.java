package uk.gov.hmcts.reform.pcs.ccd.event;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.SchedulableInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.model.DeleteDraftClaimRoleRevocationTaskData;
import uk.gov.hmcts.reform.pcs.ccd.model.DeleteDraftClaimTaskData;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseRoleAssignmentService;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftClaimDeletionService;
import uk.gov.hmcts.reform.pcs.ccd.task.DeleteDraftClaimRoleRevocationTaskComponent;
import uk.gov.hmcts.reform.pcs.ccd.task.DeleteDraftClaimTaskComponent;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteDraftClaimTest extends BaseEventTest {

    private static final String USER_ID = "user-id";

    @Mock
    private SchedulerClient schedulerClient;
    @Mock
    private SecurityContextService securityContextService;
    @Mock
    private CaseRoleAssignmentService caseRoleAssignmentService;
    @Mock
    private DraftClaimDeletionService draftClaimDeletionService;

    @BeforeEach
    void setUp() {
        setEventUnderTest(new DeleteDraftClaim(
            schedulerClient,
            securityContextService,
            caseRoleAssignmentService,
            draftClaimDeletionService
        ));
    }

    @Test
    void shouldOnlyGrantDeleteDraftClaimEventToCreator() {
        assertThat(configuredEvent.getGrants().keySet()).containsOnly(UserRole.CREATOR);
        assertThat(configuredEvent.getGrants().get(UserRole.CREATOR)).isEqualTo(Permission.CRUD);
    }

    @Test
    void shouldNotDeleteDraftClaimWhenUserSelectsNo() {
        SubmitResponse<State> response = callSubmitHandler(PCSCase.builder()
            .deleteDraftClaim(YesOrNo.NO)
            .build());

        verify(schedulerClient, never()).scheduleIfNotExists(any());
        assertThat(response.getState()).isNull();
    }

    @Test
    void shouldScheduleCaseListAccessRemovalAndDraftClaimDeletionWhenUserSelectsYes() {
        UserInfo userDetails = mock(UserInfo.class);
        when(securityContextService.getCurrentUserDetails()).thenReturn(userDetails);
        when(userDetails.getUid()).thenReturn(USER_ID);

        SubmitResponse<State> response = callSubmitHandler(PCSCase.builder()
            .deleteDraftClaim(YesOrNo.YES)
            .build());

        DeleteDraftClaimTaskData taskData = getCapturedDeleteDraftClaimTaskData();
        DeleteDraftClaimRoleRevocationTaskData roleRevocationTaskData = getCapturedRoleRevocationTaskData();
        assertThat(taskData.getCaseReference()).isEqualTo(String.valueOf(TEST_CASE_REFERENCE));
        assertThat(roleRevocationTaskData.getCaseReference()).isEqualTo(String.valueOf(TEST_CASE_REFERENCE));
        assertThat(roleRevocationTaskData.getUserId()).isEqualTo(USER_ID);
        assertThat(response.getState()).isEqualTo(State.DELETED);
        assertThat(response.getConfirmationBody()).contains("Case deleted");
    }

    @SuppressWarnings("unchecked")
    private DeleteDraftClaimTaskData getCapturedDeleteDraftClaimTaskData() {
        ArgumentCaptor<SchedulableInstance<?>> captor = ArgumentCaptor.forClass(SchedulableInstance.class);
        verify(schedulerClient, times(2)).scheduleIfNotExists(captor.capture());

        return captor.getAllValues().stream()
            .filter(t -> t.getTaskInstance().getTaskName()
                .equals(DeleteDraftClaimTaskComponent.DELETE_DRAFT_CLAIM_TASK_DESCRIPTOR.getTaskName()))
            .map(SchedulableInstance::getTaskInstance)
            .map(taskInstance -> taskInstance.getData())
            .map(DeleteDraftClaimTaskData.class::cast)
            .findFirst()
            .orElseThrow(() -> new AssertionError("No delete draft claim task found"));
    }

    @SuppressWarnings("unchecked")
    private DeleteDraftClaimRoleRevocationTaskData getCapturedRoleRevocationTaskData() {
        ArgumentCaptor<SchedulableInstance<?>> captor = ArgumentCaptor.forClass(SchedulableInstance.class);
        verify(schedulerClient, times(2)).scheduleIfNotExists(captor.capture());

        return captor.getAllValues().stream()
            .filter(t -> t.getTaskInstance().getTaskName()
                .equals(DeleteDraftClaimRoleRevocationTaskComponent
                            .DELETE_DRAFT_CLAIM_ROLE_REVOCATION_TASK_DESCRIPTOR.getTaskName()))
            .map(SchedulableInstance::getTaskInstance)
            .map(taskInstance -> taskInstance.getData())
            .map(DeleteDraftClaimRoleRevocationTaskData.class::cast)
            .findFirst()
            .orElseThrow(() -> new AssertionError("No delete draft claim role revocation task found"));
    }
}
