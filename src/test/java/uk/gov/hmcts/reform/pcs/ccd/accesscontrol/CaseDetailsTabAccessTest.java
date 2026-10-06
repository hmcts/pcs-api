package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import com.google.common.collect.SetMultimap;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import static org.assertj.core.api.Assertions.assertThat;

class CaseDetailsTabAccessTest {

    private final CaseDetailsTabAccess underTest = new CaseDetailsTabAccess();

    @Test
    void shouldGrantPartyVisibleRolesAndTheSystemUserRead() {
        SetMultimap<HasRole, Permission> grants = underTest.getGrants();

        for (UserRole role : AccessGrants.PARTY_VISIBLE_ROLES) {
            assertThat(grants.get(role)).contains(Permission.R);
        }
        assertThat(grants.get(UserRole.SYSTEM_USER)).containsExactly(Permission.R);
    }
}
