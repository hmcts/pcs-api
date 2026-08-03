package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import com.google.common.collect.SetMultimap;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CITIZEN;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CLAIMANT_SOLICITOR;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.DEFENDANT;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.DEFENDANT_SOLICITOR;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.PCS_SOLICITOR;

class DocumentAccessTest {

    private static final UserRole[] CREATE_READ_ROLES = {
        PCS_SOLICITOR,
        CITIZEN,
        DEFENDANT,
        CLAIMANT_SOLICITOR,
        DEFENDANT_SOLICITOR
    };

    private final DocumentAccess underTest = new DocumentAccess();

    @Test
    void shouldGrantAccessToUsersWhoCanViewCaseFileView() {
        SetMultimap<HasRole, Permission> grants = underTest.getGrants();

        assertAllHavePermissions(grants, CREATE_READ_ROLES, Permission.CR);
        assertAllHavePermission(grants, AccessGrants.INTERNAL_READ_ROLES, Permission.R);
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
