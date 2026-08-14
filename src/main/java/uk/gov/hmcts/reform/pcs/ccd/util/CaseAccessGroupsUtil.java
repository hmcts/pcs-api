package uk.gov.hmcts.reform.pcs.ccd.util;

import static java.util.Objects.requireNonNullElse;
import static java.util.function.Function.identity;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.GroupAccessType.forProfileAndRole;

import java.util.Map;
import java.util.stream.Collectors;
import uk.gov.hmcts.ccd.sdk.type.CaseAccessGroup;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.ClaimPartyLegalRepresentativeOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Builds the CaseAccessGroups the data store matches against users' role assignments. The id must be
 * byte-identical to the one PRM builds from the same AccessTypeRole template; a mismatch fails closed.
 */
public final class CaseAccessGroupsUtil {

    public static final String CCD_ALL_CASES_ACCESS = "CCD:all-cases-access";


    private CaseAccessGroupsUtil() {
    }

    public static List<ListValue<CaseAccessGroup>> deriveCaseAccessGroups(List<ListValue<Party>> claimants,
                                                                          List<ListValue<Party>> defendants,
                                                                          Set<PartyEntity> partyEntities) {
        List<CaseAccessGroup> caseAccessGroups = new ArrayList<>();
        Map<UUID, PartyEntity> partyEntitiesMap = partyEntities.stream()
            .collect(Collectors.toMap(PartyEntity::getId, identity()));

        requireNonNullElse(claimants, List.<ListValue<Party>>of())
            .stream()
            .map(ListValue::getId)
            .map(partyId -> partyEntitiesMap.get(UUID.fromString(partyId)))
            .filter(Objects::nonNull)
            .map(partyEntity -> forProfileAndRole(
                partyEntity.getOrganisationProfileId(), PartyRole.CLAIMANT, partyEntity.getOrganisationId())
            )
            .forEach(caseAccessGroupId ->
                         caseAccessGroups.add(new CaseAccessGroup(CCD_ALL_CASES_ACCESS, caseAccessGroupId)));

        requireNonNullElse(defendants, List.<ListValue<Party>>of()).stream()
            .map(ListValue::getId)
            .map(partyId -> partyEntitiesMap.get(UUID.fromString(partyId)))
            .map(partyEntity ->
                partyEntity.getClaimPartyLegalRepresentativeOrganisationList()
                    .stream()
                    .filter(legalRepOrg -> YesOrNo.YES == legalRepOrg.getActive())
                    .findFirst())
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(ClaimPartyLegalRepresentativeOrganisationEntity::getLegalRepresentativeOrganisation)
            .map(legalRepOrg -> forProfileAndRole(
                legalRepOrg.getOrganisationProfileId(), PartyRole.DEFENDANT, legalRepOrg.getOrganisationId())
            )
            .forEach(caseAccessGroupId ->
                         caseAccessGroups.add(new CaseAccessGroup(CCD_ALL_CASES_ACCESS, caseAccessGroupId)));




        return caseAccessGroups.stream()
            .map(CaseAccessGroup::getCaseAccessGroupId)
            .distinct()
            .sorted()
            .map(CaseAccessGroupsUtil::asStableListValue)
            .toList();
    }

    /**
     * Recomputed on every read over a HashSet of parties, so the id comes from the group rather than
     * being random - otherwise an unchanged case differs read to read.
     */
    private static ListValue<CaseAccessGroup> asStableListValue(String groupId) {
        return ListValue.<CaseAccessGroup>builder()
            .id(UUID.nameUUIDFromBytes(groupId.getBytes(StandardCharsets.UTF_8)).toString())
            .value(new CaseAccessGroup(CCD_ALL_CASES_ACCESS, groupId))
            .build();
    }
}
