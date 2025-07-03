package uk.gov.hmcts.reform.pcs.ccd.event;

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
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.GACase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.GACaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.GeneralApplicationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.sdk.api.Permission.C;
import static uk.gov.hmcts.ccd.sdk.api.Permission.D;
import static uk.gov.hmcts.ccd.sdk.api.Permission.R;
import static uk.gov.hmcts.ccd.sdk.api.Permission.U;

@ExtendWith(MockitoExtension.class)
class UpdateGeneralApplicationTest extends GABaseEventTest {

    @Mock
    private GeneralApplicationService gaService;

    private Event<GACase, UserRole, State> configuredEvent;

    @BeforeEach
    void setUp() {
        UpdateGeneralApplication underTest = new UpdateGeneralApplication(gaService);
        configuredEvent = getEvent(EventId.updateGeneralApplication, buildEventConfig(underTest));
    }

    @Test
    void shouldSetEventPermissions() {
        SetMultimap<UserRole, Permission> grants = configuredEvent.getGrants();
        assertThat(grants.keySet()).hasSize(1);
        assertThat(grants.get(UserRole.PCS_CASE_WORKER)).contains(C,R,U,D);
    }

    @Test
    void shouldHaveNameAndStateTransition() {
        assertThat(configuredEvent.getName()).isEqualTo("Withdraw Draft Gen App");
        assertThat(configuredEvent.getPreState()).containsExactly(State.DRAFT);
        assertThat(configuredEvent.getPostState()).containsExactly(State.DRAFT_WITHDRAWN);
    }

    @Test
    void shouldUpdateCaseStatusOnSubmit() {
        long caseReference = 1234L;
        GACase caseData = mock(GACase.class);
        EventPayload<GACase, State> eventPayload = new EventPayload<>(caseReference, caseData, null);

        GACaseEntity gaEntity = mock(GACaseEntity.class);
        when(gaService.findByCaseReference(caseReference)).thenReturn(gaEntity);

        Submit<GACase, State> submitHandler = configuredEvent.getSubmitHandler();
        submitHandler.submit(eventPayload);

        verify(gaEntity).setStatus(State.DRAFT_WITHDRAWN);

        verify(gaService).saveGaApp(gaEntity);
    }


}

