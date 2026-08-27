package uk.gov.hmcts.reform.pcs.ccd.view;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ccd.sdk.type.FlagDetail;
import uk.gov.hmcts.ccd.sdk.type.FlagVisibility;
import uk.gov.hmcts.ccd.sdk.type.Flags;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.PartySupport;
import uk.gov.hmcts.reform.pcs.ccd.entity.BaseCaseFlag;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.util.YesOrNoConverter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Objects.requireNonNullElse;
import static uk.gov.hmcts.reform.pcs.ccd.util.FlagVisibilityConverter.toFlagVisibility;

@Component
@AllArgsConstructor
public class CaseFlagsView {

    private static final String DEFENDANT = "Defendant";
    private static final String CLAIMANT = "Claimant";
    public static final String PATHS_DELIMITER = "_";
    public static final String PATH_DELIMITER = ":";


    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {

        mapBasicCaseFlagFields(pcsCase, pcsCaseEntity);
        mapComplexPartyFlagFields(pcsCase, pcsCaseEntity);
    }

    private void mapBasicCaseFlagFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        List<BaseCaseFlag> baseCaseFlags = new ArrayList<>(pcsCaseEntity.getCaseFlags());

        Flags caseFlags = pcsCaseEntity.getCaseFlags().isEmpty()
            ? Flags.builder().build()
            : Flags.builder()
                .visibility(FlagVisibility.INTERNAL)
                .details(mapFlagDetails(baseCaseFlags))
                .build();
        pcsCase.setCaseFlags(caseFlags);
    }

    private List<ListValue<FlagDetail>> mapFlagDetails(List<? extends BaseCaseFlag> flagsEntities) {

        return flagsEntities.stream()
            .map(caseFlagEntity -> ListValue.<FlagDetail>builder()
                .id(caseFlagEntity.getId().toString())
                .value(FlagDetail.builder()
                   .flagCode(caseFlagEntity.getFlagRefData().getFlagCode())
                   .name(caseFlagEntity.getFlagRefData().getFlagName())
                   .nameCy(caseFlagEntity.getFlagRefData().getFlagNameWelsh())
                   .flagComment(caseFlagEntity.getFlagComment())
                   .flagCommentCy(caseFlagEntity.getFlagCommentWelsh())
                   .status(caseFlagEntity.getDefaultStatus())
                   .subTypeKey(caseFlagEntity.getSubTypeKey())
                   .subTypeValue(caseFlagEntity.getSubTypeValue())
                   .subTypeValueCy(caseFlagEntity.getSubTypeValueWelsh())
                   .flagUpdateComment(caseFlagEntity.getFlagUpdateComment())
                   .dateTimeCreated(caseFlagEntity.getDateTimeCreated())
                   .dateTimeModified(caseFlagEntity.getDateTimeModified())
                   .otherDescription(caseFlagEntity.getOtherDescription())
                   .otherDescriptionCy(caseFlagEntity.getOtherDescriptionWelsh())
                   .hearingRelevant(YesOrNoConverter.toYesOrNo(
                       caseFlagEntity.getFlagRefData().getHearingRelevant()))
                   .availableExternally(YesOrNoConverter.toYesOrNo(
                       caseFlagEntity.getFlagRefData().getAvailableExternally()))
                   .path(getPaths(caseFlagEntity.getPaths()))
                   .build())
                .build())
            .toList();
    }

    // The limit of 2 keeps a value containing the path delimiter intact
    private List<ListValue<String>> getPaths(String entityPaths) {

        return Arrays.stream(entityPaths.split(PATHS_DELIMITER))
                .map(pathPairs -> pathPairs.split(PATH_DELIMITER, 2))
                .filter(paths -> paths.length > 1)
                .map(paths -> ListValue.<String>builder()
                    .id(paths[0])
                    .value(paths[1])
                    .build())
                .toList();
    }

    private void mapComplexPartyFlagFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        Map<UUID, PartyRole> supportedPartyRoles = getSupportedPartyRoles(pcsCaseEntity);

        pcsCase.setPartySupport(mapPartySupport(pcsCaseEntity, supportedPartyRoles));

        List<ListValue<Party>> partyListValues = pcsCase.getParties();
        if (CollectionUtils.isEmpty(partyListValues)) {
            return;
        }

        Map<UUID, PartyEntity> partyEntitiesById = new LinkedHashMap<>();
        pcsCaseEntity.getParties()
            .forEach(partyEntity -> partyEntitiesById.putIfAbsent(partyEntity.getId(), partyEntity));

        for (ListValue<Party> partyListValue : partyListValues) {
            Party party = partyListValue.getValue();
            PartyEntity partyEntity = partyEntitiesById.get(toPartyId(party));
            if (partyEntity == null) {
                continue;
            }

            partyListValue.setId(partyEntity.getId().toString());

            PartyRole partyRole = supportedPartyRoles.get(partyEntity.getId());
            if (partyRole == null) {
                continue;
            }

            String roleOnCase = roleOnCase(partyRole);
            party.setDefendantFlags(mapPartyFlags(partyEntity, roleOnCase, FlagVisibility.INTERNAL));
            party.setPartyFlagsExternal(mapPartyFlags(partyEntity, roleOnCase, FlagVisibility.EXTERNAL));
        }
    }

    private UUID toPartyId(Party party) {
        if (party == null || party.getId() == null) {
            return null;
        }

        try {
            return UUID.fromString(party.getId());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private List<ListValue<PartySupport>> mapPartySupport(PcsCaseEntity pcsCaseEntity,
                                                          Map<UUID, PartyRole> supportedPartyRoles) {
        List<ListValue<PartySupport>> partySupport = new ArrayList<>();

        for (PartyEntity partyEntity : pcsCaseEntity.getParties()) {
            PartyRole partyRole = supportedPartyRoles.get(partyEntity.getId());
            if (partyRole == null) {
                continue;
            }

            partySupport.add(ListValue.<PartySupport>builder()
                .id(partyEntity.getId().toString())
                .value(PartySupport.builder()
                    .supportFlags(mapPartyFlags(partyEntity, roleOnCase(partyRole), FlagVisibility.EXTERNAL))
                    .build())
                .build());
        }

        return partySupport;
    }

    private Map<UUID, PartyRole> getSupportedPartyRoles(PcsCaseEntity pcsCaseEntity) {
        List<ClaimEntity> claims = pcsCaseEntity.getClaims();
        if (CollectionUtils.isEmpty(claims)) {
            return Map.of();
        }

        Map<UUID, PartyRole> partyRoles = new LinkedHashMap<>();
        for (ClaimPartyEntity claimParty : claims.getFirst().getClaimParties()) {
            if (claimParty.getRole() != PartyRole.CLAIMANT && claimParty.getRole() != PartyRole.DEFENDANT) {
                continue;
            }
            UUID partyId = getPartyId(claimParty);
            if (partyId != null) {
                partyRoles.putIfAbsent(partyId, claimParty.getRole());
            }
        }

        return partyRoles;
    }

    private String roleOnCase(PartyRole partyRole) {
        return partyRole == PartyRole.CLAIMANT ? CLAIMANT : DEFENDANT;
    }

    private String partyName(PartyEntity partyEntity) {
        if (partyEntity.getOrgName() != null) {
            return partyEntity.getOrgName();
        }

        return requireNonNullElse(partyEntity.getFirstName(), "Person")
            + " " + requireNonNullElse(partyEntity.getLastName(), "Unknown");
    }

    private UUID getPartyId(ClaimPartyEntity claimParty) {
        if (claimParty.getId() != null && claimParty.getId().getPartyId() != null) {
            return claimParty.getId().getPartyId();
        }

        PartyEntity party = claimParty.getParty();
        return party == null ? null : party.getId();
    }

    private Flags mapPartyFlags(PartyEntity partyEntity, String roleOnCase, FlagVisibility visibility) {
        List<? extends BaseCaseFlag> partyFlags = CollectionUtils.isEmpty(partyEntity.getDefendantFlags())
            ? List.of()
            : partyEntity.getDefendantFlags().stream()
                .filter(partyFlag -> visibility == toFlagVisibility(partyFlag.getVisibility()))
                .toList();

        return Flags.builder()
            .partyName(partyName(partyEntity))
            .roleOnCase(roleOnCase)
            .groupId(partyEntity.getId())
            .visibility(visibility)
            .details(mapFlagDetails(partyFlags))
            .build();
    }
}
