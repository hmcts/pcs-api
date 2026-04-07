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

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CaseFlagService {

    /*
     * Merge incoming case flags with existing case flags
     */
    public void mergeCaseFlags(Flags incomingCaseFlags, PcsCaseEntity pcsCaseEntity) {
        // Get existing case flags map
        Map<UUID, FlagsEntity> existingCaseFlags = Map.of(pcsCaseEntity.getCaseFlags().getId(),
                                                          pcsCaseEntity.getCaseFlags());

        FlagsEntity mergedFlagsEntity = mergeFlags(incomingCaseFlags, existingCaseFlags, pcsCaseEntity);

        // Merge case flags
        //mergeFlags(incomingCaseFlags, existingCaseFlags, mergedCaseFlagEntities, pcsCaseEntity);

        // Update case flags
        pcsCaseEntity.setCaseFlags(mergedFlagsEntity);
        //pcsCaseEntity.getCaseFlags().clear();
        // pcsCaseEntity.getCaseFlags().addAll(mergedCaseFlagEntities);


    }

    private FlagsEntity mergeFlags(Flags incomingCaseFlags, Map<UUID, FlagsEntity> existingCaseFlags,
                                   PcsCaseEntity pcsCaseEntity) {

        // Get existing case flag entity
        FlagsEntity flagsEntity = existingCaseFlags.get(incomingCaseFlags.getGroupId());

        //pcsCaseEntity.getCaseFlags().removeIf(caseFlagEntity1 ->
        // caseFlagEntity1.getId().equals(incomingCaseFlags.getGroupId()));
        // Create new case flag entity if not found
        if (flagsEntity == null) {
            flagsEntity = new FlagsEntity();
            flagsEntity.setPcsCase(pcsCaseEntity);
        }

        // Merge case flag visibility
        flagsEntity.setVisibility(incomingCaseFlags.getVisibility().getValue());

        // Merge case flag details
        if (!incomingCaseFlags.getDetails().isEmpty()) {
            for (ListValue<FlagDetail> incomingFlagDetail : incomingCaseFlags.getDetails()) {
                FlagDetailsEntity flagDetailsEntity = FlagDetailsEntity.builder()
                    .flagCode(incomingFlagDetail.getValue().getFlagCode())
                    .flagComment(incomingFlagDetail.getValue().getFlagComment())
                    //.availableExternally(incomingFlagDetail.getValue().getAvailableExternally())
                    .build();
                flagsEntity.getCaseFlags().add(flagDetailsEntity);
            }
        }

        // Add merged case flag entity to list
        //mergedCaseFlagEntities.add(caseFlagEntity);
        return flagsEntity;
    }

    public void mergePartyFlags(List<ListValue<Party>> incomingParties, PcsCaseEntity pcsCaseEntity) {
        // Get existing parties map
        Map<UUID, PartyEntity> existingParties =
            pcsCaseEntity.getParties().stream()
                .collect(Collectors.toMap(PartyEntity::getId,
                                          Function.identity()
                ));

        List<PartyEntity> mergedPartyEntities = new ArrayList<>();

        // Merge parties
        for (ListValue<Party> incomingPartyValue : incomingParties) {
            Party incomingParty = incomingPartyValue.getValue();
            //incomingParty.get

            // Get existing party entity
            PartyEntity partyEntity = existingParties.get(UUID.fromString(incomingPartyValue.getId()));
            if (partyEntity == null) {
                partyEntity = new PartyEntity();
                partyEntity.setPcsCase(pcsCaseEntity);
            }

            // Merge party flags
            if (incomingParty.getAppellantFlags() != null) {
                mergePartyFlagGroup(incomingParty.getAppellantFlags(), partyEntity.getAppellantFlags(), partyEntity);
            }

            if (incomingParty.getRespondentFlags() != null) {
                mergePartyFlagGroup(incomingParty.getRespondentFlags(), partyEntity.getRespondentFlags(), partyEntity);
            }

            // Add merged party entity to list
            mergedPartyEntities.add(partyEntity);
        }

        pcsCaseEntity.getParties().clear();
        pcsCaseEntity.getParties().addAll(mergedPartyEntities);
    }

    private void mergePartyFlagGroup(Flags incomingFlags, List<FlagsEntity> existingFlags, PartyEntity partyEntity) {
        // Get existing party flags map
        FlagsEntity flagsEntity = existingFlags.stream()
            .findFirst()
            .orElseGet(() -> {
                FlagsEntity newEntity = new FlagsEntity();
                newEntity.setParty(partyEntity);
                existingFlags.add(newEntity);
                return newEntity;
            });

        flagsEntity.setVisibility(incomingFlags.getVisibility().getValue());

        // Merge party flags
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
