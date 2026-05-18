package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.HMCTS_ADMIN;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.HMCTS_CTSC;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.HMCTS_JUDICIARY;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.HMCTS_LEGAL_OPERATIONS;

import uk.gov.hmcts.ccd.sdk.api.HasAccessControl;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

public class GlobalSearchAccess implements HasAccessControl {

    @Override
    public SetMultimap<HasRole, Permission> getGrants() {
        SetMultimap<HasRole, Permission> grants = HashMultimap.create();

        grants.put(HMCTS_ADMIN, Permission.R);
        grants.put(HMCTS_CTSC, Permission.R);
        grants.put(HMCTS_LEGAL_OPERATIONS, Permission.R);
        grants.put(HMCTS_JUDICIARY, Permission.R);

        return grants;
    }
}
