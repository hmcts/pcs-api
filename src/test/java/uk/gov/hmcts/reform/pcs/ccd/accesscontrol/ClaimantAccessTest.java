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
        // Claimant fields follow the firm through its capacities.
        assertThat(grants.get(UserRole.GA_CLAIMANT_SOLICITOR)).isEqualTo(Permission.CRU);
        assertThat(grants.get(UserRole.GA_CLAIMANT)).isEqualTo(Permission.CRU);
        assertThat(grants.get(UserRole.ORGANISATION_CASE_ACCESS_ADMINISTRATOR)).isEqualTo(Permission.CRU);
        // Kept until the group roles can grant anything: creation cannot use them, the defendant's
        // solicitor has none, and pre-existing cases derive no groups.
        assertThat(grants.get(UserRole.PCS_SOLICITOR)).isEqualTo(Permission.CRU);
    }
}
