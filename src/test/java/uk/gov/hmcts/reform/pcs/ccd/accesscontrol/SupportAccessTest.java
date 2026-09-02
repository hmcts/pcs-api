package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import com.google.common.collect.SetMultimap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CITIZEN;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CLAIMANT;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CLAIMANT_SOLICITOR;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CTSC_ADMIN;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.DEFENDANT;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.DEFENDANT_SOLICITOR;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.DUTY_ADVISOR_REQUEST;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.GA_CLAIMANT_SOLICITOR;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.GA_DEFENDANT_SOLICITOR;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.JUDGE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.PCS_SOLICITOR;

class SupportAccessTest {

    private SupportAccess underTest;

    @BeforeEach
    void setUp() {
        underTest = new SupportAccess();
    }

    @Test
    void shouldGrantTheTwoGroupAccessLegalRepresentativeProfilesOnly() {
        SetMultimap<HasRole, Permission> grants = underTest.getGrants();

        assertThat(grants.get(GA_CLAIMANT_SOLICITOR)).containsAll(Permission.CRU);
        assertThat(grants.get(GA_DEFENDANT_SOLICITOR)).containsAll(Permission.CRU);
        assertThat(grants.asMap()).hasSize(2);
        assertThat(ExternalCaseFlagRoles.SUPPORT_ROLES)
            .doesNotHaveDuplicates()
            .containsExactlyInAnyOrder(GA_CLAIMANT_SOLICITOR, GA_DEFENDANT_SOLICITOR);
    }

    @Test
    void shouldNotGrantThePreGroupAccessSolicitorRoles() {
        SetMultimap<HasRole, Permission> grants = underTest.getGrants();

        assertThat(grants.get(PCS_SOLICITOR)).isEmpty();
        assertThat(grants.get(CLAIMANT_SOLICITOR)).isEmpty();
        assertThat(grants.get(DEFENDANT_SOLICITOR)).isEmpty();
    }

    @Test
    void shouldNotGrantThePartiesActingForThemselves() {
        SetMultimap<HasRole, Permission> grants = underTest.getGrants();

        assertThat(grants.get(CLAIMANT)).isEmpty();
        assertThat(grants.get(CITIZEN)).isEmpty();
        assertThat(grants.get(DEFENDANT)).isEmpty();
    }

    @Test
    void shouldNotGrantAnyInternalRole() {
        SetMultimap<HasRole, Permission> grants = underTest.getGrants();

        assertThat(grants.get(CTSC_ADMIN)).isEmpty();
        assertThat(grants.get(JUDGE)).isEmpty();
    }

    @Test
    void shouldNotGrantTheTimeBoundDutyAdvisorProfile() {
        assertThat(underTest.getGrants().get(DUTY_ADVISOR_REQUEST)).isEmpty();
    }

    @Test
    void shouldKeepTheSupportRolesNarrowerThanExternalCaseVisibility() {
        assertThat(Arrays.asList(ExternalCaseFlagRoles.EXTERNAL_CASE_FLAG_ROLES))
            .containsAll(Arrays.asList(ExternalCaseFlagRoles.SUPPORT_ROLES))
            .hasSizeGreaterThan(ExternalCaseFlagRoles.SUPPORT_ROLES.length);
    }
}
