package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Bailiffs read the case like a hearing centre admin, without the case notes and payment history, and
 * without any of the hearing centre admin's events.
 */
class BailiffRoleAccessTest {

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"BAILIFF", "BAILIFF_ADMIN", "BAILIFF_MANAGER"})
    void shouldBeRasRolesThatCanWorkOnTheCase(UserRole role) {
        assertThat(role.getRoleType()).isEqualTo(RoleType.RAS);
        assertThat(role.getCaseTypePermissions()).isEqualTo(UserRole.HEARING_CENTRE_ADMIN.getCaseTypePermissions());
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"BAILIFF", "BAILIFF_ADMIN", "BAILIFF_MANAGER"})
    void shouldReadThePartyVisibleAndInternalCaseDetails(UserRole role) {
        assertThat(AccessGrants.partyVisibleReadAccess().get(role)).containsExactly(Permission.R);
        assertThat(AccessGrants.internalReadAccess().get(role)).containsExactly(Permission.R);
        assertThat(AccessGrants.documentAccess().get(role)).containsExactly(Permission.R);
        assertThat(new InternalCaseFlagAccess().getGrants().get(role)).containsExactly(Permission.R);
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"BAILIFF", "BAILIFF_ADMIN", "BAILIFF_MANAGER"})
    void shouldNotReadCaseNotesOrPaymentHistory(UserRole role) {
        assertThat(AccessGrants.caseNoteReadAccess().get(role)).isEmpty();
        assertThat(AccessGrants.paymentHistoryReadAccess().get(role)).isEmpty();
        assertThat(CaseworkerRoles.CASEWORKER_ROLES).doesNotContain(role);
    }
}
