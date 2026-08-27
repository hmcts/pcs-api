package uk.gov.hmcts.reform.pcs.ccd.util;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNullElse;
import java.util.Objects;
import static java.util.function.Function.identity;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.GroupAccessType.caseAccessGroupIdFor;
import static uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole.CLAIMANT;
import static uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole.DEFENDANT;

import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.ccd.sdk.type.CaseAccessGroup;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.ClaimPartyOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;

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
@Slf4j
public final class CaseAccessGroupsUtil {

    public static final String CCD_ALL_CASES_ACCESS = "CCD:all-cases-access";


    private CaseAccessGroupsUtil() {
    }

    public static List<ListValue<CaseAccessGroup>> deriveCaseAccessGroups(Set<PartyEntity> parties,
                                                                          List<ListValue<Party>> defendants) {
        List<CaseAccessGroup> caseAccessGroups = new ArrayList<>();

        parties.stream()
            .filter(CaseAccessGroupsUtil::isClaimant)
            .findFirst()
            .flatMap(party ->
                         caseAccessGroupIdFor(party.getOrganisationProfileId(), CLAIMANT, party.getOrganisationId()))
            .ifPresent(caseAccessGroupId -> {
                log.info("Found claimant case access group: {}", caseAccessGroupId);
                caseAccessGroups.add(new CaseAccessGroup(CCD_ALL_CASES_ACCESS, caseAccessGroupId));
            });

        Map<UUID, PartyEntity> partyEntitiesMap = parties.stream()
            .collect(Collectors.toMap(PartyEntity::getId, identity()));

        requireNonNullElse(defendants, List.<ListValue<Party>>of()).stream()
            .map(ListValue::getId)
            .peek(partyId -> log.info("Processing defendant party id: {}", partyId))
            .map(partyId -> partyEntitiesMap.get(UUID.fromString(partyId)))
            .filter(Objects::nonNull)
            .peek(partyEntity -> log.info("Found defendant party entity: {}", partyEntity))
            .map(partyEntity ->
                     partyEntity.getClaimPartyOrganisationList()
                         .stream()
                         .filter(legalRepOrg -> YesOrNo.YES == legalRepOrg.getActive())
                         .findFirst())
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(ClaimPartyOrganisationEntity::getOrganisation)
            .map(legalRepOrg ->
                     caseAccessGroupIdFor(legalRepOrg.getOrganisationProfileId(),
                                          DEFENDANT, legalRepOrg.getOrganisationId()))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .peek(caseAccessGroupId ->
                      log.info("Found defendant case access group: {}", caseAccessGroupId))
            .forEach(caseAccessGroupId ->
                         caseAccessGroups.add(new CaseAccessGroup(CCD_ALL_CASES_ACCESS, caseAccessGroupId)));;

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

    private static boolean isClaimant(PartyEntity party) {
        return nonNull(party.getOrganisationId()) && party.isClaimCreator();
    }
}
