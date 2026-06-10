package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import com.google.common.collect.SetMultimap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CTSC_ADMIN;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.HEARING_CENTRE_ADMIN;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.AccessProfile.WLU_ADMIN_READ;

class CaseLinkingAccessTest {

    private CaseLinkingAccess underTest;

    @BeforeEach
    void setup() {
        underTest = new CaseLinkingAccess();
    }

    @Test
    void shouldGrantCaseLinkingAccess() {
        SetMultimap<HasRole, Permission> grants = underTest.getGrants();
        assertThat(grants.asMap()).contains(entry(CTSC_ADMIN, Permission.CRU));
        assertThat(grants.asMap()).contains(entry(HEARING_CENTRE_ADMIN, Permission.CRU));
        assertThat(grants.get(WLU_ADMIN_READ)).contains(Permission.R);
    }
}
