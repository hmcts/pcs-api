package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import java.util.Collection;
import java.util.Set;

/**
 * The group-access roles held by users who belong to a professional organisation - local authority,
 * other organisation and solicitor - and by nobody else. PRM grants them at organisation level from
 * the access types in {@link GroupAccessType}, so a user holds one before they touch any case.
 *
 * <p>These are AM organisational role assignments, so they are only present in a role collection
 * sourced from {@link uk.gov.hmcts.reform.pcs.ccd.service.UserRoleService}: CCD's case-assignment
 * API returns {@code roleType=CASE} assignments only and never these.
 *
 * <p>The legacy markers {@code caseworker-pcs-solicitor}, {@code [CLAIMANTSOLICITOR]} and
 * {@code [DEFENDANTSOLICITOR]} are deliberately absent - they are being retired under HDPI-7333.
 */
public final class OrganisationUserRoles {

    /**
     * {@code claimant} covers local authority and other-organisation users, which share the
     * {@code prof-org-claimant-access} access type; solicitor organisations get the two
     * solicitor roles instead.
     */
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
