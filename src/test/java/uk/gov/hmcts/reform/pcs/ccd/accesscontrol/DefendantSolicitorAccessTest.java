package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import com.google.common.collect.SetMultimap;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DefendantSolicitorAccessTest {

    @Test
    void getGrants_ReturnsGrantMap() {
        // given
        DefendantSolicitorAccess defendantSolicitorAccess = new DefendantSolicitorAccess();

        // when
        SetMultimap<HasRole, Permission> grants = defendantSolicitorAccess.getGrants();

        // then
        Set<Permission> permissions = grants.get(UserRole.DEFENDANT_SOLICITOR);
        assertThat(permissions).isEqualTo(Permission.CRUD);
    }

}
