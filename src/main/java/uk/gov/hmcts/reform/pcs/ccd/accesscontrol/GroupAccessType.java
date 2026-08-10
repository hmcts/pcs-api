package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.OrganisationProfile.LOCALAUTH_PROFILE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.OrganisationProfile.OTHER_CHARITY_PROFILE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.OrganisationProfile.OTHER_NFP_PROFILE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.OrganisationProfile.OTHER_PROP_PROFILE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.OrganisationProfile.OTHER_REALT_PROFILE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.OrganisationProfile.SOLICITOR_PROFILE;

import java.util.List;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.CCDAccessGroup;
import uk.gov.hmcts.ccd.sdk.api.HasRole;

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
 * <p>The role-valued members are resolved in method bodies rather than captured as constructor
 * arguments. This enum and {@link AccessProfile} reference each other, so their static
 * initialisations are circular: a role read while building a constant comes out null in whichever
 * enum initialises second, because the JVM will not re-enter an in-progress {@code <clinit>}. A
 * method body is not evaluated until the SDK builds the config, by which point both enums are
 * complete.</p>
 */
@Getter
public enum GroupAccessType implements CCDAccessGroup {

    LOCAL_AUTHORITY_CLAIMANT_ACCESS(
        LOCALAUTH_PROFILE.getId(), "prof-org-claimant-access",
        "Grants claimant access on all cases associated with this organisation", "",
        1, true, true, true, true
    ),
    REAL_ESTATE_ORG_CLAIMANT_ACCESS(
        OTHER_REALT_PROFILE.getId(), "prof-org-claimant-access",
        "Grants claimant access on all cases associated with this organisation", "",
        2, true, true, true, true
    ),
    PROPERTY_CONSTRUCTION_ORG_CLAIMANT_ACCESS(
        OTHER_PROP_PROFILE.getId(), "prof-org-claimant-access",
        "Grants claimant access on all cases associated with this organisation", "",
        3, true, true, true, true
    ),
    NOT_FOR_PROFIT_ORG_CLAIMANT_ACCESS(
        OTHER_NFP_PROFILE.getId(), "prof-org-claimant-access",
        "Grants claimant access on all cases associated with this organisation", "",
        4, true, true, true, true
    ),
    CHARITY_ORG_CLAIMANT_ACCESS(
        OTHER_CHARITY_PROFILE.getId(), "prof-org-claimant-access",
        "Grants claimant access on all cases associated with this organisation", "",
        5, true, true, true, true
    ),
    SOLICITOR_ORG_CLAIMANT_ACCESS(
        SOLICITOR_PROFILE.getId(), "solicitor-org-claimant-access",
        "Grants solicitors claimant access on all cases associated with this organisation", "",
        6, true, true, true, true
    ),
    SOLICITOR_ORG_DEFENDANT_ACCESS(
        SOLICITOR_PROFILE.getId(), "solicitor-org-defendant-access",
        "Grants solicitors defendant access on all cases associated with this organisation", "",
        7, true, true, true, true
    ),
    DUTY_ADVISOR_ACCESS(
        SOLICITOR_PROFILE.getId(), "duty-advisor-access",
        "Grants solicitors access to request time-bound duty-advisor role", "",
        8, true, true, true, true
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

    GroupAccessType(String organisationProfileId, String accessTypeId, String description, String hintText,
                    int displayOrder, boolean accessMandatory, boolean accessDefault, boolean display,
                    boolean groupAccessEnabled) {
        this.organisationProfileId = organisationProfileId;
        this.accessTypeId = accessTypeId;
        this.accessMandatory = accessMandatory;
        this.accessDefault = accessDefault;
        this.display = display;
        this.description = description;
        this.hintText = hintText;
        this.displayOrder = displayOrder;
        this.groupAccessEnabled = groupAccessEnabled;
    }

    /**
     * The role PRM mints per organisation for this access type. Also the case role naming the
     * OrganisationPolicy that supplies the organisation ID, since for every access type here the
     * two are the same role.
     */
    private AccessProfile groupRole() {
        return switch (this) {
            case LOCAL_AUTHORITY_CLAIMANT_ACCESS, REAL_ESTATE_ORG_CLAIMANT_ACCESS,
                 PROPERTY_CONSTRUCTION_ORG_CLAIMANT_ACCESS, NOT_FOR_PROFIT_ORG_CLAIMANT_ACCESS,
                 CHARITY_ORG_CLAIMANT_ACCESS -> AccessProfile.CLAIMANT;
            case SOLICITOR_ORG_CLAIMANT_ACCESS, SOLICITOR_ORG_DEFENDANT_ACCESS -> AccessProfile.GA_CLAIMANT_SOLICITOR;
            case DUTY_ADVISOR_ACCESS -> AccessProfile.DUTY_ADVISOR_REQUEST;
        };
    }

    @Override
    public HasRole getGroupRoleName() {
        return groupRole();
    }

    @Override
    public HasRole getCaseAssignedRoleField() {
        return groupRole();
    }

    @Override
    public List<String> getGroupRoleAccessProfiles() {
        return List.of(groupRole().getRole());
    }

    /**
     * Builds the case access group ID template from this constant's own {@code accessTypeId} and
     * group role, e.g. {@code "PCS:PCS:solicitor-org-claimant-access:claimant_solicitor:$ORGID$"}.
     */
    @Override
    public String getCaseAccessGroupIdTemplate() {
        return "PCS:PCS:" + accessTypeId + ":" + groupRole().getRole() + ":$ORGID$";
    }
}
