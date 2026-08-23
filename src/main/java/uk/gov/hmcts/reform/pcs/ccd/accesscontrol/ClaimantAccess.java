package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import uk.gov.hmcts.ccd.sdk.api.HasAccessControl;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.GA_CLAIMANT;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.GA_CLAIMANT_SOLICITOR;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.ORGANISATION_CASE_ACCESS_ADMINISTRATOR;


/**
 * Claimant data is reachable only through the group access roles, so that a solicitor holding the
 * blanket caseworker-pcs-solicitor role cannot stand in for an organisation that derives no
 * CaseAccessGroups - otherwise a broken group access configuration still reads as working.
 */
public class ClaimantAccess implements HasAccessControl {

    @Override
    public SetMultimap<HasRole, Permission> getGrants() {
        SetMultimap<HasRole, Permission> grants = HashMultimap.create();
        grants.putAll(GA_CLAIMANT_SOLICITOR, Permission.CRU);
        // The organisations that are the claimant themselves - local authority and the "other"
        // profiles - hold this capacity rather than claimant-solicitor
        grants.putAll(GA_CLAIMANT, Permission.CRU);
        grants.putAll(ORGANISATION_CASE_ACCESS_ADMINISTRATOR, Permission.CRU);
        return grants;
    }

}
