package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import com.google.common.collect.SetMultimap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CTSC_ADMIN;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CTSC_TEAM_LEADER;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.HEARING_CENTRE_ADMIN;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.HEARING_CENTRE_TEAM_LEADER;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.WLU_ADMIN;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.WLU_TEAM_LEADER;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.FEE_PAID_JUDGE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CIRCUIT_JUDGE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.LEADERSHIP_JUDGE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.JUDGE;

class InternalCaseFlagAccessTest {

    private InternalCaseFlagAccess underTest;

    @BeforeEach
    void setUp() {
        underTest = new InternalCaseFlagAccess();
    }

    @Test
    void shouldGrantInternalCaseFlagAccess() {

        // When
        SetMultimap<HasRole, Permission> grants = underTest.getGrants();

        // Then
        assertThat(grants.asMap()).contains(entry(CTSC_TEAM_LEADER, Permission.CRU));
        assertThat(grants.asMap()).contains(entry(CTSC_ADMIN, Permission.CRU));
        assertThat(grants.asMap()).contains(entry(HEARING_CENTRE_TEAM_LEADER, Permission.CRU));
        assertThat(grants.asMap()).contains(entry(HEARING_CENTRE_ADMIN, Permission.CRU));
        assertThat(grants.asMap()).contains(entry(WLU_TEAM_LEADER, Permission.CRU));
        assertThat(grants.asMap()).contains(entry(WLU_ADMIN, Permission.CRU));
        assertThat(grants.get(FEE_PAID_JUDGE)).contains(Permission.R);
        assertThat(grants.get(CIRCUIT_JUDGE)).contains(Permission.R);
        assertThat(grants.get(LEADERSHIP_JUDGE)).contains(Permission.R);
        assertThat(grants.get(JUDGE)).contains(Permission.R);
        assertThat(grants.asMap().size()).isEqualTo(10);
    }
}
