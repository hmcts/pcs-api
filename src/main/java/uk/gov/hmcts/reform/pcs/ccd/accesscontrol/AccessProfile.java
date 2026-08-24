package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import static java.util.Arrays.stream;
import static uk.gov.hmcts.ccd.sdk.api.Permission.CRU;
import static uk.gov.hmcts.ccd.sdk.api.Permission.R;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.GroupAccessType.CHARITY_ORG_CLAIMANT_ACCESS;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.GroupAccessType.DUTY_ADVISOR_ACCESS;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.GroupAccessType.LOCAL_AUTHORITY_CLAIMANT_ACCESS;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.GroupAccessType.NOT_FOR_PROFIT_ORG_CLAIMANT_ACCESS;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.GroupAccessType.PROPERTY_CONSTRUCTION_ORG_CLAIMANT_ACCESS;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.GroupAccessType.REAL_ESTATE_ORG_CLAIMANT_ACCESS;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.GroupAccessType.SOLICITOR_ORG_CLAIMANT_ACCESS;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.GroupAccessType.SOLICITOR_ORG_DEFENDANT_ACCESS;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.CCDAccessGroup;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

@Getter
public enum AccessProfile implements HasRole {

    CREATOR("[CREATOR]", CRU),
    RAS_VALIDATOR("caseworker-ras-validation", Set.of(R)),
    CITIZEN("citizen", CRU),
    DEFENDANT("[DEFENDANT]", CRU),
    CLAIMANT_SOLICITOR("[CLAIMANTSOLICITOR]", CRU),
    DEFENDANT_SOLICITOR("[DEFENDANTSOLICITOR]", CRU),
    PCS_CASE_WORKER("caseworker-pcs", Set.of(R)),
    PCS_SOLICITOR("caseworker-pcs-solicitor", CRU),

    GA_CLAIMANT("claimant", CRU,
             LOCAL_AUTHORITY_CLAIMANT_ACCESS,
             REAL_ESTATE_ORG_CLAIMANT_ACCESS,
             PROPERTY_CONSTRUCTION_ORG_CLAIMANT_ACCESS,
             NOT_FOR_PROFIT_ORG_CLAIMANT_ACCESS,
             CHARITY_ORG_CLAIMANT_ACCESS),
    GA_CLAIMANT_SOLICITOR("claimant-solicitor", CRU, SOLICITOR_ORG_CLAIMANT_ACCESS),
    GA_DEFENDANT_SOLICITOR("defendant-solicitor", CRU, SOLICITOR_ORG_DEFENDANT_ACCESS),
    DUTY_ADVISOR_REQUEST("duty-advisor-request", Set.of(R), DUTY_ADVISOR_ACCESS),

    JUDGE("judge", CRU),
    FEE_PAID_JUDGE("fee-paid-judge", CRU),
    CIRCUIT_JUDGE("circuit-judge", CRU),
    LEADERSHIP_JUDGE("leadership-judge", CRU),
    CTSC_TEAM_LEADER("ctsc-team-leader", Permission.CRU),
    CTSC_ADMIN("ctsc", Permission.CRU),
    HEARING_CENTRE_TEAM_LEADER("hearing-centre-team-leader", Permission.CRU),
    HEARING_CENTRE_ADMIN("hearing-centre-admin", Permission.CRU),
    WLU_TEAM_LEADER("wlu-team-leader", Permission.CRU),
    WLU_ADMIN("wlu-admin", Permission.CRU),
    GS_PROFILE("GS_profile", Set.of(R)),
    SYSTEM_USER("pcs-system-update", CRU),
    WA_SYSTEM_USER("caseworker-wa-task-configuration", CRU),
    ORGANISATION_CASE_ACCESS_ADMINISTRATOR("caseworker-caa", CRU);


    @JsonValue
    private final String role;
    private final Set<Permission> caseTypePermissions;

    /**
     * The access types this role participates in, empty if none. One access type per organisation
     * profile, since the definition store identifies an access type by
     * {@code (accessTypeId, organisationProfileId)}.
     *
     * <p>Safe to hold as a field because {@link GroupAccessType} resolves its own roles in method
     * bodies: its static initialisation never reads this enum, so only one side of the cycle has to
     * be lazy.</p>
     */
    private final List<CCDAccessGroup> accessGroups;

    AccessProfile(String role, Set<Permission> permissions, CCDAccessGroup... accessGroups) {
        this.role = role;
        this.caseTypePermissions = permissions;
        this.accessGroups = List.of(accessGroups);
    }

    public static String[] toRoles(AccessProfile... profiles) {
        return stream(profiles)
            .map(AccessProfile::getRole)
            .toArray(String[]::new);
    }

    public String getCaseTypePermissions() {
        return Permission.toString(caseTypePermissions);
    }
}
