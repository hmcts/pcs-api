package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import com.google.common.collect.SetMultimap;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import static org.assertj.core.api.Assertions.assertThat;

class CitizenReadAccessTest {

    private final CitizenReadAccess underTest = new CitizenReadAccess();

    @Test
    void shouldGrantReadOnlyAccessToCitizen() {
        SetMultimap<HasRole, Permission> grants = underTest.getGrants();

        assertThat(grants.get(UserRole.CITIZEN)).containsExactly(Permission.R);
    }
}
