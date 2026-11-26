package uk.gov.hmcts.reform.pcs.ccd.service.genapp;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.DraftOrderWordingFieldAccess;
import uk.gov.hmcts.reform.pcs.ccd.service.UserRoleService;

@Component
@RequiredArgsConstructor
public class DraftOrderWordingAccess {

    private final UserRoleService userRoleService;

    public boolean isAllowed(long caseReference) {
        return userRoleService.getCurrentUserCaseRoles(caseReference).roles().stream()
            .anyMatch(DraftOrderWordingFieldAccess::isSolicitorRole);
    }
}
