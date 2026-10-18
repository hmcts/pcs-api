package uk.gov.hmcts.reform.pcs.enforcement.accesscontrol;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import uk.gov.hmcts.ccd.sdk.api.HasAccessControl;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CLAIMANT;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.GA_CLAIMANT_SOLICITOR;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.PCS_SOLICITOR;

/**
 * Read only access for the claimant side to a field that gates a claimant enforcement event. A defendant's legal
 * representative holds {@code caseworker-pcs-solicitor} as well, so
 * {@link uk.gov.hmcts.reform.pcs.enforcement.view.DefendantEnforcementView} leaves these fields unset whenever the
 * user is a defendant on the case.
 */
public class ClaimantEventFlagAccess implements HasAccessControl {

    @Override
    public SetMultimap<HasRole, Permission> getGrants() {
        SetMultimap<HasRole, Permission> grants = HashMultimap.create();
        grants.put(PCS_SOLICITOR, Permission.R);
        grants.put(GA_CLAIMANT_SOLICITOR, Permission.R);
        grants.put(CLAIMANT, Permission.R);
        return grants;
    }
}
