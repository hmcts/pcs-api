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
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.BaseCaseFlag;
import uk.gov.hmcts.reform.pcs.ccd.util.YesOrNoConverter;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNullElse;

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
        if (CollectionUtils.isEmpty(pcsCase.getParties())) {
            return;
        }

        Map<String, PartyEntity> defendantEntitiesById = pcsCaseEntity.getParties().stream()
            .filter(this::isDefendant)
            .collect(Collectors.toMap(partyEntity -> partyEntity.getId().toString(), Function.identity()));

        pcsCase.getParties().forEach(partyListValue -> {
            Party party = partyListValue.getValue();
            PartyEntity defendantEntity = defendantEntitiesById.get(party.getId());
            if (defendantEntity != null) {
                party.setDefendantFlags(mapDefendantFlags(defendantEntity));
            }
        });
    }

    private boolean isDefendant(PartyEntity partyEntity) {
        return !CollectionUtils.isEmpty(partyEntity.getClaimParties())
            && partyEntity.getClaimParties().stream()
                .anyMatch(claimParty -> claimParty.getRole() == PartyRole.DEFENDANT);
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
