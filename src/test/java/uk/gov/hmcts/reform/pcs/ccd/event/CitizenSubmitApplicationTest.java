package uk.gov.hmcts.reform.pcs.ccd.event;

import com.google.common.collect.SetMultimap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.ccd.sdk.api.Permission.C;
import static uk.gov.hmcts.ccd.sdk.api.Permission.R;
import static uk.gov.hmcts.ccd.sdk.api.Permission.U;
import static uk.gov.hmcts.reform.pcs.ccd.domain.State.CASE_ISSUED;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.citizenSubmitApplication;

@ExtendWith(MockitoExtension.class)
class CitizenSubmitApplicationTest extends BaseEventTest {

    private Event<PCSCase, UserRole, State> configuredEvent;

    @BeforeEach
    void setUp() {
        CitizenSubmitApplication underTest = new CitizenSubmitApplication();
        configuredEvent = getEvent(citizenSubmitApplication, buildEventConfig(underTest));
    }

    @Test
    void shouldSetEventPermissions() {
        SetMultimap<UserRole, Permission> grants = configuredEvent.getGrants();
        assertThat(grants.keySet()).hasSize(2);
        assertThat(grants.get(UserRole.CREATOR)).contains(C, R, U);
        assertThat(grants.get(UserRole.PCS_CASE_WORKER)).containsOnly(R);
    }

    @Test
    void shouldNotShowEvent() {
        assertThat(configuredEvent.getShowCondition()).isEqualTo(ShowConditions.NEVER_SHOW);
    }

    @Test
    void shouldHavePostStateOfCaseIssued() {
        assertThat(configuredEvent.getPostState()).containsExactly(CASE_ISSUED);
    }

}
