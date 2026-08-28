package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import uk.gov.hmcts.ccd.sdk.api.HasAccessControl;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.ExternalCaseFlagRoles.EXTERNAL_CASE_FLAG_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CLAIMANT;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.GA_CLAIMANT_SOLICITOR;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.GA_DEFENDANT_SOLICITOR;

public class ExternalCaseFlagAccess implements HasAccessControl {

    @Override
    public SetMultimap<HasRole, Permission> getGrants() {
        SetMultimap<HasRole, Permission> grants = HashMultimap.create();
        for (UserRole externalRole : EXTERNAL_CASE_FLAG_ROLES) {
            grants.putAll(externalRole, Permission.CRU);
        }
        // Group-access users reach the case through their organisation role alone (HDPI-8701).
        grants.putAll(CLAIMANT, Permission.CRU);
        grants.putAll(GA_CLAIMANT_SOLICITOR, Permission.CRU);
        grants.putAll(GA_DEFENDANT_SOLICITOR, Permission.CRU);

        return grants;
    }
}
