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
 * Derives the CaseAccessGroups the data store's group matcher compares against the
 * caseAccessGroupId on users' role assignments. Must produce exactly the id PRM builds
 * from the caseAccessGroupIdTemplate declared in CaseType's AccessTypeRole config -
 * both sides are manufactured from that one declaration; a mismatch fails closed.
 */
public final class CaseAccessGroupsUtil {

    public static final String CCD_ALL_CASES_ACCESS = "CCD:all-cases-access";
    public static final String ORGANISATION_PROFILE = "ORGANISATION_PROFILE";
    static final String ORG_IDENTIFIER_TEMPLATE = "$ORGID$";

    /**
     * Only the claimant's organisation is stamped onto a case. The defendant access type is declared
     * but not implemented; widening this set is the defendant/notice-of-change ticket's job, and the
     * lookup in GroupAccessType already returns the right template once it is.
     */
    private static final Set<PartyRole> DERIVED_ROLES = Set.of(PartyRole.CLAIMANT);

    private CaseAccessGroupsUtil() {
    }

    /**
     * Derives one group per organisation-owned party, the access type chosen by the party's
     * organisation profile and its role. Parties without an organisation (citizens) contribute
     * nothing.
     */
    public static List<ListValue<CaseAccessGroup>> deriveCaseAccessGroups(Set<PartyEntity> parties) {
        List<CaseAccessGroup> caseAccessGroups = new ArrayList<>();

        parties.forEach(party -> derivedRole(party)
            .flatMap(role -> GroupAccessType.forProfileAndRole(organisationProfileId(party), role))
            .ifPresent(accessType -> caseAccessGroups.add(new CaseAccessGroup(
                CCD_ALL_CASES_ACCESS,
                accessType.getCaseAccessGroupIdTemplate()
                    .replace(ORG_IDENTIFIER_TEMPLATE, party.getOrganisationId())))));

        // Stable output: parties come from a HashSet and this is recomputed on every read, so a random
        // id would make an unchanged case differ read to read. The group id is the identity.
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
     * The role this party's organisation holds the case in, empty when it contributes no group.
     *
     * <p>A party created with the case has no claim link yet - that is the whole draft phase, which is
     * the point of firm-visible drafts - so an organisation-bearing party without one is the claimant
     * the case was created for.</p>
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
     * The profile the access type is keyed on. Every organisation also carries the generic
     * ORGANISATION_PROFILE, which is skipped; more than one profile beyond it is ambiguous, and
     * picking either would silently decide the capacity a case is stamped with.
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
