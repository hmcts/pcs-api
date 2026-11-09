package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import com.google.common.collect.SetMultimap;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import static org.assertj.core.api.Assertions.assertThat;

class InternalDocumentMoveAccessTest {

    private final InternalDocumentMoveAccess underTest = new InternalDocumentMoveAccess();

    @Test
    void shouldGrantReadAndUpdateToDocumentMoveRolesOnly() {
        SetMultimap<HasRole, Permission> grants = underTest.getGrants();

        assertThat(grants.keySet()).containsExactlyInAnyOrderElementsOf(DocumentMoveRoles.DOCUMENT_MOVE_ROLES);
        for (UserRole role : DocumentMoveRoles.DOCUMENT_MOVE_ROLES) {
            assertThat(grants.get(role)).containsExactlyInAnyOrder(Permission.R, Permission.U);
        }
    }

    @Test
    void shouldNotGrantAccessToExternalUsers() {
        SetMultimap<HasRole, Permission> grants = underTest.getGrants();

        assertThat(grants.get(UserRole.PCS_SOLICITOR)).isEmpty();
        assertThat(grants.get(UserRole.CLAIMANT)).isEmpty();
        assertThat(grants.get(UserRole.GA_CLAIMANT_SOLICITOR)).isEmpty();
        assertThat(grants.get(UserRole.DEFENDANT)).isEmpty();
        assertThat(grants.get(UserRole.GA_DEFENDANT_SOLICITOR)).isEmpty();
    }
}
