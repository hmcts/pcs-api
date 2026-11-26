package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import uk.gov.hmcts.ccd.sdk.api.HasAccessControl;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import java.util.Set;

public class DraftOrderWordingFieldAccess implements HasAccessControl {

    private static final Set<UserRole> SOLICITOR_ROLES = Set.of(
        UserRole.CLAIMANT_SOLICITOR,
        UserRole.GA_CLAIMANT_SOLICITOR,
        UserRole.DEFENDANT_SOLICITOR,
        UserRole.GA_DEFENDANT_SOLICITOR
    );

    public static boolean isSolicitorRole(String role) {
        return SOLICITOR_ROLES.stream().anyMatch(solicitorRole -> solicitorRole.getRole().equals(role));
    }

    @Override
    public SetMultimap<HasRole, Permission> getGrants() {
        SetMultimap<HasRole, Permission> grants = HashMultimap.create();
        SOLICITOR_ROLES.forEach(role -> grants.putAll(role, Permission.CRUD));
        return grants;
    }
}
