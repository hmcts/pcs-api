package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import com.google.common.collect.SetMultimap;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CIRCUIT_JUDGE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CITIZEN;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CLAIMANT_SOLICITOR;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CTSC_ADMIN;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CTSC_TEAM_LEADER;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.DEFENDANT;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.DEFENDANT_SOLICITOR;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.FEE_PAID_JUDGE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.HEARING_CENTRE_ADMIN;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.HEARING_CENTRE_TEAM_LEADER;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.JUDGE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.LEADERSHIP_JUDGE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.PCS_SOLICITOR;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.WLU_ADMIN;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.WLU_TEAM_LEADER;

class DocumentsAccessTest {

    private final DocumentsAccess underTest = new DocumentsAccess();

    @Test
    void shouldGrantAccessToUsersWhoCanViewCaseFileView() {
        SetMultimap<HasRole, Permission> grants = underTest.getGrants();

        assertThat(grants.asMap())
            .contains(
                entry(PCS_SOLICITOR, Permission.CR),
                entry(CITIZEN, Permission.CR),
                entry(DEFENDANT, Permission.CR),
                entry(CLAIMANT_SOLICITOR, Permission.CR),
                entry(DEFENDANT_SOLICITOR, Permission.CR)
            );
        assertThat(grants.get(JUDGE)).contains(Permission.R);
        assertThat(grants.get(FEE_PAID_JUDGE)).contains(Permission.R);
        assertThat(grants.get(CIRCUIT_JUDGE)).contains(Permission.R);
        assertThat(grants.get(LEADERSHIP_JUDGE)).contains(Permission.R);
        assertThat(grants.get(HEARING_CENTRE_TEAM_LEADER)).contains(Permission.R);
        assertThat(grants.get(HEARING_CENTRE_ADMIN)).contains(Permission.R);
        assertThat(grants.get(CTSC_TEAM_LEADER)).contains(Permission.R);
        assertThat(grants.get(CTSC_ADMIN)).contains(Permission.R);
        assertThat(grants.get(WLU_TEAM_LEADER)).contains(Permission.R);
        assertThat(grants.get(WLU_ADMIN)).contains(Permission.R);
    }
}
