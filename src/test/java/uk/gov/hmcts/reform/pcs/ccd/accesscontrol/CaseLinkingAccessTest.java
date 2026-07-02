package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import com.google.common.collect.SetMultimap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.AccessProfile.CIRCUIT_JUDGE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.AccessProfile.CTSC_ADMIN;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.AccessProfile.FEE_PAID_JUDGE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.AccessProfile.HEARING_CENTRE_ADMIN;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.AccessProfile.JUDGE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.AccessProfile.LEADERSHIP_JUDGE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.AccessProfile.PCS_CASE_WORKER;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.AccessProfile.PCS_SOLICITOR;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.AccessProfile.WLU_ADMIN;


class CaseLinkingAccessTest {

    private CaseLinkingAccess underTest;

    @BeforeEach
    void setup() {
        underTest = new CaseLinkingAccess();
    }

    @Test
    void shouldGrantCaseLinkingAccess() {
        SetMultimap<HasRole, Permission> grants = underTest.getGrants();

        assertThat(grants.get(PCS_SOLICITOR)).isEmpty();
        assertThat(grants.get(PCS_CASE_WORKER)).isEmpty();
        assertThat(grants.get(CTSC_ADMIN)).containsAll(Permission.CRU);
        assertThat(grants.get(HEARING_CENTRE_ADMIN)).containsAll(Permission.CRU);
        assertThat(grants.get(JUDGE)).containsAll(Permission.CRU);
        assertThat(grants.get(WLU_ADMIN)).contains(Permission.R);
    }
}
