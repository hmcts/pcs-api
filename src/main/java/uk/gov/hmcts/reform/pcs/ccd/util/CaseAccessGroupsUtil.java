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
    static final String CLAIMANT_SOLICITOR_GROUP_ID_TEMPLATE =
        "PCS:PCS:solicitor-org-claimant-access:claimant_solicitor:%s";

    private CaseAccessGroupsUtil() {
    }

    public static List<ListValue<CaseAccessGroup>> deriveCaseAccessGroups(String organisationId) {
        CaseAccessGroup group = new CaseAccessGroup(
            CCD_ALL_CASES_ACCESS,
            CLAIMANT_SOLICITOR_GROUP_ID_TEMPLATE.formatted(organisationId)
        );
        return List.of(ListValue.<CaseAccessGroup>builder()
                           .id(UUID.randomUUID().toString())
                           .value(group)
                           .build());
    }
}
