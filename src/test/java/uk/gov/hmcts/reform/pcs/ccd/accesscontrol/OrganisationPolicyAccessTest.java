package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import com.google.common.collect.SetMultimap;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import static org.assertj.core.api.Assertions.assertThat;

class OrganisationPolicyAccessTest {

    @Test
    void getGrants_ReturnsMultiMap() {
        // given
        OrganisationPolicyAccess organisationPolicyAccess = new OrganisationPolicyAccess();

        // when
        SetMultimap<HasRole, Permission> grants = organisationPolicyAccess.getGrants();

        // then
        assertThat(grants.get(UserRole.CTSC_ADMIN)).isEqualTo(Permission.CRU);
        assertThat(grants.get(UserRole.HEARING_CENTRE_ADMIN)).isEqualTo(Permission.CRU);
        assertThat(grants.get(UserRole.WLU_ADMIN)).isEqualTo(Permission.CRU);
        assertThat(grants.get(UserRole.FEE_PAID_JUDGE)).contains(Permission.R);
        assertThat(grants.get(UserRole.CIRCUIT_JUDGE)).contains(Permission.R);
        assertThat(grants.get(UserRole.LEADERSHIP_JUDGE)).contains(Permission.R);
        assertThat(grants.get(UserRole.JUDGE)).contains(Permission.R);
    }
}
