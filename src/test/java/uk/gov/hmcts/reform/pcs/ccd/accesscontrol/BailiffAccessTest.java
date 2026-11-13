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
    void shouldGrantCreateReadUpdateToBailiffAdmin() {
        SetMultimap<HasRole, Permission> grants = underTest.getGrants();

        assertThat(grants.get(UserRole.BAILIFF_ADMIN)).isEqualTo(Permission.CRU);
    }

    @Test
    void shouldOnlyGrantAccessToBailiffAdmin() {
        SetMultimap<HasRole, Permission> grants = underTest.getGrants();

        assertThat(grants.keySet()).isEqualTo(Set.of(UserRole.BAILIFF_ADMIN));
    }
}
