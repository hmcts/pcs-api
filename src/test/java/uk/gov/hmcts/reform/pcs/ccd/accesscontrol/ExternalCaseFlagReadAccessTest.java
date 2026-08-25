package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import com.google.common.collect.SetMultimap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CTSC_ADMIN;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.JUDGE;

class ExternalCaseFlagReadAccessTest {

    private ExternalCaseFlagReadAccess underTest;

    @BeforeEach
    void setUp() {
        underTest = new ExternalCaseFlagReadAccess();
    }

    @Test
    void shouldGrantReadToEveryExternalCaseFlagRole() {
        SetMultimap<HasRole, Permission> grants = underTest.getGrants();

        Arrays.stream(ExternalCaseFlagRoles.EXTERNAL_CASE_FLAG_ROLES)
            .forEach(externalRole -> assertThat(grants.get(externalRole)).containsExactly(Permission.R));
        assertThat(grants.asMap()).hasSize(ExternalCaseFlagRoles.EXTERNAL_CASE_FLAG_ROLES.length);
    }

    @Test
    void shouldNotGrantCreateUpdateOrDeleteToAnyRole() {
        SetMultimap<HasRole, Permission> grants = underTest.getGrants();

        assertThat(grants.values())
            .doesNotContain(Permission.C, Permission.U, Permission.D)
            .containsOnly(Permission.R);
    }

    @Test
    void shouldNotGrantAnyInternalRoleAccess() {
        SetMultimap<HasRole, Permission> grants = underTest.getGrants();

        assertThat(grants.get(CTSC_ADMIN)).isEmpty();
        assertThat(grants.get(JUDGE)).isEmpty();
    }
}
