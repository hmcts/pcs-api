package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import java.util.Collection;
import java.util.Set;

/** PRM organisational group-access roles. Not CCD case roles; retired IDAM/case-role markers are omitted. */
public final class OrganisationUserRoles {

    /** claimant covers LA/other-org; solicitor orgs get the two solicitor roles instead. */
    public static final Set<String> ORGANISATION_ROLES = Set.of(
        UserRole.CLAIMANT.getRole(),
        UserRole.GA_CLAIMANT_SOLICITOR.getRole(),
        UserRole.GA_DEFENDANT_SOLICITOR.getRole()
    );

    private OrganisationUserRoles() {
    }

    public static boolean belongsToOrganisation(Collection<String> roles) {
        return roles != null && roles.stream().anyMatch(ORGANISATION_ROLES::contains);
    }
}
