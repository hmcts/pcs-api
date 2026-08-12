package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import com.google.common.collect.SetMultimap;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import static org.assertj.core.api.Assertions.assertThat;

class AcaSystemUserAccessTest {

    @Test
    void getGrants_ReturnsMultiMap() {
        // given
        AcaSystemUserAccess acaSystemUserAccess = new AcaSystemUserAccess();

        // when
        SetMultimap<HasRole, Permission> grants = acaSystemUserAccess.getGrants();

        // then
        assertThat(grants.get(UserRole.ORGANISATION_CASE_ACCESS_ADMINISTRATOR)).isEqualTo(Permission.CRUD);
    }

}
