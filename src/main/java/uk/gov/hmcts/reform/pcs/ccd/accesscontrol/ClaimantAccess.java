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
 * Claimant data. Group access roles are the intended route; caseworker-pcs-solicitor stays
 * until they can grant anything (needs CaseAccessGroups on new cases, the NoC group role,
 * and org profiles on pre-existing cases). Removing it early locks claimant fields for everyone.
 */
public class ClaimantAccess implements HasAccessControl {

    @Override
    public SetMultimap<HasRole, Permission> getGrants() {
        SetMultimap<HasRole, Permission> grants = HashMultimap.create();
        grants.putAll(PCS_SOLICITOR, Permission.CRU);
        grants.putAll(GA_CLAIMANT_SOLICITOR, Permission.CRU);
        // orgs that are the claimant themselves (LA / "other" profiles)
        grants.putAll(GA_CLAIMANT, Permission.CRU);
        grants.putAll(ORGANISATION_CASE_ACCESS_ADMINISTRATOR, Permission.CRU);
        return grants;
    }

}
