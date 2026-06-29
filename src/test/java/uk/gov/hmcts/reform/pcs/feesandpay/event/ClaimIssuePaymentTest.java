package uk.gov.hmcts.reform.pcs.feesandpay.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CTSC_ADMIN;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CTSC_TEAM_LEADER;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.HEARING_CENTRE_ADMIN;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.HEARING_CENTRE_TEAM_LEADER;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.JUDGE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.LEADERSHIP_JUDGE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.WLU_ADMIN;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.WLU_TEAM_LEADER;

@ExtendWith(MockitoExtension.class)
class ClaimIssuePaymentTest extends BaseEventTest {

    @Mock
    private PcsCaseService pcsCaseService;

    @BeforeEach
    void setUp() {
        setEventUnderTest(new ClaimIssuePayment(pcsCaseService));
    }

    @Test
    void shouldGrantReadAccessToInternalServiceRequestRoles() {
        assertThat(configuredEvent.getGrants().get(CTSC_ADMIN)).contains(Permission.R);
        assertThat(configuredEvent.getGrants().get(CTSC_TEAM_LEADER)).contains(Permission.R);
        assertThat(configuredEvent.getGrants().get(HEARING_CENTRE_ADMIN)).contains(Permission.R);
        assertThat(configuredEvent.getGrants().get(HEARING_CENTRE_TEAM_LEADER)).contains(Permission.R);
        assertThat(configuredEvent.getGrants().get(JUDGE)).contains(Permission.R);
        assertThat(configuredEvent.getGrants().get(LEADERSHIP_JUDGE)).contains(Permission.R);
        assertThat(configuredEvent.getGrants().get(WLU_ADMIN)).contains(Permission.R);
        assertThat(configuredEvent.getGrants().get(WLU_TEAM_LEADER)).contains(Permission.R);
    }

    @Test
    void shouldSetCaseIssuedStateOnSubmit() {
        PCSCase pcsCase = PCSCase.builder().build();

        SubmitResponse<State> response = callSubmitHandler(pcsCase);

        assertThat(response.getState()).isEqualTo(State.CASE_ISSUED);
    }
}
