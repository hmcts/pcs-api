package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import com.google.common.collect.SetMultimap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CITIZEN;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CLAIMANT_SOLICITOR;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CTSC_ADMIN;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.DEFENDANT;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.DEFENDANT_SOLICITOR;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CLAIMANT;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.GA_CLAIMANT_SOLICITOR;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.GA_DEFENDANT_SOLICITOR;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.JUDGE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.PCS_SOLICITOR;

class ExternalCaseFlagAccessTest {

    private ExternalCaseFlagAccess underTest;

    @BeforeEach
    void setUp() {
        underTest = new ExternalCaseFlagAccess();
    }

    @Test
    void shouldGrantExternalCaseFlagAccessToEveryExternalPersona() {
        // When
        SetMultimap<HasRole, Permission> grants = underTest.getGrants();

        // Then
        assertThat(grants.get(PCS_SOLICITOR)).containsAll(Permission.CRU);
        assertThat(grants.get(CITIZEN)).containsAll(Permission.CRU);
        assertThat(grants.get(CLAIMANT_SOLICITOR)).containsAll(Permission.CRU);
        assertThat(grants.get(DEFENDANT)).containsAll(Permission.CRU);
        assertThat(grants.get(DEFENDANT_SOLICITOR)).containsAll(Permission.CRU);
        assertThat(grants.get(CLAIMANT)).containsAll(Permission.CRU);
        assertThat(grants.get(GA_CLAIMANT_SOLICITOR)).containsAll(Permission.CRU);
        assertThat(grants.get(GA_DEFENDANT_SOLICITOR)).containsAll(Permission.CRU);
        assertThat(grants.asMap()).hasSize(ExternalCaseFlagRoles.EXTERNAL_CASE_FLAG_ROLES.length);
    }

    @Test
    void shouldNotGrantAnyInternalRoleAccessToExternalFlags() {
        SetMultimap<HasRole, Permission> grants = underTest.getGrants();

        assertThat(grants.get(CTSC_ADMIN)).isEmpty();
        assertThat(grants.get(JUDGE)).isEmpty();
    }
}
