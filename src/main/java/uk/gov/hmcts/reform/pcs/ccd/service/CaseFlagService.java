package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.Flags;
import uk.gov.hmcts.ccd.sdk.type.FlagDetail;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagDetailsEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagPathEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.util.YesOrNoConverter;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class CaseFlagService {
    public void mergeCaseFlags(Flags incomingCaseFlags, PcsCaseEntity pcsCaseEntity, List<ListValue<Party>> parties) {
        Map<UUID, FlagDetailsEntity> existingFlagDetailsMap =
            pcsCaseEntity.getCaseFlags().stream()
                .collect(Collectors.toMap(FlagDetailsEntity::getId, Function.identity()));

        List<String> existingFlagPathIds = getExistingPathIds(existingFlagDetailsMap);

        List<FlagDetailsEntity> mergedFlagDetails = mergeFlagDetails(
            existingFlagDetailsMap, incomingCaseFlags, pcsCaseEntity, existingFlagPathIds,
            null);

        pcsCaseEntity.getCaseFlags().clear();
        pcsCaseEntity.getCaseFlags().addAll(mergedFlagDetails);

        if (parties != null) {
            List<PartyEntity> mergedPartyEntities = mergePartyFlags(parties, pcsCaseEntity);

            pcsCaseEntity.getParties().clear();
            pcsCaseEntity.getParties().addAll(mergedPartyEntities);
        }
    }

    public List<PartyEntity> mergePartyFlags(List<ListValue<Party>> incomingParties, PcsCaseEntity pcsCaseEntity) {
        Map<UUID, PartyEntity> existingPartiesMap = pcsCaseEntity.getParties().stream()
                .collect(Collectors.toMap(PartyEntity::getId,
                                          Function.identity()
                ));
        List<PartyEntity> mergedPartyEntities = new ArrayList<>();

        for (ListValue<Party> incomingPartyValue : incomingParties) {
            Party incomingParty = incomingPartyValue.getValue();

            PartyEntity partyEntity = existingPartiesMap.get(UUID.fromString(incomingPartyValue.getId()));

            if (incomingParty.getRespondentFlags() != null
                && !incomingParty.getRespondentFlags().getDetails().isEmpty()) {
                mergePartyFlagGroup(incomingParty.getRespondentFlags(), partyEntity.getRespondentFlags(), partyEntity,
                                    pcsCaseEntity);
            }
            mergedPartyEntities.add(partyEntity);
        }

        return mergedPartyEntities;
    }

    private void mergePartyFlagGroup(Flags incomingPartyFlags, List<FlagDetailsEntity> existingFlags,
                                     PartyEntity partyEntity, PcsCaseEntity pcsCaseEntity) {
        Map<UUID, FlagDetailsEntity> existingFlagDetailsMap =
            existingFlags.stream()
                .collect(Collectors.toMap(FlagDetailsEntity::getId, Function.identity()));

        List<String> existingFlagPathIds = getExistingPathIds(existingFlagDetailsMap);

        if (incomingPartyFlags.getDetails() != null && !incomingPartyFlags.getDetails().isEmpty()) {

            List<FlagDetailsEntity> mergedPartyFlagDetails = mergeFlagDetails(
                existingFlagDetailsMap, incomingPartyFlags, pcsCaseEntity, existingFlagPathIds, partyEntity);

            partyEntity.getRespondentFlags().clear();
            partyEntity.getRespondentFlags().addAll(mergedPartyFlagDetails);
        }
    }

    private List<FlagDetailsEntity> mergeFlagDetails(Map<UUID, FlagDetailsEntity> existingFlagDetailsMap,
                                                     Flags incomingFlags, PcsCaseEntity pcsCaseEntity,
                                                     List<String> existingFlagPathIds, PartyEntity partyEntity) {

        List<FlagDetailsEntity> mergedFlagDetails = new ArrayList<>();

        for (ListValue<FlagDetail> incomingFlagDetailListValue : incomingFlags.getDetails()) {
            FlagDetail incomingFlagDetail = incomingFlagDetailListValue.getValue();
            String flagId = incomingFlagDetailListValue.getId();

            FlagDetailsEntity flagDetailsEntity = existingFlagDetailsMap.remove(UUID.fromString(flagId));

            if (flagDetailsEntity == null && partyEntity != null) {
                flagDetailsEntity = FlagDetailsEntity.builder()
                    .party(partyEntity)
                    .build();
            } else if (flagDetailsEntity == null) {
                flagDetailsEntity = FlagDetailsEntity.builder()
                    .pcsCase(pcsCaseEntity)
                    .build();
            }
            flagDetailsEntity.setFlagCode(incomingFlagDetail.getFlagCode());

            flagDetailsEntity.setName(incomingFlagDetail.getName());
            flagDetailsEntity.setNameWelsh(incomingFlagDetail.getNameCy());

            flagDetailsEntity.setFlagComment(incomingFlagDetail.getFlagComment());
            flagDetailsEntity.setFlagCommentWelsh(incomingFlagDetail.getFlagCommentCy());

            flagDetailsEntity.setDateTimeCreated(incomingFlagDetail.getDateTimeCreated());
            flagDetailsEntity.setDateTimeModified(incomingFlagDetail.getDateTimeModified());

            flagDetailsEntity.setDefaultStatus(incomingFlagDetail.getStatus());
            flagDetailsEntity.setSubTypeKey(incomingFlagDetail.getSubTypeKey());
            flagDetailsEntity.setSubTypeValue(incomingFlagDetail.getSubTypeValue());
            flagDetailsEntity.setSubTypeValueWelsh(incomingFlagDetail.getSubTypeValueCy());

            flagDetailsEntity.setFlagUpdateComment(incomingFlagDetail.getFlagUpdateComment());

            flagDetailsEntity.setOtherDescription(incomingFlagDetail.getOtherDescription());
            flagDetailsEntity.setOtherDescriptionWelsh(incomingFlagDetail.getOtherDescriptionCy());

            flagDetailsEntity.setHearingRelevant(getBooleanValue(incomingFlagDetail.getHearingRelevant()));
            flagDetailsEntity.setAvailableExternally(getBooleanValue(incomingFlagDetail.getAvailableExternally()));

            if (incomingFlagDetail.getPath() != null
                && !(new HashSet<>(existingFlagPathIds).containsAll(getIncomingFlagPathIds(incomingFlagDetail)))) {
                for (ListValue<String> path : incomingFlagDetail.getPath()) {
                    FlagPathEntity flagPathEntity = FlagPathEntity.builder()
                        .flagDetails(flagDetailsEntity)
                        .path(path.getValue())
                        .build();
                    flagDetailsEntity.getPaths().add(flagPathEntity);
                }
            }
            mergedFlagDetails.add(flagDetailsEntity);

        }

        return mergedFlagDetails;
    }

    private Boolean getBooleanValue(YesOrNo yesOrNoValue) {
        return YesOrNoConverter.toBoolean(yesOrNoValue);
    }

    private List<String> getIncomingFlagPathIds(FlagDetail incomingFlagDetail) {

        return incomingFlagDetail.getPath().stream().map(ListValue::getId).toList();
    }

    private List<String> getExistingPathIds(Map<UUID, FlagDetailsEntity> existingFlagDetails) {
        List<String> list = new ArrayList<>();
        for (FlagDetailsEntity flagDetails : existingFlagDetails.values()) {
            for (FlagPathEntity flagPathEntity : flagDetails.getPaths()) {
                list.add(flagPathEntity.getId().toString());
            }
        }
        return list;
    }
}

