package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import uk.gov.hmcts.ccd.sdk.api.HasAccessControl;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CLAIMANT;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.GA_CLAIMANT_SOLICITOR;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.ORGANISATION_CASE_ACCESS_ADMINISTRATOR;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.PCS_SOLICITOR;


public class ClaimantAccess implements HasAccessControl {

    @Override
    public SetMultimap<HasRole, Permission> getGrants() {
        SetMultimap<HasRole, Permission> grants = HashMultimap.create();
        grants.putAll(GA_CLAIMANT_SOLICITOR, Permission.CRU);
        grants.putAll(CLAIMANT, Permission.CRU);
        grants.putAll(ORGANISATION_CASE_ACCESS_ADMINISTRATOR, Permission.CRU);
        // Retained until group access is enabled in every environment: a solicitor reaching a case
        // through [CREATOR] while the platform flags are off holds no capacity, so without this the
        // claimant fields are ungranted and the case opens blank. Removed once those flags are on.
        grants.putAll(PCS_SOLICITOR, Permission.CRU);
        return grants;
    }

}
