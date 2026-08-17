package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.OrganisationProfile.LOCALAUTH_PROFILE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.OrganisationProfile.OTHER_CHARITY_PROFILE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.OrganisationProfile.OTHER_NFP_PROFILE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.OrganisationProfile.OTHER_PROP_PROFILE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.OrganisationProfile.OTHER_REALT_PROFILE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.OrganisationProfile.SOLICITOR_PROFILE;

import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.CCDAccessGroup;

@Getter
public enum GroupAccessType implements CCDAccessGroup {

    LOCAL_AUTHORITY_CLAIMANT_ACCESS(
        LOCALAUTH_PROFILE, "prof-org-claimant-access", "claimant",
        "Grants claimant access on all cases associated with this organisation", 1
    ),
    REAL_ESTATE_ORG_CLAIMANT_ACCESS(
        OTHER_REALT_PROFILE, "prof-org-claimant-access", "claimant",
        "Grants claimant access on all cases associated with this organisation", 2
    ),
    PROPERTY_CONSTRUCTION_ORG_CLAIMANT_ACCESS(
        OTHER_PROP_PROFILE, "prof-org-claimant-access", "claimant",
        "Grants claimant access on all cases associated with this organisation", 3
    ),
    NOT_FOR_PROFIT_ORG_CLAIMANT_ACCESS(
        OTHER_NFP_PROFILE, "prof-org-claimant-access", "claimant",
        "Grants claimant access on all cases associated with this organisation", 4
    ),
    CHARITY_ORG_CLAIMANT_ACCESS(
        OTHER_CHARITY_PROFILE, "prof-org-claimant-access", "claimant",
        "Grants claimant access on all cases associated with this organisation", 5
    ),
    SOLICITOR_ORG_CLAIMANT_ACCESS(
        SOLICITOR_PROFILE, "solicitor-org-claimant-access", "claimant-solicitor",
        "Grants solicitors claimant access on all cases associated with this organisation", 6
    ),
    SOLICITOR_ORG_DEFENDANT_ACCESS(
        SOLICITOR_PROFILE, "solicitor-org-defendant-access", "defendant-solicitor",
        "Grants solicitors defendant access on all cases associated with this organisation", 7
    ),
    DUTY_ADVISOR_ACCESS(
        SOLICITOR_PROFILE, "duty-advisor-access", "duty-advisor-request",
        "Grants solicitors access to request time-bound duty-advisor role",
        "Assign to Users who may need to request time-bound duty-advisor access", 8,
        false, false, true, true
    );

    /**
     * The definition store rejects the import with "'HintText' must be set for 'Display' to be
     * used" when a displayed access type has no hint, so every constant needs one.
     */
    private static final String ASSIGN_HINT =
        "Assign to Users to enable access to all cases associated with this organisation";

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

    GroupAccessType(OrganisationProfile orgProfileId, String accessTypeId, String caseAssignedRoleField,
                    String description, int displayOrder) {
        this.organisationProfileId = orgProfileId.getId();
        this.accessTypeId = accessTypeId;
        this.accessMandatory = true;
        this.accessDefault = true;
        this.display = true;
        this.description = description;
        this.hintText = ASSIGN_HINT;
        this.groupAccessEnabled = true;
        this.displayOrder = displayOrder;
        this.caseAssignedRoleField = caseAssignedRoleField;
    }

    GroupAccessType(OrganisationProfile orgProfileId, String accessTypeId, String caseAssignedRoleField,
                    String description, String hintText, int displayOrder,
                    boolean accessMandatory, boolean accessDefault, boolean display, boolean groupAccessEnabled) {
        this.organisationProfileId = orgProfileId.getId();
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
     * group role, e.g. {@code "PCS:PCS:solicitor-org-claimant-access:claimant-solicitor:$ORGID$"}.
     */
    @Override
    public String getCaseAccessGroupIdTemplate() {
        return "PCS:PCS:" + accessTypeId + ":" + caseAssignedRoleField + ":$ORGID$";
    }

    /**
     * Cannot be left blank: the definition reader parses this column unconditionally and an empty
     * value fails the import with DateTimeParseException. Far future so it is not a live expiry -
     * every access type stops working on this date, so move it rather than let it arrive.
     */
    @Override
    public String getLiveTo() {
        return "01/01/2099";
    }
}
