package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import uk.gov.hmcts.ccd.sdk.api.HasAccessControl;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import java.util.Arrays;
import java.util.Collection;

/** The combined bundle, its status and missing-document details are for judges only. */
public class JudgeBundleAccess implements HasAccessControl {
    private static final UserRole[] JUDGES = {
        UserRole.JUDGE, UserRole.FEE_PAID_JUDGE, UserRole.CIRCUIT_JUDGE, UserRole.LEADERSHIP_JUDGE
    };

    public static boolean isJudge(Collection<String> roles) {
        return Arrays.stream(JUDGES).anyMatch(role -> roles.contains(role.getRole()));
    }

    @Override
    public SetMultimap<HasRole, Permission> getGrants() {
        SetMultimap<HasRole, Permission> grants = HashMultimap.create();
        Arrays.stream(JUDGES).forEach(role -> grants.put(role, Permission.R));
        return grants;
    }
}
