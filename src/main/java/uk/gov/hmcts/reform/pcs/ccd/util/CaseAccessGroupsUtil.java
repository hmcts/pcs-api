package uk.gov.hmcts.reform.pcs.ccd.util;

import uk.gov.hmcts.ccd.sdk.type.CaseAccessGroup;
import uk.gov.hmcts.ccd.sdk.type.ListValue;

import java.util.List;
import java.util.UUID;

/**
 * Derives the CaseAccessGroups the data store's group matcher compares against the
 * caseAccessGroupId on users' role assignments. Must produce exactly the id PRM builds
 * from the caseAccessGroupIdTemplate declared in CaseType's AccessTypeRole config -
 * both sides are manufactured from that one declaration; a mismatch fails closed.
 */
public final class CaseAccessGroupsUtil {

    public static final String CCD_ALL_CASES_ACCESS = "CCD:all-cases-access";
    static final String SOLICITOR_PROFILE = "SOLICITOR_PROFILE";
    static final String CLAIMANT_SOLICITOR_GROUP_ID_TEMPLATE =
        "PCS:PCS:solicitor-org-claimant-access:claimant_solicitor:%s";
    static final String PROF_ORG_CLAIMANT_GROUP_ID_TEMPLATE =
        "PCS:PCS:prof-org-claimant-access:claimant:%s";

    private CaseAccessGroupsUtil() {
    }

    /**
     * Selects the claimant-side template matching the creating organisation's profile. PRD always
     * derives at least one profile for an organisation, so a case holding an organisation without
     * a profile is broken data - better to fail than mint a group id the matcher can never match.
     */
    public static List<ListValue<CaseAccessGroup>> deriveCaseAccessGroups(String organisationId,
                                                                          String organisationProfileId) {
        if (organisationProfileId == null) {
            throw new IllegalArgumentException(
                "Organisation profile id is required to derive case access groups for organisation "
                    + organisationId);
        }
        String template = SOLICITOR_PROFILE.equals(organisationProfileId)
            ? CLAIMANT_SOLICITOR_GROUP_ID_TEMPLATE
            : PROF_ORG_CLAIMANT_GROUP_ID_TEMPLATE;
        CaseAccessGroup group = new CaseAccessGroup(
            CCD_ALL_CASES_ACCESS,
            template.formatted(organisationId)
        );
        return List.of(ListValue.<CaseAccessGroup>builder()
                           .id(UUID.randomUUID().toString())
                           .value(group)
                           .build());
    }
}
