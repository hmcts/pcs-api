package uk.gov.hmcts.reform.pcs.ccd.domain;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import uk.gov.hmcts.ccd.sdk.api.HasAccessControl;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import static uk.gov.hmcts.reform.pcs.ccd.domain.UserRole.CASE_WORKER;
import static uk.gov.hmcts.reform.pcs.ccd.domain.UserRole.INTERESTED_PARTY;
import static uk.gov.hmcts.reform.pcs.ccd.domain.UserRole.JUDICIARY;


/**
 * Placeholder access control configuration granting caseworker CRU.
 */
public class DefaultStateAccess implements HasAccessControl {
    @Override
    public SetMultimap<HasRole, Permission> getGrants() {
        SetMultimap<HasRole, Permission> grants = HashMultimap.create();
        grants.putAll(CASE_WORKER, Permission.CRU);
        grants.putAll(JUDICIARY, Permission.CRUD);
        grants.putAll(INTERESTED_PARTY, Permission.CRUD);
        return grants;
    }
}
