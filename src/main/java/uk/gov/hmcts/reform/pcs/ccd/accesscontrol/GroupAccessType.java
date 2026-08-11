package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.OrganisationProfile.LOCALAUTH_PROFILE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.OrganisationProfile.OTHER_CHARITY_PROFILE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.OrganisationProfile.OTHER_NFP_PROFILE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.OrganisationProfile.OTHER_PROP_PROFILE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.OrganisationProfile.OTHER_REALT_PROFILE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.OrganisationProfile.SOLICITOR_PROFILE;

import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.CCDAccessGroup;

/**
 * Organisational access types for group access.
 *
 * <p>A constant reaches the generated definition by being returned from
 * {@link AccessProfile#getAccessGroups()}: the SDK derives {@code AccessType} and
 * {@code AccessTypeRole} rows by walking the case's role class, so an access type no role points at
 * is silently dropped.</p>
 *
 * <p>The five claimant constants share the {@code prof-org-claimant-access} access type ID and
 * differ only by organisation profile, which the definition store treats as five distinct access
 * types — its {@code AccessTypesValidator} keys on
 * {@code (caseType, jurisdiction, accessTypeId, organisationProfileId)}.</p>
 *
 * <p>Nothing here refers back to {@link AccessProfile}, so the two enums are not circularly
 * initialised and every member can be a constructor argument. Roles are named as literals for that
 * reason: reading {@code AccessProfile.CLAIMANT} while building a constant would come out null in
 * whichever enum initialised second, because the JVM will not re-enter an in-progress
 * {@code <clinit>}.</p>
 */
@Getter
public enum GroupAccessType implements CCDAccessGroup {

    LOCAL_AUTHORITY_CLAIMANT_ACCESS(
        LOCALAUTH_PROFILE.getId(), "prof-org-claimant-access",
        "Grants claimant access on all cases associated with this organisation", "",
        1, true, true, true, true, "claimant"
    ),
    REAL_ESTATE_ORG_CLAIMANT_ACCESS(
        OTHER_REALT_PROFILE.getId(), "prof-org-claimant-access",
        "Grants claimant access on all cases associated with this organisation", "",
        2, true, true, true, true, "claimant"
    ),
    PROPERTY_CONSTRUCTION_ORG_CLAIMANT_ACCESS(
        OTHER_PROP_PROFILE.getId(), "prof-org-claimant-access",
        "Grants claimant access on all cases associated with this organisation", "",
        3, true, true, true, true, "claimant"
    ),
    NOT_FOR_PROFIT_ORG_CLAIMANT_ACCESS(
        OTHER_NFP_PROFILE.getId(), "prof-org-claimant-access",
        "Grants claimant access on all cases associated with this organisation", "",
        4, true, true, true, true, "claimant"
    ),
    CHARITY_ORG_CLAIMANT_ACCESS(
        OTHER_CHARITY_PROFILE.getId(), "prof-org-claimant-access",
        "Grants claimant access on all cases associated with this organisation", "",
        5, true, true, true, true, "claimant"
    ),
    SOLICITOR_ORG_CLAIMANT_ACCESS(
        SOLICITOR_PROFILE.getId(), "solicitor-org-claimant-access",
        "Grants solicitors claimant access on all cases associated with this organisation", "",
        6, true, true, true, true, "claimant_solicitor"
    ),
    SOLICITOR_ORG_DEFENDANT_ACCESS(
        SOLICITOR_PROFILE.getId(), "solicitor-org-defendant-access",
        "Grants solicitors defendant access on all cases associated with this organisation", "",
        7, true, true, true, true, "claimant_solicitor"
    ),
    DUTY_ADVISOR_ACCESS(
        SOLICITOR_PROFILE.getId(), "duty-advisor-access",
        "Grants solicitors access to request time-bound duty-advisor role", "",
        8, true, true, true, true, "duty-advisor-request"
    );

    private final String organisationProfileId;
    private final String accessTypeId;
    private final String description;
    private final String hintText;
    private final int displayOrder;
    private final boolean accessMandatory;
    private final boolean accessDefault;
    private final boolean display;
    private final boolean groupAccessEnabled;

    /**
     * The case role naming the OrganisationPolicy that supplies the organisation ID. A role name
     * rather than a field name, despite the column's title, and given as a literal rather than
     * {@code AccessProfile.CLAIMANT.getRole()} so that this enum holds no reference back into
     * {@link AccessProfile}. The definition store validates it against
     * {@code RoleToAccessProfiles.RoleName}, so it must be a declared role.
     */
    private final String caseAssignedRoleField;

    GroupAccessType(String organisationProfileId, String accessTypeId, String description, String hintText,
                    int displayOrder, boolean accessMandatory, boolean accessDefault, boolean display,
                    boolean groupAccessEnabled, String caseAssignedRoleField) {
        this.organisationProfileId = organisationProfileId;
        this.accessTypeId = accessTypeId;
        this.accessMandatory = accessMandatory;
        this.accessDefault = accessDefault;
        this.display = display;
        this.description = description;
        this.hintText = hintText;
        this.displayOrder = displayOrder;
        this.groupAccessEnabled = groupAccessEnabled;
        this.caseAssignedRoleField = caseAssignedRoleField;
    }

    /**
     * Builds the case access group ID template from this constant's own {@code accessTypeId} and
     * case-assigned role, e.g.
     * {@code "PCS:PCS:solicitor-org-claimant-access:claimant_solicitor:$ORGID$"}.
     */
    @Override
    public String getCaseAccessGroupIdTemplate() {
        return "PCS:PCS:" + accessTypeId + ":" + caseAssignedRoleField + ":$ORGID$";
    }
}
