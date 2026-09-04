package uk.gov.hmcts.reform.pcs.ccd.task;

import com.github.kagkarlsson.scheduler.task.CompletionHandler;
import com.github.kagkarlsson.scheduler.task.Execution;
import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.CustomTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.model.NocAccessChangeTaskData;
import uk.gov.hmcts.reform.pcs.noc.service.NoticeOfChangeAppliedEventService;
import uk.gov.hmcts.reform.pcs.reference.dto.OrganisationDetailsResponse;
import uk.gov.hmcts.reform.pcs.service.LegalRepresentativePartyLinkService;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NocAccessChangeTaskComponentTest {

    private static final int MAX_RETRIES = 3;
    private static final Duration BACKOFF_DELAY = Duration.ofSeconds(10);

    @Mock
    private LegalRepresentativePartyLinkService legalRepresentativePartyLinkService;

    @Mock
    private NoticeOfChangeAppliedEventService noticeOfChangeAppliedEventService;

    @Mock
    private TaskInstance<NocAccessChangeTaskData> taskInstance;

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private OrganisationDetailsResponse organisationDetailsResponse;

    private NocAccessChangeTaskComponent nocAccessChangeTaskComponent;

    @BeforeEach
    void setUp() {
        nocAccessChangeTaskComponent = new NocAccessChangeTaskComponent(
            legalRepresentativePartyLinkService,
            noticeOfChangeAppliedEventService,
            MAX_RETRIES,
            BACKOFF_DELAY
        );
    }

    @Test
    void nocAccessChangeTask() {
        // given
        String partyId = UUID.randomUUID().toString();
        String userId = UUID.randomUUID().toString();
        String email = "solicitor@example.com";
        NocAccessChangeTaskData taskData = NocAccessChangeTaskData.builder()
            .partyId(partyId)
            .organisationDetailsResponse(organisationDetailsResponse)
            .userId(userId)
            .email(email)
            .caseReference("1")
            .build();
        when(taskInstance.getData()).thenReturn(taskData);

        // when
        CustomTask<NocAccessChangeTaskData> task = nocAccessChangeTaskComponent.nocAccessChangeTask();
        CompletionHandler<NocAccessChangeTaskData> completionHandler =
            task.execute(taskInstance, executionContext);

        // then - access is granted through the case access groups, so the task only links the representative
        assertThat(completionHandler).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
        verify(legalRepresentativePartyLinkService).linkLegalRepresentativeToParty(1L, partyId,
                                                                                   email,
                                                                                   organisationDetailsResponse);
        // the rep change feeds derived CaseAccessGroups; only an event stores a fresh indexed snapshot
        verify(noticeOfChangeAppliedEventService).submit(1L, email);
    }

    @Test
    void nocAccessChangeTask_shouldLogErrorAndThrowException_whenServiceFails() {
        // given
        String partyId = UUID.randomUUID().toString();
        String userId = UUID.randomUUID().toString();
        String email = "solicitor@example.com";
        NocAccessChangeTaskData taskData = NocAccessChangeTaskData.builder()
            .partyId(partyId)
            .organisationDetailsResponse(organisationDetailsResponse)
            .userId(userId)
            .email(email)
            .caseReference("1")
            .build();

        when(taskInstance.getData()).thenReturn(taskData);

        Execution execution = mock(Execution.class);
        when(executionContext.getExecution()).thenReturn(execution);

        RuntimeException expectedException = new RuntimeException("Database error or service unavailable");
        doThrow(expectedException).when(legalRepresentativePartyLinkService)
            .linkLegalRepresentativeToParty(1L, partyId, email, organisationDetailsResponse);

        CustomTask<NocAccessChangeTaskData> task = nocAccessChangeTaskComponent.nocAccessChangeTask();

        // when & then - the failure propagates so db-scheduler can retry
        assertThatThrownBy(() -> task.execute(taskInstance, executionContext))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Database error or service unavailable");

        verify(legalRepresentativePartyLinkService).linkLegalRepresentativeToParty(1L, partyId,
                                                                                   email,
                                                                                   organisationDetailsResponse);
        verifyNoInteractions(noticeOfChangeAppliedEventService);
    }
}
