package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import com.google.common.collect.SetMultimap;
import uk.gov.hmcts.ccd.sdk.api.HasAccessControl;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import java.util.Arrays;
import java.util.Collection;

public class JudicialReadAccess implements HasAccessControl {

    public static boolean isJudge(Collection<String> roles) {
        return Arrays.stream(AccessGrants.JUDICIAL_READ_ROLES).anyMatch(role -> roles.contains(role.getRole()));
    }

    @Override
    public SetMultimap<HasRole, Permission> getGrants() {
        return AccessGrants.judicialReadAccess();
    }
}
