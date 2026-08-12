package uk.gov.hmcts.reform.pcs.ccd.util;


import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Arrays;
import java.util.Set;
import uk.gov.hmcts.ccd.sdk.type.CaseAccessGroup;
import uk.gov.hmcts.ccd.sdk.type.ListValue;

import java.util.List;
import java.util.UUID;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.GroupAccessType;
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
     * profile. Parties without an organisation (citizens) contribute nothing.
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

            Arrays.stream(GroupAccessType.values())
                .filter(accessType -> accessType.getOrganisationProfileId().equals(orgProfileId))
                .map(GroupAccessType::getCaseAccessGroupIdTemplate).findFirst()
                .ifPresent(template -> {
                    CaseAccessGroup group = new CaseAccessGroup(
                        CCD_ALL_CASES_ACCESS,
                        template.replace(ORG_IDENTIFIER_TEMPLATE, organisationId));
                    caseAccessGroups.add(group);
                });
        });
        // Stable output: parties come from a HashSet and this is recomputed on every read, so a random
        // id would make an unchanged case differ read to read. The group id is the identity, so derive
        // the list id from it and sort on it.
        return caseAccessGroups.stream()
            .sorted(Comparator.comparing(CaseAccessGroup::getCaseAccessGroupId))
            .map(group -> ListValue.<CaseAccessGroup>builder()
                .id(UUID.nameUUIDFromBytes(group.getCaseAccessGroupId().getBytes(StandardCharsets.UTF_8)).toString())
                .value(group)
                .build())
            .toList();
    }
}
