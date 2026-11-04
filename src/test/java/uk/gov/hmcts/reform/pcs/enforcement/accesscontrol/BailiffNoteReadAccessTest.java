package uk.gov.hmcts.reform.pcs.enforcement.accesscontrol;

import com.google.common.collect.SetMultimap;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.BAILIFF;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.BAILIFF_ADMIN;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.BAILIFF_MANAGER;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CITIZEN;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CLAIMANT;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CLAIMANT_SOLICITOR;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.DEFENDANT;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.DEFENDANT_SOLICITOR;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.GA_CLAIMANT_SOLICITOR;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.GA_DEFENDANT_SOLICITOR;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.HEARING_CENTRE_ADMIN;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.JUDGE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.PCS_SOLICITOR;

class BailiffNoteReadAccessTest {

    private final SetMultimap<HasRole, Permission> grants = new BailiffNoteReadAccess().getGrants();

    @Test
    void shouldGrantOnlyReadAccess() {
        assertThat(grants.values()).containsOnly(Permission.R);
    }

    @Test
    void shouldGrantReadAccessToBailiffsAndOtherInternalUsers() {
        assertThat(grants.keySet()).contains(BAILIFF, BAILIFF_ADMIN, BAILIFF_MANAGER, HEARING_CENTRE_ADMIN, JUDGE);
    }

    @Test
    void shouldNotGrantAccessToClaimantsOrDefendants() {
        assertThat(grants.keySet()).doesNotContain(
            PCS_SOLICITOR, GA_CLAIMANT_SOLICITOR, CLAIMANT, CLAIMANT_SOLICITOR,
            DEFENDANT, DEFENDANT_SOLICITOR, GA_DEFENDANT_SOLICITOR, CITIZEN);
    }
}
