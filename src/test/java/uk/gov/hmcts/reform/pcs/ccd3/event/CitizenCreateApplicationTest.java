package uk.gov.hmcts.reform.pcs.ccd3.event;

import com.google.common.collect.SetMultimap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.Submit;
import uk.gov.hmcts.reform.pcs.ccd3.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd3.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd3.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd3.domain.State;
import uk.gov.hmcts.reform.pcs.ccd3.service.PcsCaseService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.ccd.sdk.api.Permission.C;
import static uk.gov.hmcts.ccd.sdk.api.Permission.R;
import static uk.gov.hmcts.ccd.sdk.api.Permission.U;
import static uk.gov.hmcts.reform.pcs.ccd3.domain.State.AWAITING_SUBMISSION_TO_HMCTS;

@ExtendWith(MockitoExtension.class)
class CitizenCreateApplicationTest extends BaseEventTest {

    @Mock
    private PcsCaseService pcsCaseService;

    private Event<PCSCase, UserRole, State> configuredEvent;

    @BeforeEach
    void setUp() {
        CitizenCreateApplication underTest = new CitizenCreateApplication(pcsCaseService);
        configuredEvent = getEvent(EventId.citizenCreateApplication, buildEventConfig(underTest));
    }

    @Test
    void shouldSetEventPermissions() {
        SetMultimap<UserRole, Permission> grants = configuredEvent.getGrants();
        assertThat(grants.keySet()).hasSize(2);
        assertThat(grants.get(UserRole.CITIZEN)).contains(C, R, U);
        assertThat(grants.get(UserRole.PCS_CASE_WORKER)).containsOnly(R);
    }

    @Test
    void shouldNotShowEvent() {
        assertThat(configuredEvent.getShowCondition()).isEqualTo(ShowConditions.NEVER_SHOW);
    }

    @Test
    void shouldHavePostStateOfAwaitingSubmission() {
        assertThat(configuredEvent.getPostState()).containsExactly(AWAITING_SUBMISSION_TO_HMCTS);
    }

    @Test
    void shouldCreateCaseOnSubmit() {
        long caseReference = 1234L;
        PCSCase caseData = mock(PCSCase.class);
        EventPayload<PCSCase, State> eventPayload = new EventPayload<>(caseReference, caseData, null);

        Submit<PCSCase, State> submitHandler = configuredEvent.getSubmitHandler();
        submitHandler.submit(eventPayload);

        verify(pcsCaseService).createCase(caseReference, caseData);
    }

}
