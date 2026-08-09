package uk.gov.hmcts.reform.pcs.ccd.util;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import uk.gov.hmcts.ccd.sdk.type.CaseAccessGroup;
import uk.gov.hmcts.ccd.sdk.type.ListValue;

import java.util.List;
import java.util.UUID;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.AccessTypes;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;

/**
 * Derives the CaseAccessGroups the data store's group matcher compares against the
 * caseAccessGroupId on users' role assignments. Must produce exactly the id PRM builds
 * from the caseAccessGroupIdTemplate declared in CaseType's AccessTypeRole config -
 * both sides are manufactured from that one declaration; a mismatch fails closed.
 */
public final class CaseAccessGroupsUtil {

    public static final String CCD_ALL_CASES_ACCESS = "CCD:all-cases-access";
    static final String ORGANISATION_PROFILE = "ORGANISATION_PROFILE";
    static final String ORG_IDENTIFIER_TEMPLATE = "$ORGID$";

    private CaseAccessGroupsUtil() {
    }

    /**
     * Derives one group per party organisation, the template chosen by the organisation's
     * profile. Parties without an organisation (citizens, unrepresented defendants) contribute
     * nothing - they keep their per-case access routes. PRD always derives at least one profile
     * for an organisation, so a party holding an organisation without profiles is broken data -
     * better to fail than mint a group id the matcher can never match.
     */
    public static List<ListValue<CaseAccessGroup>> deriveCaseAccessGroups(Set<PartyEntity> parties) {
        List<CaseAccessGroup> caseAccessGroups = new ArrayList<>();
        parties.forEach(party -> {
            var organisationId = party.getOrganisationId();
            if (organisationId == null) {
                return;
            }
            List<String> organisationProfileIds = party.getOrganisationProfileIds();
            if (organisationProfileIds == null || organisationProfileIds.isEmpty()) {
                throw new IllegalArgumentException(
                    "Organisation profile ids are required to derive case access groups for party "
                        + party.getId());
            }

            String orgProfileId = organisationProfileIds.stream()
                .filter(profileId -> !profileId.equals(ORGANISATION_PROFILE))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                    "No valid organisation profile id found for organisation " + organisationId));

            // Same token replacement the data store's CaseAccessGroupUtils applies centrally, so
            // the enum's template strings stay identical to the AccessTypeRole config.
            Arrays.stream(AccessTypes.values())
                .filter(accessType -> accessType.getOrganisationProfileId().equals(orgProfileId))
                .map(AccessTypes::getCaseAccessGroupIdTemplate).findFirst()
                .ifPresent(template -> {
                    CaseAccessGroup group = new CaseAccessGroup(
                        CCD_ALL_CASES_ACCESS,
                        template.replace(ORG_IDENTIFIER_TEMPLATE, organisationId));
                    caseAccessGroups.add(group);
                });
        });
        return caseAccessGroups.stream()
            .map(group ->
                     ListValue.<CaseAccessGroup>builder().id(UUID.randomUUID().toString()).value(group).build()
            ).toList();
    }
}
