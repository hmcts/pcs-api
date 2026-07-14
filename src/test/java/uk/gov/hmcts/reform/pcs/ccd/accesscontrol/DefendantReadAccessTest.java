package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import com.google.common.collect.SetMultimap;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import static org.assertj.core.api.Assertions.assertThat;

class DefendantReadAccessTest {

    private final DefendantReadAccess underTest = new DefendantReadAccess();

    @Test
    void shouldGrantReadOnlyAccessToDefendantRoles() {
        SetMultimap<HasRole, Permission> grants = underTest.getGrants();

        assertThat(grants.get(UserRole.DEFENDANT)).containsExactly(Permission.R);
        assertThat(grants.get(UserRole.DEFENDANT_SOLICITOR)).containsExactly(Permission.R);
    }
}
