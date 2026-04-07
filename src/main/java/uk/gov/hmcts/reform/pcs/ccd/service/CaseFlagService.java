package uk.gov.hmcts.reform.pcs.ccd.service;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.FlagDetail;
import uk.gov.hmcts.ccd.sdk.type.Flags;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagsEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagDetailsEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CaseFlagService {

    public void mergeCaseFlags(Flags incomingCaseFlags, PcsCaseEntity pcsCaseEntity) {
        Map<UUID, FlagsEntity> existingCaseFlags = pcsCaseEntity.getCaseFlags() != null
            ? Map.of(pcsCaseEntity.getCaseFlags().getId(), pcsCaseEntity.getCaseFlags())
            : Map.of();

        if (existingCaseFlags.isEmpty()) {
            return;
        }

        FlagsEntity mergedFlagsEntity = mergeFlags(incomingCaseFlags, existingCaseFlags, pcsCaseEntity);

        pcsCaseEntity.setCaseFlags(mergedFlagsEntity);
    }

    private FlagsEntity mergeFlags(Flags incomingCaseFlags, Map<UUID, FlagsEntity> existingCaseFlags,
                                   PcsCaseEntity pcsCaseEntity) {

        FlagsEntity flagsEntity = existingCaseFlags.get(incomingCaseFlags.getGroupId());

        if (flagsEntity == null) {
            flagsEntity = new FlagsEntity();
            flagsEntity.setPcsCase(pcsCaseEntity);
        }

        flagsEntity.setVisibility(incomingCaseFlags.getVisibility().getValue());

        if (!incomingCaseFlags.getDetails().isEmpty()) {
            for (ListValue<FlagDetail> incomingFlagDetail : incomingCaseFlags.getDetails()) {
                FlagDetailsEntity flagDetailsEntity = FlagDetailsEntity.builder()
                    .flagCode(incomingFlagDetail.getValue().getFlagCode())
                    .name(incomingFlagDetail.getValue().getName())
                    .nameWelsh(incomingFlagDetail.getValue().getNameCy())
                    .flagComment(incomingFlagDetail.getValue().getFlagComment())
                    .flagCommentWelsh(incomingFlagDetail.getValue().getFlagCommentCy())
                    .availableExternally(incomingFlagDetail.getValue().getAvailableExternally().toBoolean())
                    .dateTimeCreated(LocalDateTime.now())
                    .defaultStatus(incomingFlagDetail.getValue().getStatus())
                    .hearingRelevant(incomingFlagDetail.getValue().getHearingRelevant().toBoolean())
                    .subTypeKey(incomingFlagDetail.getValue().getSubTypeKey())
                    .subTypeValue(incomingFlagDetail.getValue().getSubTypeValue())
                    .subTypeValueWelsh(incomingFlagDetail.getValue().getSubTypeValueCy())
                    .flagUpdateComment(incomingFlagDetail.getValue().getFlagUpdateComment())
                    .build();
                flagsEntity.getCaseFlags().add(flagDetailsEntity);
            }
        }

        return flagsEntity;
    }

    public void mergePartyFlags(List<ListValue<Party>> incomingParties, PcsCaseEntity pcsCaseEntity) {
        Map<UUID, PartyEntity> existingParties =
            pcsCaseEntity.getParties().stream()
                .collect(Collectors.toMap(PartyEntity::getId,
                                          Function.identity()
                ));

        List<PartyEntity> mergedPartyEntities = new ArrayList<>();

        for (ListValue<Party> incomingPartyValue : incomingParties) {
            Party incomingParty = incomingPartyValue.getValue();

            PartyEntity partyEntity = existingParties.get(UUID.fromString(incomingPartyValue.getId()));
            if (partyEntity == null) {
                partyEntity = new PartyEntity();
                partyEntity.setPcsCase(pcsCaseEntity);
            }

            if (incomingParty.getAppellantFlags() != null) {
                mergePartyFlagGroup(incomingParty.getAppellantFlags(), partyEntity.getAppellantFlags(), partyEntity);
            }

            if (incomingParty.getRespondentFlags() != null) {
                mergePartyFlagGroup(incomingParty.getRespondentFlags(), partyEntity.getRespondentFlags(), partyEntity);
            }

            mergedPartyEntities.add(partyEntity);
        }

        //pcsCaseEntity.getParties().clear();
        pcsCaseEntity.getParties().addAll(mergedPartyEntities);
    }

    private void mergePartyFlagGroup(Flags incomingFlags, List<FlagsEntity> existingFlags, PartyEntity partyEntity) {
        FlagsEntity flagsEntity = existingFlags.stream()
            .findFirst()
            .orElseGet(() -> {
                FlagsEntity newEntity = new FlagsEntity();
                newEntity.setParty(partyEntity);
                existingFlags.add(newEntity);
                return newEntity;
            });

        flagsEntity.setVisibility(incomingFlags.getVisibility().getValue());

        if (incomingFlags.getDetails() != null) {
            flagsEntity.getCaseFlags().clear();

            for (ListValue<FlagDetail> incomingFlagDetail : incomingFlags.getDetails()) {
                FlagDetailsEntity flagDetailsEntity = FlagDetailsEntity.builder()
                    .flagCode(incomingFlagDetail.getValue().getFlagCode())
                    .flagComment(incomingFlagDetail.getValue().getFlagComment())
                    .build();

                flagDetailsEntity.setCaseFlag(flagsEntity);
                flagsEntity.getCaseFlags().add(flagDetailsEntity);
            }
        }
    }
}
