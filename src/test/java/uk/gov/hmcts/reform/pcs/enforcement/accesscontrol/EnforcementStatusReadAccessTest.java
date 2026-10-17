package uk.gov.hmcts.reform.pcs.enforcement.accesscontrol;

import com.google.common.collect.SetMultimap;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.InternalTabAccess;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CLAIMANT;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.DEFENDANT;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.DEFENDANT_SOLICITOR;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.GA_CLAIMANT_SOLICITOR;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.GA_DEFENDANT_SOLICITOR;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.PCS_SOLICITOR;

class EnforcementStatusReadAccessTest {

    private final SetMultimap<HasRole, Permission> grants = new EnforcementStatusReadAccess().getGrants();

    @Test
    void shouldGrantOnlyReadAccess() {
        assertThat(grants.values()).containsOnly(Permission.R);
    }

    @Test
    void shouldGrantReadAccessToInternalUsersAndTheClaimantSide() {
        Set<HasRole> expected = new InternalTabAccess().getGrants().keySet();

        assertThat(grants.keySet()).containsAll(expected).contains(PCS_SOLICITOR, GA_CLAIMANT_SOLICITOR, CLAIMANT);
    }

    @Test
    void shouldNotGrantAccessToDefendants() {
        assertThat(grants.keySet()).doesNotContain(DEFENDANT, DEFENDANT_SOLICITOR, GA_DEFENDANT_SOLICITOR);
    }
}
