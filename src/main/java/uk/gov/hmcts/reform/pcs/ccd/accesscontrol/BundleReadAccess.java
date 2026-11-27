package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import com.google.common.collect.SetMultimap;
import uk.gov.hmcts.ccd.sdk.api.HasAccessControl;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import java.util.Collection;
import java.util.Set;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.HEARING_CENTRE_ADMIN;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.HEARING_CENTRE_TEAM_LEADER;

public class BundleReadAccess implements HasAccessControl {

    private static final Set<UserRole> STAFF = Set.of(HEARING_CENTRE_ADMIN, HEARING_CENTRE_TEAM_LEADER);

    public static boolean canRead(Collection<String> roles) {
        return JudicialReadAccess.isJudge(roles) || STAFF.stream().anyMatch(role -> roles.contains(role.getRole()));
    }

    @Override
    public SetMultimap<HasRole, Permission> getGrants() {
        var grants = AccessGrants.judicialReadAccess();
        STAFF.forEach(role -> grants.put(role, Permission.R));
        return grants;
    }
}
