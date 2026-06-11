package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import com.google.common.collect.SetMultimap;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CIRCUIT_JUDGE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CITIZEN;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CLAIMANT_SOLICITOR;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CREATOR;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CTSC_ADMIN;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CTSC_TEAM_LEADER;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.DEFENDANT;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.DEFENDANT_SOLICITOR;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.FEE_PAID_JUDGE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.HEARING_CENTRE_ADMIN;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.HEARING_CENTRE_TEAM_LEADER;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.JUDGE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.LEADERSHIP_JUDGE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.WLU_ADMIN;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.WLU_TEAM_LEADER;

class PartyVisibleTabAccessTest {

    private final PartyVisibleTabAccess underTest = new PartyVisibleTabAccess();

    @Test
    void shouldGrantReadAccessToPartyVisibleTabRoles() {
        SetMultimap<HasRole, Permission> grants = underTest.getGrants();

        assertThat(grants.get(CREATOR)).contains(Permission.R);
        assertThat(grants.get(CITIZEN)).contains(Permission.R);
        assertThat(grants.get(DEFENDANT)).contains(Permission.R);
        assertThat(grants.get(CLAIMANT_SOLICITOR)).contains(Permission.R);
        assertThat(grants.get(DEFENDANT_SOLICITOR)).contains(Permission.R);
        assertThat(grants.get(JUDGE)).contains(Permission.R);
        assertThat(grants.get(FEE_PAID_JUDGE)).contains(Permission.R);
        assertThat(grants.get(CIRCUIT_JUDGE)).contains(Permission.R);
        assertThat(grants.get(LEADERSHIP_JUDGE)).contains(Permission.R);
        assertThat(grants.get(HEARING_CENTRE_TEAM_LEADER)).contains(Permission.R);
        assertThat(grants.get(HEARING_CENTRE_ADMIN)).contains(Permission.R);
        assertThat(grants.get(CTSC_TEAM_LEADER)).contains(Permission.R);
        assertThat(grants.get(CTSC_ADMIN)).contains(Permission.R);
        assertThat(grants.get(WLU_TEAM_LEADER)).contains(Permission.R);
        assertThat(grants.get(WLU_ADMIN)).contains(Permission.R);
    }
}
