package uk.gov.hmcts.reform.pcs.enforcement.accesscontrol;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.InternalTabAccess;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;

import static org.assertj.core.api.Assertions.assertThat;

class RiskLevelReadAccessTest {

    private final RiskLevelReadAccess underTest = new RiskLevelReadAccess();

    @Test
    void shouldGiveInternalUsersReadAccess() {
        assertThat(underTest.getGrants()).isEqualTo(new InternalTabAccess().getGrants());
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"BAILIFF", "BAILIFF_ADMIN", "BAILIFF_MANAGER"})
    void shouldLetBailiffsReadTheRiskLevel(UserRole userRole) {
        assertThat(underTest.getGrants().get(userRole)).containsExactly(Permission.R);
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {
        "PCS_SOLICITOR", "GA_CLAIMANT_SOLICITOR", "CLAIMANT", "CLAIMANT_SOLICITOR", "DEFENDANT_SOLICITOR",
        "GA_DEFENDANT_SOLICITOR", "DEFENDANT", "CITIZEN"})
    void shouldNotLetThePartiesReadTheRiskLevel(UserRole userRole) {
        assertThat(underTest.getGrants().get(userRole)).isEmpty();
    }
}
