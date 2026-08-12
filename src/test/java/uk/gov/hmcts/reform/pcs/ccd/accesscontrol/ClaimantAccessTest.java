package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import com.google.common.collect.SetMultimap;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import static org.assertj.core.api.Assertions.assertThat;

class ClaimantAccessTest {

    @Test
    void getGrants_ReturnsMultiMap() {
        // given
        ClaimantAccess claimantAccess = new ClaimantAccess();

        // when
        SetMultimap<HasRole, Permission> grants = claimantAccess.getGrants();

        // then
        // Claimant fields are granted to the organisation's capacities rather than to the
        // caseworker-pcs-solicitor IDAM role, so that access follows the firm rather than the
        // individual who happens to hold that role.
        assertThat(grants.get(UserRole.GA_CLAIMANT_SOLICITOR)).isEqualTo(Permission.CRU);
        assertThat(grants.get(UserRole.CLAIMANT)).isEqualTo(Permission.CRU);
        assertThat(grants.get(UserRole.ORGANISATION_CASE_ACCESS_ADMINISTRATOR)).isEqualTo(Permission.CRU);
    }
}
