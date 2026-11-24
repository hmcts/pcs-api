package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import com.google.common.collect.SetMultimap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class BailiffAccessTest {

    private BailiffAccess underTest;

    @BeforeEach
    void setup() {
        underTest = new BailiffAccess();
    }

    @Test
    void shouldGrantCreateReadUpdateToTheThreeBailiffPersonas() {
        SetMultimap<HasRole, Permission> grants = underTest.getGrants();

        for (UserRole role : Set.of(UserRole.BAILIFF, UserRole.BAILIFF_ADMIN, UserRole.BAILIFF_MANAGER)) {
            assertThat(grants.get(role)).isEqualTo(Permission.CRU);
        }
    }

    @Test
    void shouldOnlyGrantAccessToBailiffRoles() {
        SetMultimap<HasRole, Permission> grants = underTest.getGrants();

        assertThat(grants.keySet()).containsExactlyInAnyOrder(
            UserRole.BAILIFF, UserRole.BAILIFF_ADMIN, UserRole.BAILIFF_MANAGER
        );
    }
}
