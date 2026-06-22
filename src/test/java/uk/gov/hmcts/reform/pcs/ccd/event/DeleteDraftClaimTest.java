package uk.gov.hmcts.reform.pcs.ccd.event;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.SchedulableInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.model.DeleteDraftClaimTaskData;
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
import static uk.gov.hmcts.ccd.sdk.api.Permission.C;
import static uk.gov.hmcts.ccd.sdk.api.Permission.D;
import static uk.gov.hmcts.ccd.sdk.api.Permission.R;
import static uk.gov.hmcts.ccd.sdk.api.Permission.U;

@ExtendWith(MockitoExtension.class)
class DeleteDraftClaimTest extends BaseEventTest {

    private static final String USER_ID = "user-id";

    @Mock
    private SchedulerClient schedulerClient;
    @Mock
    private SecurityContextService securityContextService;

    @BeforeEach
    void setUp() {
        setEventUnderTest(new DeleteDraftClaim(
            schedulerClient,
            securityContextService
        ));
    }

    @Test
    void shouldConfigureDeleteDraftClaimEventForDraftStatesAndCreatorOrClaimantSolicitor() {
        assertThat(configuredEvent.getPreState())
            .containsExactlyInAnyOrder(State.AWAITING_SUBMISSION_TO_HMCTS, State.PENDING_CASE_ISSUED);
        assertThat(configuredEvent.getGrants().get(UserRole.CREATOR)).containsExactlyInAnyOrder(C, R, U, D);
        assertThat(configuredEvent.getGrants().get(UserRole.CLAIMANT_SOLICITOR)).containsExactlyInAnyOrder(C, R, U, D);
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
    void shouldScheduleOrderedDraftClaimDeletionWhenUserSelectsYes() {
        UserInfo userDetails = mock(UserInfo.class);
        when(securityContextService.getCurrentUserDetails()).thenReturn(userDetails);
        when(userDetails.getUid()).thenReturn(USER_ID);

        SubmitResponse<State> response = callSubmitHandler(PCSCase.builder()
            .deleteDraftClaim(YesOrNo.YES)
            .build());

        DeleteDraftClaimTaskData taskData = getCapturedDeleteDraftClaimTaskData();
        assertThat(taskData.getCaseReference()).isEqualTo(String.valueOf(TEST_CASE_REFERENCE));
        assertThat(taskData.getUserId()).isEqualTo(USER_ID);
        assertThat(response.getState()).isEqualTo(State.DELETED);
        assertThat(response.getConfirmationBody()).contains("Case deleted");
    }

    @SuppressWarnings("unchecked")
    private DeleteDraftClaimTaskData getCapturedDeleteDraftClaimTaskData() {
        ArgumentCaptor<SchedulableInstance<?>> captor = ArgumentCaptor.forClass(SchedulableInstance.class);
        verify(schedulerClient, times(1)).scheduleIfNotExists(captor.capture());

        return captor.getAllValues().stream()
            .filter(t -> t.getTaskInstance().getTaskName()
                .equals(DeleteDraftClaimTaskComponent.DELETE_DRAFT_CLAIM_TASK_DESCRIPTOR.getTaskName()))
            .map(SchedulableInstance::getTaskInstance)
            .map(taskInstance -> taskInstance.getData())
            .map(DeleteDraftClaimTaskData.class::cast)
            .findFirst()
            .orElseThrow(() -> new AssertionError("No delete draft claim task found"));
    }
}
