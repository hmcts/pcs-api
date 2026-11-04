package uk.gov.hmcts.reform.pcs.enforcement.accesscontrol;

import com.google.common.collect.SetMultimap;
import uk.gov.hmcts.ccd.sdk.api.HasAccessControl;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.DEFENDANT;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.DEFENDANT_SOLICITOR;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.GA_DEFENDANT_SOLICITOR;

public class DefendantVisibleEnforcementAccess implements HasAccessControl {

    @Override
    public SetMultimap<HasRole, Permission> getGrants() {
        SetMultimap<HasRole, Permission> grants = new EnforcementStatusReadAccess().getGrants();
        grants.put(DEFENDANT, Permission.R);
        grants.put(DEFENDANT_SOLICITOR, Permission.R);
        grants.put(GA_DEFENDANT_SOLICITOR, Permission.R);
        return grants;
    }
}
