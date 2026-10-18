package uk.gov.hmcts.reform.pcs.enforcement.accesscontrol;

import com.google.common.collect.SetMultimap;
import uk.gov.hmcts.ccd.sdk.api.HasAccessControl;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.InternalTabAccess;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CLAIMANT;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.GA_CLAIMANT_SOLICITOR;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.PCS_SOLICITOR;

/**
 * Read only access to the enforcement status for internal users and the claimant side. Defendants get none.
 * A defendant's legal representative holds {@code caseworker-pcs-solicitor} as well, though, so
 * {@link uk.gov.hmcts.reform.pcs.enforcement.view.DefendantEnforcementView} also leaves these fields unset
 * whenever the user is a defendant on the case.
 */
public class EnforcementStatusReadAccess implements HasAccessControl {

    @Override
    public SetMultimap<HasRole, Permission> getGrants() {
        SetMultimap<HasRole, Permission> grants = new InternalTabAccess().getGrants();
        grants.put(PCS_SOLICITOR, Permission.R);
        grants.put(GA_CLAIMANT_SOLICITOR, Permission.R);
        grants.put(CLAIMANT, Permission.R);
        return grants;
    }
}
