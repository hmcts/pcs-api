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
     * The data store skips collection items that have no id, so every group needs one. Deriving it
     * from the group id rather than generating one keeps it the same on every read, and these are
     * rebuilt on each read rather than stored.
     */
    private static ListValue<CaseAccessGroup> asIdentifiedGroupItem(String groupId) {
        return ListValue.<CaseAccessGroup>builder()
            .id(UUID.nameUUIDFromBytes(groupId.getBytes(StandardCharsets.UTF_8)).toString())
            .value(new CaseAccessGroup(CCD_ALL_CASES_ACCESS, groupId))
            .build();
    }

    private static Optional<PartyRole> derivedRole(PartyEntity party) {
        if (!party.hasOrganisation()) {
            return Optional.empty();
        }
        if (claimPartiesNotCreatedYet(party)) {
            return Optional.of(PartyRole.CLAIMANT);
        }
        return party.getClaimParties().stream()
            .map(ClaimPartyEntity::getRole)
            .filter(DERIVED_ROLES::contains)
            .findFirst();
    }

    /**
     * Claim parties are only created when the claim is submitted, so before that there is no role to
     * read. createClaimantStub is the only thing that makes a party that early, so one without claim
     * parties is the claimant the case was created for.
     */
    private static boolean claimPartiesNotCreatedYet(PartyEntity party) {
        return party.getClaimParties().isEmpty();
    }


}
