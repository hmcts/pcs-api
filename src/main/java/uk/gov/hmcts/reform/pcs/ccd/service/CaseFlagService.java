package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.Flags;
import uk.gov.hmcts.ccd.sdk.type.FlagDetail;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.ccd.sdk.type.FlagVisibility;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseFlagEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagPathEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.RefDataFlagsEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.RefDataFlagsRepository;
import uk.gov.hmcts.reform.pcs.ccd.util.YesOrNoConverter;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class CaseFlagService {

    private RefDataFlagsRepository refDataFlagsRepository;

    public void mergeCaseFlags(Flags incomingCaseFlags, PcsCaseEntity pcsCaseEntity,
                               List<ListValue<Party>> parties, String flow) {

        Map<UUID, CaseFlagEntity> existingCaseFlagEntitiesMap =
            pcsCaseEntity.getCaseFlags().stream()
                .collect(Collectors.toMap(CaseFlagEntity::getId, Function.identity()));

        Set<String> existingFlagPathIds = getExistingPathIds(existingCaseFlagEntitiesMap);

        if (incomingCaseFlags != null && incomingCaseFlags.getDetails() != null) {
            List<CaseFlagEntity> mergedFlagDetails = mergeFlagDetails(
                existingCaseFlagEntitiesMap, incomingCaseFlags, pcsCaseEntity, existingFlagPathIds,
                null, flow
            );

            pcsCaseEntity.getCaseFlags().clear();
            pcsCaseEntity.getCaseFlags().addAll(mergedFlagDetails);
        }

        if (parties != null) {
            List<PartyEntity> mergedPartyEntities = mergePartyFlags(parties, pcsCaseEntity, flow);

            pcsCaseEntity.getParties().clear();
            pcsCaseEntity.getParties().addAll(mergedPartyEntities);
        }
    }

    public List<PartyEntity> mergePartyFlags(List<ListValue<Party>> incomingParties, PcsCaseEntity pcsCaseEntity,
                                             String flow) {
        Map<UUID, PartyEntity> existingPartiesMap = pcsCaseEntity.getParties().stream()
            .collect(Collectors.toMap(
                PartyEntity::getId,
                Function.identity()
            ));
        List<PartyEntity> mergedPartyEntities = new ArrayList<>();

        for (ListValue<Party> incomingPartyValue : incomingParties) {
            Party incomingParty = incomingPartyValue.getValue();

            PartyEntity partyEntity = existingPartiesMap.get(UUID.fromString(incomingPartyValue.getId()));

            if (partyEntity != null && incomingParty.getRespondentFlags() != null
                && !incomingParty.getRespondentFlags().getDetails().isEmpty()) {
                mergePartyFlagGroup(
                    incomingParty.getRespondentFlags(), partyEntity.getRespondentFlags(), partyEntity,
                    pcsCaseEntity, flow
                );
            }
            mergedPartyEntities.add(partyEntity);
        }

        return mergedPartyEntities;
    }

    private void mergePartyFlagGroup(Flags incomingPartyFlags, List<CaseFlagEntity> existingFlags,
                                     PartyEntity partyEntity, PcsCaseEntity pcsCaseEntity, String flow) {

        if (incomingPartyFlags.getDetails() != null && !incomingPartyFlags.getDetails().isEmpty()) {

            Map<UUID, CaseFlagEntity> existingCaseFlagEntityMap =
                existingFlags.stream()
                    .collect(Collectors.toMap(CaseFlagEntity::getId, Function.identity()));

            Set<String> existingFlagPathIds = getExistingPathIds(existingCaseFlagEntityMap);
            List<CaseFlagEntity> mergedPartyFlagDetails = mergeFlagDetails(
                existingCaseFlagEntityMap, incomingPartyFlags, pcsCaseEntity, existingFlagPathIds, partyEntity, flow);

            partyEntity.getRespondentFlags().clear();
            partyEntity.getRespondentFlags().addAll(mergedPartyFlagDetails);
        }
    }

    private List<CaseFlagEntity> mergeFlagDetails(Map<UUID, CaseFlagEntity> existingCaseFlagEntitiesMap,
                                                  Flags incomingCaseFlags, PcsCaseEntity pcsCaseEntity,
                                                  Set<String> existingFlagPathIds,
                                                  PartyEntity partyEntity, String flow) {

        List<CaseFlagEntity> mergedFlagDetails = new ArrayList<>();
        List<RefDataFlagsEntity> refDataFlagsEntities = new ArrayList<>();

        for (ListValue<FlagDetail> incomingFlagDetailListValue : incomingCaseFlags.getDetails()) {
            FlagDetail incomingFlagDetail = incomingFlagDetailListValue.getValue();

            RefDataFlagsEntity refDataFlagsEntity = getRefDataEntity(incomingCaseFlags, incomingFlagDetail);
            refDataFlagsEntities.add(refDataFlagsEntity);

            String flagId = incomingFlagDetailListValue.getId();
            CaseFlagEntity caseFlagEntity = existingCaseFlagEntitiesMap.remove(UUID.fromString(flagId));

            if (caseFlagEntity == null && partyEntity != null) {
                caseFlagEntity = new CaseFlagEntity();
                caseFlagEntity.setParty(partyEntity);
            } else if (caseFlagEntity == null) {
                caseFlagEntity = new CaseFlagEntity();
                caseFlagEntity.setPcsCase(pcsCaseEntity);
            }

            caseFlagEntity.setFlagCode(incomingFlagDetail.getFlagCode());

            if (flow.equals("CREATE")) {
                caseFlagEntity.setFlagComment(incomingFlagDetail.getFlagComment());
                caseFlagEntity.setFlagCommentWelsh(incomingFlagDetail.getFlagCommentCy());
            }

            caseFlagEntity.setDateTimeCreated(incomingFlagDetail.getDateTimeCreated());
            caseFlagEntity.setDateTimeModified(incomingFlagDetail.getDateTimeModified());

            caseFlagEntity.setDefaultStatus(incomingFlagDetail.getStatus());
            caseFlagEntity.setSubTypeKey(incomingFlagDetail.getSubTypeKey());
            caseFlagEntity.setSubTypeValue(incomingFlagDetail.getSubTypeValue());
            caseFlagEntity.setSubTypeValueWelsh(incomingFlagDetail.getSubTypeValueCy());

            if (flow.equals("UPDATE")) {
                caseFlagEntity.setFlagUpdateComment(incomingFlagDetail.getFlagComment());
                caseFlagEntity.setFlagUpdateCommentWelsh(incomingFlagDetail.getFlagCommentCy());
            }

            caseFlagEntity.setOtherDescription(incomingFlagDetail.getOtherDescription());
            caseFlagEntity.setOtherDescriptionWelsh(incomingFlagDetail.getOtherDescriptionCy());

            setFlagPath(incomingFlagDetail, existingFlagPathIds, caseFlagEntity);

            mergedFlagDetails.add(caseFlagEntity);
        }

        if (!refDataFlagsEntities.isEmpty()) {
            refDataFlagsRepository.saveAll(refDataFlagsEntities);
        }

        return mergedFlagDetails;
    }

    private RefDataFlagsEntity getRefDataEntity(Flags incomingCaseFlags, FlagDetail incomingFlagDetail) {

        RefDataFlagsEntity refDataFlagsEntity = refDataFlagsRepository.findByFlagCode(
            incomingFlagDetail.getFlagCode()).orElse(null);

        if (refDataFlagsEntity == null) {
            refDataFlagsEntity = new RefDataFlagsEntity();
        }
        refDataFlagsEntity.setFlagCode(incomingFlagDetail.getFlagCode());
        refDataFlagsEntity.setFlagName(incomingFlagDetail.getName());
        refDataFlagsEntity.setFlagNameWelsh(incomingFlagDetail.getNameCy());
        refDataFlagsEntity.setVisibility(incomingCaseFlags.getVisibility() != null
                                             ? incomingCaseFlags.getVisibility().getValue()
                                             : FlagVisibility.INTERNAL.getValue());
        refDataFlagsEntity.setHearingRelevant(YesOrNoConverter.toBoolean(incomingFlagDetail.getHearingRelevant()));
        refDataFlagsEntity.setAvailableExternally(YesOrNoConverter.toBoolean(
            incomingFlagDetail.getAvailableExternally()));

        return refDataFlagsEntity;
    }


    private void setFlagPath(FlagDetail incomingFlagDetail, Set<String> existingFlagPathIds,
                                             CaseFlagEntity caseFlagEntity) {
        if (incomingFlagDetail.getPath() != null
            && !(existingFlagPathIds.containsAll(getIncomingFlagPathIds(incomingFlagDetail)))) {
            for (ListValue<String> path : incomingFlagDetail.getPath()) {
                FlagPathEntity flagPathEntity = FlagPathEntity.builder()
                    .caseFlagEntity(caseFlagEntity)
                    .path(path.getValue())
                    .build();
                caseFlagEntity.getPaths().add(flagPathEntity);
            }
        }
    }

    private List<String> getIncomingFlagPathIds(FlagDetail incomingFlagDetail) {

        return incomingFlagDetail.getPath().stream().map(ListValue::getId).toList();
    }

    private Set<String> getExistingPathIds(Map<UUID, CaseFlagEntity> existingCaseFlagEntities) {
        Set<String> pathIds = new HashSet<>();
        for (CaseFlagEntity flagDetails : existingCaseFlagEntities.values()) {
            for (FlagPathEntity flagPathEntity : flagDetails.getPaths()) {
                pathIds.add(flagPathEntity.getId().toString());
            }
        }
        return pathIds;
    }
}

