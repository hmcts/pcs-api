package uk.gov.hmcts.reform.pcs.ccd.util;

import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.GroupAccessType;
import uk.gov.hmcts.ccd.sdk.type.CaseAccessGroup;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Builds the CaseAccessGroups the data store matches against users' role assignments. The id must be
 * byte-identical to the one PRM builds from the same AccessTypeRole template; a mismatch fails closed.
 */
public final class CaseAccessGroupsUtil {

    public static final String CCD_ALL_CASES_ACCESS = "CCD:all-cases-access";
    static final String ORG_IDENTIFIER_TEMPLATE = "$ORGID$";

    /** Claimant only for now; the defendant side belongs to the notice-of-change work. */
    private static final Set<PartyRole> DERIVED_ROLES = Set.of(PartyRole.CLAIMANT);

    private CaseAccessGroupsUtil() {
    }

    public static List<ListValue<CaseAccessGroup>> deriveCaseAccessGroups(Set<PartyEntity> parties) {
        List<CaseAccessGroup> caseAccessGroups = new ArrayList<>();

        parties.forEach(party -> derivedRole(party)
            .flatMap(role -> GroupAccessType.caseAccessGroupIdTemplateFor(party.getOrganisationProfileId(), role))
            .ifPresent(template -> caseAccessGroups.add(new CaseAccessGroup(
                CCD_ALL_CASES_ACCESS,
                template.replace(ORG_IDENTIFIER_TEMPLATE, party.getOrganisationId())))));

        return caseAccessGroups.stream()
            .map(CaseAccessGroup::getCaseAccessGroupId)
            .distinct()
            .sorted()
            .map(CaseAccessGroupsUtil::asIdentifiedGroupItem)
            .toList();
    }

    /**
     * Every item needs an id (data store skips id-less ones); derived from the group id
     * so it is stable across reads.
     */
    private static ListValue<CaseAccessGroup> asIdentifiedGroupItem(String groupId) {
        return ListValue.<CaseAccessGroup>builder()
            .id(UUID.nameUUIDFromBytes(groupId.getBytes(StandardCharsets.UTF_8)).toString())
            .value(new CaseAccessGroup(CCD_ALL_CASES_ACCESS, groupId))
            .build();
    }

    private static Optional<PartyRole> derivedRole(PartyEntity party) {
        if (party.getOrganisationId() == null) {
            return Optional.empty();
        }
        if (party.isClaimCreator()) {
            return Optional.of(PartyRole.CLAIMANT);
        }
        return party.getClaimParties().stream()
            .map(ClaimPartyEntity::getRole)
            .filter(DERIVED_ROLES::contains)
            .findFirst();
    }


}
