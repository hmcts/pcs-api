package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentHistoryTabAccessTest {

    private final PaymentHistoryTabAccess underTest = new PaymentHistoryTabAccess();

    @ParameterizedTest(name = "{0} should have read access")
    @MethodSource("rolesWithPaymentHistoryAccess")
    void shouldGrantReadAccessToPermittedRole(UserRole role) {
        assertThat(underTest.getGrants().get(role)).containsExactly(Permission.R);
    }

    @ParameterizedTest(name = "{0} should not have access")
    @MethodSource("rolesWithoutPaymentHistoryAccess")
    void shouldNotGrantAccessToNonPermittedRole(UserRole role) {
        assertThat(underTest.getGrants().get(role)).isEmpty();
    }

    @Test
    void shouldNotGrantAnyUnexpectedRoles() {
        assertThat(underTest.getGrants().keySet()).containsExactlyInAnyOrder(
            rolesWithPaymentHistoryAccess().toArray(UserRole[]::new)
        );
    }

    private static Stream<UserRole> rolesWithPaymentHistoryAccess() {
        return Stream.of(
            UserRole.JUDGE,
            UserRole.FEE_PAID_JUDGE,
            UserRole.CIRCUIT_JUDGE,
            UserRole.LEADERSHIP_JUDGE,
            UserRole.HEARING_CENTRE_TEAM_LEADER,
            UserRole.HEARING_CENTRE_ADMIN,
            UserRole.CTSC_TEAM_LEADER,
            UserRole.CTSC_ADMIN,
            UserRole.WLU_TEAM_LEADER,
            UserRole.WLU_ADMIN
        );
    }

    private static Stream<UserRole> rolesWithoutPaymentHistoryAccess() {
        return Stream.of(
            UserRole.CREATOR,
            UserRole.CITIZEN,
            UserRole.DEFENDANT,
            UserRole.GA_DEFENDANT_SOLICITOR,
            UserRole.CLAIMANT,
            UserRole.GA_CLAIMANT_SOLICITOR,
            UserRole.PCS_CASE_WORKER,
            UserRole.RAS_VALIDATOR,
            UserRole.SYSTEM_USER
        );
    }
}
