package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CTSC_ADMIN;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CTSC_TEAM_LEADER;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.WLU_ADMIN;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.WLU_TEAM_LEADER;

class JudicialHistoryRolesTest {

    @Test
    void shouldGrantHistoryRowsToInternalAdminRolesWithHistoryTabAccess() {
        assertThat(JUDICIAL_HISTORY_ROLES)
            .contains(CTSC_TEAM_LEADER, CTSC_ADMIN, WLU_TEAM_LEADER, WLU_ADMIN);
    }
}
