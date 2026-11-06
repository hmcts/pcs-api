package uk.gov.hmcts.reform.pcs.ccd.service.genapp;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.service.UserRoleService;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class DraftOrderWordingAccess {

    private static final Set<String> CLAIMANT_SOLICITOR_ROLES = Set.of(
        UserRole.CLAIMANT_SOLICITOR.getRole(),
        UserRole.GA_CLAIMANT_SOLICITOR.getRole()
    );

    private final UserRoleService userRoleService;

    public boolean isAllowed(long caseReference) {
        return userRoleService.getCurrentUserCaseRoles(caseReference).roles().stream()
            .anyMatch(CLAIMANT_SOLICITOR_ROLES::contains);
    }
}
