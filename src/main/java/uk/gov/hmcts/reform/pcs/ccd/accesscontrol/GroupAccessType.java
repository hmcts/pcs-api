package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.OrganisationProfile.LOCALAUTH_PROFILE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.OrganisationProfile.OTHER_CHARITY_PROFILE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.OrganisationProfile.OTHER_NFP_PROFILE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.OrganisationProfile.OTHER_PROP_PROFILE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.OrganisationProfile.OTHER_REALT_PROFILE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.OrganisationProfile.SOLICITOR_PROFILE;
import static uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole.CLAIMANT;
import static uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole.DEFENDANT;

import lombok.Getter;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import uk.gov.hmcts.ccd.sdk.api.CCDAccessGroup;

@Getter
public enum GroupAccessType implements CCDAccessGroup {

    LOCAL_AUTHORITY_CLAIMANT_ACCESS(
        LOCALAUTH_PROFILE, CLAIMANT, "prof-org-claimant-access", "claimant",
        "Grants claimant access on all cases associated with this organisation", 1
    ),
    REAL_ESTATE_ORG_CLAIMANT_ACCESS(
        OTHER_REALT_PROFILE, CLAIMANT, "prof-org-claimant-access", "claimant",
        "Grants claimant access on all cases associated with this organisation", 2
    ),
    PROPERTY_CONSTRUCTION_ORG_CLAIMANT_ACCESS(
        OTHER_PROP_PROFILE, CLAIMANT, "prof-org-claimant-access", "claimant",
        "Grants claimant access on all cases associated with this organisation", 3
    ),
    NOT_FOR_PROFIT_ORG_CLAIMANT_ACCESS(
        OTHER_NFP_PROFILE, CLAIMANT, "prof-org-claimant-access", "claimant",
        "Grants claimant access on all cases associated with this organisation", 4
    ),
    CHARITY_ORG_CLAIMANT_ACCESS(
        OTHER_CHARITY_PROFILE, CLAIMANT, "prof-org-claimant-access", "claimant",
        "Grants claimant access on all cases associated with this organisation", 5
    ),
    SOLICITOR_ORG_CLAIMANT_ACCESS(
        SOLICITOR_PROFILE, CLAIMANT, "solicitor-org-claimant-access", "claimant-solicitor",
        "Grants solicitors claimant access on all cases associated with this organisation", 6
    ),
    SOLICITOR_ORG_DEFENDANT_ACCESS(
        SOLICITOR_PROFILE, DEFENDANT, "solicitor-org-defendant-access", "defendant-solicitor",
        "Grants solicitors defendant access on all cases associated with this organisation", 7
    ),
    DUTY_ADVISOR_ACCESS(
        SOLICITOR_PROFILE, null, "duty-advisor-access", "duty-advisor-request",
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

    private static final Map<Key, GroupAccessType> BY_PROFILE_AND_ROLE = buildIndex();

    /** Null for duty-advisor access, which is requested per case rather than stamped on one. */
    private final PartyRole partyRole;
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

    GroupAccessType(OrganisationProfile orgProfileId, PartyRole partyRole, String accessTypeId,
                    String caseAssignedRoleField, String description, int displayOrder) {
        this.partyRole = partyRole;
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

    GroupAccessType(OrganisationProfile orgProfileId, PartyRole partyRole, String accessTypeId,
                    String caseAssignedRoleField, String description, String hintText, int displayOrder,
                    boolean accessMandatory, boolean accessDefault, boolean display, boolean groupAccessEnabled) {
        this.partyRole = partyRole;
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

    private record Key(String organisationProfileId, PartyRole partyRole) { }

    private static Map<Key, GroupAccessType> buildIndex() {
        Map<Key, GroupAccessType> index = new HashMap<>();
        for (GroupAccessType accessType : values()) {
            if (accessType.partyRole != null) {
                index.put(new Key(accessType.organisationProfileId, accessType.partyRole), accessType);
            }
        }
        return Map.copyOf(index);
    }

    /**
     * The group ID template for an organisation profile acting in a party role, empty where the
     * combination has no access type. Keyed lookup, so selection does not depend on the order these
     * constants are declared in.
     */
    public static Optional<String> caseAccessGroupIdTemplateFor(String organisationProfileId, PartyRole partyRole) {
        return Optional.ofNullable(partyRole)
            .map(role -> new Key(organisationProfileId, role))
            .map(BY_PROFILE_AND_ROLE::get)
            .map(GroupAccessType::getCaseAccessGroupIdTemplate);
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
