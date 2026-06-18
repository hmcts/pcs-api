package uk.gov.hmcts.reform.pcs.feesandpay.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CTSC_ADMIN;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CTSC_TEAM_LEADER;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.HEARING_CENTRE_ADMIN;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.HEARING_CENTRE_TEAM_LEADER;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.JUDGE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.LEADERSHIP_JUDGE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.WLU_ADMIN;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.WLU_TEAM_LEADER;

class ClaimIssuePaymentTest extends BaseEventTest {

    @BeforeEach
    void setUp() {
        setEventUnderTest(new ClaimIssuePayment());
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
}
