package uk.gov.hmcts.reform.pcs.ccd.util;

import uk.gov.hmcts.ccd.sdk.type.CaseAccessGroup;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.GroupAccessType;
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
    public static final String ORGANISATION_PROFILE = "ORGANISATION_PROFILE";
    static final String ORG_IDENTIFIER_TEMPLATE = "$ORGID$";

    /** Claimant only for now; the defendant side belongs to the notice-of-change work. */
    private static final Set<PartyRole> DERIVED_ROLES = Set.of(PartyRole.CLAIMANT);

    private CaseAccessGroupsUtil() {
    }

    public static List<ListValue<CaseAccessGroup>> deriveCaseAccessGroups(Set<PartyEntity> parties) {
        List<CaseAccessGroup> caseAccessGroups = new ArrayList<>();

        parties.forEach(party -> derivedRole(party)
            .flatMap(role -> GroupAccessType.forProfileAndRole(organisationProfileId(party), role))
            .ifPresent(accessType -> caseAccessGroups.add(new CaseAccessGroup(
                CCD_ALL_CASES_ACCESS,
                accessType.getCaseAccessGroupIdTemplate()
                    .replace(ORG_IDENTIFIER_TEMPLATE, party.getOrganisationId())))));

        // Parties are a HashSet and this runs on every read, so derive the list id from the group id
        // rather than randomly - otherwise an unchanged case differs read to read.
        return caseAccessGroups.stream()
            .map(CaseAccessGroup::getCaseAccessGroupId)
            .distinct()
            .sorted()
            .map(groupId -> ListValue.<CaseAccessGroup>builder()
                .id(UUID.nameUUIDFromBytes(groupId.getBytes(StandardCharsets.UTF_8)).toString())
                .value(new CaseAccessGroup(CCD_ALL_CASES_ACCESS, groupId))
                .build())
            .toList();
    }

    /**
     * A party created with the case has no claim link until submit, so an organisation-bearing party
     * without one is the claimant it was created for. Matching only on the claim link would derive
     * nothing for the whole draft phase.
     */
    private static Optional<PartyRole> derivedRole(PartyEntity party) {
        if (party.getOrganisationId() == null) {
            return Optional.empty();
        }
        if (party.getClaimParties().isEmpty()) {
            return Optional.of(PartyRole.CLAIMANT).filter(DERIVED_ROLES::contains);
        }
        return party.getClaimParties().stream()
            .map(ClaimPartyEntity::getRole)
            .filter(DERIVED_ROLES::contains)
            .findFirst();
    }

    /**
     * Every organisation also carries the generic ORGANISATION_PROFILE, so that one is skipped. More
     * than one profile beyond it is ambiguous - picking either would silently decide the capacity.
     */
    private static String organisationProfileId(PartyEntity party) {
        List<String> organisationProfileIds = party.getOrganisationProfileIds();
        if (organisationProfileIds == null || organisationProfileIds.isEmpty()) {
            throw new IllegalArgumentException(
                "Organisation profile ids are required to derive case access groups for party " + party.getId());
        }

        List<String> candidates = organisationProfileIds.stream()
            .filter(profileId -> !ORGANISATION_PROFILE.equals(profileId))
            .distinct()
            .toList();

        if (candidates.isEmpty()) {
            throw new IllegalArgumentException(
                "No valid organisation profile id found for organisation " + party.getOrganisationId());
        }
        if (candidates.size() > 1) {
            throw new IllegalArgumentException(
                "Organisation " + party.getOrganisationId() + " carries more than one profile " + candidates
                    + "; cannot determine which access type applies");
        }

        return candidates.getFirst();
    }
}
