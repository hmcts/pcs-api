package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import com.google.common.collect.SetMultimap;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import static org.assertj.core.api.Assertions.assertThat;

class PartyVisibleTabAccessTest {

    private final PartyVisibleTabAccess underTest = new PartyVisibleTabAccess();

    @Test
    void shouldGrantReadAccessToPartyVisibleTabRoles() {
        SetMultimap<HasRole, Permission> grants = underTest.getGrants();

        for (UserRole role : AccessGrants.PARTY_VISIBLE_ROLES) {
            assertThat(grants.get(role)).contains(Permission.R);
        }
    }
}
