package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import com.google.common.collect.SetMultimap;
import uk.gov.hmcts.ccd.sdk.api.HasAccessControl;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

/**
 * The Case details tab: everyone who sees party-visible tabs, plus read for the system user so
 * pcs-api can fetch the tab's documents through CDAM.
 */
public class CaseDetailsTabAccess implements HasAccessControl {

    @Override
    public SetMultimap<HasRole, Permission> getGrants() {
        return AccessGrants.caseDetailsTabAccess();
    }

}
