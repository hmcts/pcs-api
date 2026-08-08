package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasRole;

@Getter
public enum AccessTypes {

    SOLICITOR_ORG_CLAIMANT_ACCESS(
        "solicitor-org-claimant-access",
        "SOLICITOR_PROFILE",
        true,
        true,
        true,
        "Can manage all cases associated with this organisation as claimant",
        "Assign to Users to enable claimant access to all cases associated with this organisation",
        1,
        UserRole.CLAIMANT_SOLICITOR_ORG,
        true,
        "PUBLICLAW:CARE_SUPERVISION_EPO:caseworker-approver-group:$ORGID$");

    private final String accessTypeId;
    private final String organisationProfileId;
    private final boolean accessMandatory;
    private final boolean accessDefault;
    private final boolean display;
    private final String description;
    private final String hintText;
    private final int displayOrder;
    private final HasRole groupRoleName;
    private final boolean groupAccessEnabled;
    private final String caseAccessGroupIdTemplate;

    AccessTypes(String accessTypeId, String organisationProfileId, boolean accessMandatory, boolean accessDefault,
                boolean display, String description, String hintText, int displayOrder,
                HasRole groupRoleName, boolean groupAccessEnabled, String caseAccessGroupIdTemplate) {
        this.accessTypeId = accessTypeId;
        this.organisationProfileId = organisationProfileId;
        this.accessMandatory = accessMandatory;
        this.accessDefault = accessDefault;
        this.display = display;
        this.description = description;
        this.hintText = hintText;
        this.displayOrder = displayOrder;
        this.groupRoleName = groupRoleName;
        this.groupAccessEnabled = groupAccessEnabled;
        this.caseAccessGroupIdTemplate = caseAccessGroupIdTemplate;
    }

    public String getAccessTypeId() {
        return name();
    }
}
