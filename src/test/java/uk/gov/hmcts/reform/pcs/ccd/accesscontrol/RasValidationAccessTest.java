package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import com.google.common.collect.SetMultimap;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.RAS_VALIDATOR;

class RasValidationAccessTest {

    private final RasValidationAccess underTest = new RasValidationAccess();

    @Test
    void shouldGrantReadAccessToRasValidatorRole() {
        SetMultimap<HasRole, Permission> grants = underTest.getGrants();

        assertThat(grants.keySet()).containsExactly(RAS_VALIDATOR);
        assertThat(grants.get(RAS_VALIDATOR)).containsExactly(Permission.R);
    }
}
