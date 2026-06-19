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
import uk.gov.hmcts.reform.pcs.ccd.entity.BaseCaseFlag;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.util.YesOrNoConverter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static java.util.Objects.requireNonNullElse;
import static java.util.stream.Collectors.toSet;

@Component
@AllArgsConstructor
public class CaseFlagsView {

    private static final String DEFENDANT = "Defendant";
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

    private List<ListValue<FlagDetail>> mapFlagDetails(List<BaseCaseFlag> flagsEntities) {

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

    private List<ListValue<String>> getPaths(String entityPaths) {

        return Arrays.stream(entityPaths.split(PATHS_DELIMITER))
                .map(pathPairs -> pathPairs.split(PATH_DELIMITER))
                .map(paths -> ListValue.<String>builder()
                    .id(paths[0])
                    .value(paths[1])
                    .build())
                .toList();
    }

    private void mapComplexPartyFlagFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        List<ListValue<Party>> partyListValues = pcsCase.getParties();
        if (CollectionUtils.isEmpty(partyListValues)) {
            return;
        }

        // pcsCase.parties is wrapped from pcsCaseEntity.getParties() in iteration order, but the
        // entity id is dropped during the entity->domain mapping. Re-attach it onto each ListValue
        // so the entity can be matched back, then apply the defendant flags.
        // same party set.
        List<PartyEntity> partyEntities = new ArrayList<>(pcsCaseEntity.getParties());
        Set<UUID> defendantPartyIds = getDefendantPartyIds(pcsCaseEntity);
        for (int i = 0; i < partyListValues.size() && i < partyEntities.size(); i++) {
            PartyEntity partyEntity = partyEntities.get(i);
            ListValue<Party> partyListValue = partyListValues.get(i);
            partyListValue.setId(partyEntity.getId().toString());
            if (defendantPartyIds.contains(partyEntity.getId())) {
                partyListValue.getValue().setDefendantFlags(mapDefendantFlags(partyEntity));
            }
        }
    }

    private Set<UUID> getDefendantPartyIds(PcsCaseEntity pcsCaseEntity) {
        List<ClaimEntity> claims = pcsCaseEntity.getClaims();
        if (CollectionUtils.isEmpty(claims)) {
            return Set.of();
        }

        return claims.getFirst().getClaimParties().stream()
            .filter(claimParty -> claimParty.getRole() == PartyRole.DEFENDANT)
            .map(this::getPartyId)
            .filter(Objects::nonNull)
            .collect(toSet());
    }

    private UUID getPartyId(ClaimPartyEntity claimParty) {
        if (claimParty.getId() != null && claimParty.getId().getPartyId() != null) {
            return claimParty.getId().getPartyId();
        }

        PartyEntity party = claimParty.getParty();
        return party == null ? null : party.getId();
    }

    private Flags mapDefendantFlags(PartyEntity partyEntity) {
        if (CollectionUtils.isEmpty(partyEntity.getDefendantFlags())) {
            return Flags.builder()
                .partyName((requireNonNullElse(partyEntity.getFirstName(), "Person")
                    + " " + requireNonNullElse(partyEntity.getLastName(), "Unknown")))
                .roleOnCase(DEFENDANT)
                .details(new ArrayList<>())
                .build();
        }

        List<BaseCaseFlag> defendantFlags = new ArrayList<>(partyEntity.getDefendantFlags());

        return Flags.builder()
            .partyName((requireNonNullElse(partyEntity.getFirstName(), "Person")
                + " " + requireNonNullElse(partyEntity.getLastName(), "Unknown")))
            .roleOnCase(
                DEFENDANT)
            .details(mapFlagDetails(defendantFlags))
            .visibility(FlagVisibility.INTERNAL)
            .build();
    }
}
