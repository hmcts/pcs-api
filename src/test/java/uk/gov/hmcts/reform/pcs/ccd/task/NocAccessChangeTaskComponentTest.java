package uk.gov.hmcts.reform.pcs.ccd.task;

import com.github.kagkarlsson.scheduler.task.CompletionHandler;
import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.CustomTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.model.NocAccessChangeTaskData;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseRoleAssignmentService;
import uk.gov.hmcts.reform.pcs.reference.dto.OrganisationDetailsResponse;
import uk.gov.hmcts.reform.pcs.service.LegalRepresentativePartyLinkService;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NocAccessChangeTaskComponentTest {

    private static final int MAX_RETRIES = 3;
    private static final Duration BACKOFF_DELAY = Duration.ofSeconds(10);

    @Mock
    private CaseRoleAssignmentService caseRoleAssignmentService;

    @Mock
    private LegalRepresentativePartyLinkService legalRepresentativePartyLinkService;

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
            caseRoleAssignmentService,
            legalRepresentativePartyLinkService,
            MAX_RETRIES,
            BACKOFF_DELAY
        );
    }

    @Test
    void nocAccessChangeTask() {
        // given
        String partyId = UUID.randomUUID().toString();
        String userId = UUID.randomUUID().toString();
        NocAccessChangeTaskData taskData = NocAccessChangeTaskData.builder()
            .partyId(partyId)
            .organisationDetailsResponse(organisationDetailsResponse)
            .userId(userId)
            .caseReference("1")
            .build();
        when(taskInstance.getData()).thenReturn(taskData);

        CustomTask<NocAccessChangeTaskData> task = nocAccessChangeTaskComponent.nocAccessChangeTask();
        CompletionHandler<NocAccessChangeTaskData> completionHandler =
            task.execute(taskInstance, executionContext);

        assertThat(completionHandler).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
        verify(caseRoleAssignmentService).assignRasRole(1L, userId, UserRole.DEFENDANT_SOLICITOR);
        verify(legalRepresentativePartyLinkService).linkLegalRepresentativeToParty(1L, partyId,
                                                                                   UUID.fromString(userId),
                                                                                   organisationDetailsResponse);
    }
}
