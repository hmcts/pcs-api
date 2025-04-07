package uk.gov.hmcts.reform.pcs.ccd.domain;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import uk.gov.hmcts.ccd.sdk.api.HasAccessControl;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import java.util.Set;


/**
 * Placeholder access control configuration granting caseworker CRU.
 */
public class DefaultStateAccess implements HasAccessControl {
    @Override
    public SetMultimap<HasRole, Permission> getGrants() {
        SetMultimap<HasRole, Permission> grants = HashMultimap.create();
        grants.putAll(UserRole.CREATOR_WITH_UPDATE, Permission.CRUD);

        // These seem to be necessary to avoid server side "case not found" errors in XUI
        // when clicking on the cases in the Case List
        grants.putAll(UserRole.CREATOR_NO_READ, Set.of(Permission.R));
        grants.putAll(UserRole.READER, Set.of(Permission.R));
        return grants;
    }
}
