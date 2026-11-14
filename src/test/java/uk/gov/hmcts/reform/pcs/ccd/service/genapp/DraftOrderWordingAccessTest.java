package uk.gov.hmcts.reform.pcs.ccd.service.genapp;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.service.UserRoleService;
import uk.gov.hmcts.reform.pcs.ccd.service.UserRoles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DraftOrderWordingAccessTest {

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void shouldAllowOnlySolicitorCaseRoles(UserRole role) {
        UserRoleService userRoleService = mock(UserRoleService.class);
        when(userRoleService.getCurrentUserCaseRoles(1234L))
            .thenReturn(new UserRoles("user", List.of(role.getRole())));

        boolean allowed = new DraftOrderWordingAccess(userRoleService).isAllowed(1234L);

        assertThat(allowed).isEqualTo(
            role == UserRole.CLAIMANT_SOLICITOR || role == UserRole.GA_CLAIMANT_SOLICITOR
                || role == UserRole.DEFENDANT_SOLICITOR || role == UserRole.GA_DEFENDANT_SOLICITOR);
    }
}
