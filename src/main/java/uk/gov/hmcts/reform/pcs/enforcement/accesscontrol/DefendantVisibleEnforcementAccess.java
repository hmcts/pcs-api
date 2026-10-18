package uk.gov.hmcts.reform.pcs.enforcement.accesscontrol;

import com.google.common.collect.SetMultimap;
import uk.gov.hmcts.ccd.sdk.api.HasAccessControl;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.DEFENDANT;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.DEFENDANT_SOLICITOR;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.GA_DEFENDANT_SOLICITOR;

/**
 * Read only access to the enforcement fields defendants may see once a warrant's notice of eviction is served:
 * everyone with {@link EnforcementStatusReadAccess}, plus defendants and their legal representatives. Which
 * warrants a defendant actually gets is decided per case view by
 * {@link uk.gov.hmcts.reform.pcs.enforcement.view.DefendantEnforcementView}, which leaves the field unset for
 * them until one is visible.
 */
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
