package uk.gov.hmcts.reform.pcs.enforcement.accesscontrol;

import com.google.common.collect.SetMultimap;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CITIZEN;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.DEFENDANT;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.DEFENDANT_SOLICITOR;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.GA_DEFENDANT_SOLICITOR;

class DefendantVisibleEnforcementAccessTest {

    private final SetMultimap<HasRole, Permission> grants = new DefendantVisibleEnforcementAccess().getGrants();

    @Test
    void shouldGrantOnlyReadAccess() {
        assertThat(grants.values()).containsOnly(Permission.R);
    }

    @Test
    void shouldGrantEveryoneWithEnforcementStatusAccessAndTheDefendantSide() {
        assertThat(grants.keySet())
            .containsAll(new EnforcementStatusReadAccess().getGrants().keySet())
            .contains(DEFENDANT, DEFENDANT_SOLICITOR, GA_DEFENDANT_SOLICITOR)
            .doesNotContain(CITIZEN);
    }
}
