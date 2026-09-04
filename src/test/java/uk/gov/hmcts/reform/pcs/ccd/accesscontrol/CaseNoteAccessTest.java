package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import com.google.common.collect.SetMultimap;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import static org.assertj.core.api.Assertions.assertThat;

class CaseNoteAccessTest {

    private final CaseNoteAccess underTest = new CaseNoteAccess();

    @Test
    void shouldGrantReadAccessToCaseNoteRoles() {
        SetMultimap<HasRole, Permission> grants = underTest.getGrants();

        for (UserRole role : CaseNoteRoles.CASE_NOTE_ROLES) {
            assertThat(grants.get(role)).contains(Permission.R);
        }
    }

    @Test
    void shouldNotGrantReadAccessToProfessionalUsers() {
        SetMultimap<HasRole, Permission> grants = underTest.getGrants();

        assertThat(grants.get(UserRole.PCS_SOLICITOR)).isEmpty();
        assertThat(grants.get(UserRole.CLAIMANT)).isEmpty();
        assertThat(grants.get(UserRole.GA_CLAIMANT_SOLICITOR)).isEmpty();
        assertThat(grants.get(UserRole.DEFENDANT)).isEmpty();
        assertThat(grants.get(UserRole.GA_DEFENDANT_SOLICITOR)).isEmpty();
    }
}
