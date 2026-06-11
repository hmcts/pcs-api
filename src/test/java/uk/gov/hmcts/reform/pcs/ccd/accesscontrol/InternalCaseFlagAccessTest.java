package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import com.google.common.collect.SetMultimap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CIRCUIT_JUDGE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CTSC_ADMIN;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.FEE_PAID_JUDGE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.HEARING_CENTRE_ADMIN;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.JUDGE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.LEADERSHIP_JUDGE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.WLU_ADMIN;

class InternalCaseFlagAccessTest {

    private static final UserRole[] CREATE_READ_UPDATE_ROLES = {
        CTSC_ADMIN,
        HEARING_CENTRE_ADMIN,
        WLU_ADMIN
    };

    private static final UserRole[] READ_ROLES = {
        FEE_PAID_JUDGE,
        CIRCUIT_JUDGE,
        LEADERSHIP_JUDGE,
        JUDGE
    };

    private InternalCaseFlagAccess underTest;

    @BeforeEach
    void setUp() {
        underTest = new InternalCaseFlagAccess();
    }

    @Test
    void shouldGrantInternalCaseFlagAccess() {

        // When
        SetMultimap<HasRole, Permission> grants = underTest.getGrants();

        // Then
        assertAllHavePermissions(grants, CREATE_READ_UPDATE_ROLES, Permission.CRU);
        assertAllHavePermission(grants, READ_ROLES, Permission.R);
        assertThat(grants.asMap().size()).isEqualTo(7);
    }

    private void assertAllHavePermissions(SetMultimap<HasRole, Permission> grants,
                                          UserRole[] roles,
                                          Set<Permission> permissions) {
        for (UserRole role : roles) {
            assertThat(grants.get(role)).containsAll(permissions);
        }
    }

    private void assertAllHavePermission(SetMultimap<HasRole, Permission> grants,
                                         UserRole[] roles,
                                         Permission permission) {
        for (UserRole role : roles) {
            assertThat(grants.get(role)).contains(permission);
        }
    }
}
