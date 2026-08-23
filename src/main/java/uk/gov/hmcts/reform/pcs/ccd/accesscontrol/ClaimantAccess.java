package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import uk.gov.hmcts.ccd.sdk.api.HasAccessControl;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.GA_CLAIMANT;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.GA_CLAIMANT_SOLICITOR;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.ORGANISATION_CASE_ACCESS_ADMINISTRATOR;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.PCS_SOLICITOR;


/**
 * Claimant data. The group access roles are the intended route, but caseworker-pcs-solicitor is
 * kept alongside them until they can actually grant anything.
 *
 * <p>Three things have to land first. A case carries no CaseAccessGroups until it holds a claimant
 * party, so no group role can reach the event that creates one. The defendant's solicitor has no
 * group role at all until the notice-of-change work lands. And cases created before this ticket
 * hold no organisation profile, so they derive no groups to match.
 *
 * <p>Removing it while those hold takes claimant fields away from everyone: the event grants say a
 * PCS solicitor may run the journey while the fields it touches say they may not.
 */
public class ClaimantAccess implements HasAccessControl {

    @Override
    public SetMultimap<HasRole, Permission> getGrants() {
        SetMultimap<HasRole, Permission> grants = HashMultimap.create();
        grants.putAll(PCS_SOLICITOR, Permission.CRU);
        grants.putAll(GA_CLAIMANT_SOLICITOR, Permission.CRU);
        // The organisations that are the claimant themselves - local authority and the "other"
        // profiles - hold this capacity rather than claimant-solicitor
        grants.putAll(GA_CLAIMANT, Permission.CRU);
        grants.putAll(ORGANISATION_CASE_ACCESS_ADMINISTRATOR, Permission.CRU);
        return grants;
    }

}
