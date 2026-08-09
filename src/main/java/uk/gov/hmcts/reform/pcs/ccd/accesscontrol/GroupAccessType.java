package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.OrganisationProfile.LOCALAUTH_PROFILE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.OrganisationProfile.OTHER_CHARITY_PROFILE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.OrganisationProfile.OTHER_NFP_PROFILE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.OrganisationProfile.OTHER_PROP_PROFILE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.OrganisationProfile.OTHER_REALT_PROFILE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.OrganisationProfile.SOLICITOR_PROFILE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.DUTY_ADVISOR_REQUEST;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.GA_CLAIMANT_SOLICITOR;

import java.util.List;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.CCDAccessGroup;
import uk.gov.hmcts.ccd.sdk.api.HasRole;

@Getter
public enum GroupAccessType implements CCDAccessGroup {

    LOCAL_AUTHORITY_CLAIMANT_ACCESS(
        LOCALAUTH_PROFILE.getId(), "prof-org-claimant-access",
        "Grants claimant access on all cases associated with this organisation", "",
        UserRole.CLAIMANT, List.of(UserRole.CLAIMANT.getAccessProfiles()),
        1, true, true, true, true
    ) {
        @Override
        public HasRole getCaseAssignedRoleField() {
            return UserRole.CLAIMANT;
        }
    },
    REAL_ESTATE_ORG_CLAIMANT_ACCESS(
        OTHER_REALT_PROFILE.getId(), "prof-org-claimant-access",
        "Grants claimant access on all cases associated with this organisation", "",
        UserRole.CLAIMANT, List.of(UserRole.CLAIMANT.getAccessProfiles()),
        2, true, true, true, true
    ) {
        @Override
        public HasRole getCaseAssignedRoleField() {
            return UserRole.CLAIMANT;
        }
    },
    PROPERTY_CONSTRUCTION_ORG_CLAIMANT_ACCESS(
        OTHER_PROP_PROFILE.getId(), "prof-org-claimant-access",
        "Grants claimant access on all cases associated with this organisation", "",
        UserRole.CLAIMANT, List.of(UserRole.CLAIMANT.getAccessProfiles()),
        3, true, true, true, true
    ) {
        @Override
        public HasRole getCaseAssignedRoleField() {
            return UserRole.CLAIMANT;
        }
    },
    NOT_FOR_PROFIT_ORG_CLAIMANT_ACCESS(
        OTHER_NFP_PROFILE.getId(), "prof-org-claimant-access",
        "Grants claimant access on all cases associated with this organisation", "",
        UserRole.CLAIMANT, List.of(UserRole.CLAIMANT.getAccessProfiles()),
        4, true, true, true, true
    ) {
        @Override
        public HasRole getCaseAssignedRoleField() {
            return UserRole.CLAIMANT;
        }
    },
    CHARITY_ORG_CLAIMANT_ACCESS(
        OTHER_CHARITY_PROFILE.getId(), "prof-org-claimant-access",
        "Grants claimant access on all cases associated with this organisation", "",
        UserRole.CLAIMANT, List.of(UserRole.CLAIMANT.getAccessProfiles()),
        5, true, true, true, true
    ) {
        @Override
        public HasRole getCaseAssignedRoleField() {
            return UserRole.CLAIMANT;
        }
    },
    SOLICITOR_ORG_CLAIMANT_ACCESS(
        SOLICITOR_PROFILE.getId(), "solicitor-org-claimant-access",
        "Grants solicitors claimant access on all cases associated with this organisation", "",
        GA_CLAIMANT_SOLICITOR, List.of(GA_CLAIMANT_SOLICITOR.getAccessProfiles()),
        6, true, true, true, true
    ) {
        @Override
        public HasRole getCaseAssignedRoleField() {
            return GA_CLAIMANT_SOLICITOR;
        }
    },
    SOLICITOR_ORG_DEFENDANT_ACCESS(
        SOLICITOR_PROFILE.getId(), "solicitor-org-defendant-access",
        "Grants solicitors defendant access on all cases associated with this organisation", "",
        GA_CLAIMANT_SOLICITOR, List.of(GA_CLAIMANT_SOLICITOR.getAccessProfiles()),
        7, true, true, true, true
    ) {
        @Override
        public HasRole getCaseAssignedRoleField() {
            return GA_CLAIMANT_SOLICITOR;
        }
    },
    DUTY_ADVISOR_ACCESS(
        SOLICITOR_PROFILE.getId(), "duty-advisor-access",
        "Grants solicitors access to request time-bound duty-advisor role", "",
        DUTY_ADVISOR_REQUEST, List.of(DUTY_ADVISOR_REQUEST.getAccessProfiles()),
        1, true, true, true, true
    ) {
        @Override
        public HasRole getCaseAssignedRoleField() {
            return DUTY_ADVISOR_REQUEST;
        }
    };

    private final String organisationProfileId;
    private final String accessTypeId;
    private final String description;
    private final String hintText;
    private final HasRole groupRoleName;
    private final List<String> groupRoleAccessProfiles;
    private final int displayOrder;
    private final boolean accessMandatory;
    private final boolean accessDefault;
    private final boolean display;
    private final boolean groupAccessEnabled;

    GroupAccessType(String organisationProfileId, String accessTypeId, String description, String hintText,
                    HasRole groupRoleName, List<String> groupRoleAccessProfiles,
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
        this.groupRoleName = groupRoleName;
        this.groupRoleAccessProfiles = groupRoleAccessProfiles;
        this.groupAccessEnabled = groupAccessEnabled;
    }

    /**
     * Builds the case access group ID template from this constant's own {@code accessTypeId} and
     * {@code groupRoleName}, e.g. {@code "PCS:PCS:solicitor-org-claimant-access:claimant_solicitor:$ORGID$"}.
     */
    public String getCaseAccessGroupIdTemplate() {
        return "PCS:PCS:" + accessTypeId + ":" + groupRoleName.getRole() + ":$ORGID$";
    }
}
