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
import uk.gov.hmcts.reform.pcs.ccd.entity.CasePartyFlagEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.BaseCaseFlag;
import uk.gov.hmcts.reform.pcs.ccd.entity.RefDataFlagEntity;
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

        if (incomingCaseFlags != null && incomingCaseFlags.getDetails() != null) {

            Map<UUID, BaseCaseFlag> existingCaseFlagEntitiesMap =
                pcsCaseEntity.getCaseFlags().stream()
                    .collect(Collectors.toMap(BaseCaseFlag::getId, Function.identity()));

            Set<String> existingFlagPathIds = getExistingPathIds(existingCaseFlagEntitiesMap);

            List<BaseCaseFlag> mergedBaseFlagDetails = mergeFlagDetails(
                existingCaseFlagEntitiesMap, incomingCaseFlags, pcsCaseEntity, existingFlagPathIds,
                null, flow
            );

            List<CaseFlagEntity> mergedCaseFlagDetails = new ArrayList<>();
            for (BaseCaseFlag baseCaseFlag : mergedBaseFlagDetails) {
                mergedCaseFlagDetails.add((CaseFlagEntity) baseCaseFlag);
            }

            pcsCaseEntity.getCaseFlags().clear();
            pcsCaseEntity.getCaseFlags().addAll(mergedCaseFlagDetails);
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
                mergePartyFlagGroup(incomingParty.getRespondentFlags(), partyEntity.getRespondentFlags(),
                                    partyEntity, pcsCaseEntity, flow);
            }
            mergedPartyEntities.add(partyEntity);
        }

        return mergedPartyEntities;
    }

    private void mergePartyFlagGroup(Flags incomingPartyFlags, List<CasePartyFlagEntity> existingFlags,
                                     PartyEntity partyEntity, PcsCaseEntity pcsCaseEntity, String flow) {

        if (incomingPartyFlags.getDetails() != null && !incomingPartyFlags.getDetails().isEmpty()) {

            Map<UUID, BaseCaseFlag> existingCaseFlagEntityMap =
                existingFlags.stream()
                    .collect(Collectors.toMap(BaseCaseFlag::getId, Function.identity()));

            Set<String> existingFlagPathIds = getExistingPathIds(existingCaseFlagEntityMap);
            List<BaseCaseFlag> mergedBaseFlagDetails = mergeFlagDetails(
                existingCaseFlagEntityMap, incomingPartyFlags, pcsCaseEntity, existingFlagPathIds, partyEntity, flow);

            List<CasePartyFlagEntity> mergedPartyFlagDetails = castToCasePartyFlagEntity(mergedBaseFlagDetails,
                                                                                         partyEntity);
            partyEntity.getRespondentFlags().clear();
            partyEntity.getRespondentFlags().addAll(mergedPartyFlagDetails);
        }
    }

    private List<CasePartyFlagEntity> castToCasePartyFlagEntity(List<BaseCaseFlag> mergedBaseFlagDetails,
                                                                PartyEntity partyEntity) {
        List<CasePartyFlagEntity> mergedPartyFlagDetails = new ArrayList<>();
        for (BaseCaseFlag baseCaseFlag: mergedBaseFlagDetails) {
            CasePartyFlagEntity casePartyFlagEntity = new CasePartyFlagEntity();
            casePartyFlagEntity = (CasePartyFlagEntity) baseCaseFlag;
            casePartyFlagEntity.setParty(partyEntity);
            casePartyFlagEntity.setCasePartyFlagPaths(((CasePartyFlagEntity) baseCaseFlag).getCasePartyFlagPaths());
            casePartyFlagEntity.setRefDataFlag(baseCaseFlag.getRefDataFlag());
            mergedPartyFlagDetails.add(casePartyFlagEntity);
        }

        return mergedPartyFlagDetails;
    }

    private List<BaseCaseFlag> mergeFlagDetails(Map<UUID, BaseCaseFlag> existingCaseFlagEntitiesMap,
                                                  Flags incomingCaseFlags, PcsCaseEntity pcsCaseEntity,
                                                  Set<String> existingFlagPathIds,
                                                  PartyEntity partyEntity, String flow) {

        List<BaseCaseFlag> mergedFlagDetails = new ArrayList<>();
        List<RefDataFlagEntity> refDataFlagEntities = new ArrayList<>();

        for (ListValue<FlagDetail> incomingFlagDetailListValue : incomingCaseFlags.getDetails()) {
            Map<UUID, BaseCaseFlag> existingFlagEntities =
                pcsCaseEntity.getCaseFlags().stream()
                    .collect(Collectors.toMap(BaseCaseFlag::getId, Function.identity()));

            FlagDetail incomingFlagDetail = incomingFlagDetailListValue.getValue();

            RefDataFlagEntity refDataFlagEntity = getRefDataEntity(incomingCaseFlags, incomingFlagDetail);
            refDataFlagEntities.add(refDataFlagEntity);

            String flagId = incomingFlagDetailListValue.getId();
            BaseCaseFlag flagEntity = existingCaseFlagEntitiesMap.remove(UUID.fromString(flagId));
            CasePartyFlagEntity ca = new CasePartyFlagEntity();

            if (flagEntity == null && partyEntity != null) {
                flagEntity = new CasePartyFlagEntity();
                flagEntity.setPcsCase(pcsCaseEntity);
            } else if (flagEntity == null) {
                flagEntity = new CaseFlagEntity();
                flagEntity.setPcsCase(pcsCaseEntity);
            }

            flagEntity.setFlagCode(incomingFlagDetail.getFlagCode());

            if (flow.equals("CREATE")) {
                flagEntity.setFlagComment(incomingFlagDetail.getFlagComment());
                flagEntity.setFlagCommentWelsh(incomingFlagDetail.getFlagCommentCy());
            }

            flagEntity.setDateTimeCreated(incomingFlagDetail.getDateTimeCreated());
            flagEntity.setDateTimeModified(incomingFlagDetail.getDateTimeModified());

            flagEntity.setDefaultStatus(incomingFlagDetail.getStatus());
            flagEntity.setSubTypeKey(incomingFlagDetail.getSubTypeKey());
            flagEntity.setSubTypeValue(incomingFlagDetail.getSubTypeValue());
            flagEntity.setSubTypeValueWelsh(incomingFlagDetail.getSubTypeValueCy());
            flagEntity.setRefDataFlag(refDataFlagEntity);

            if (flow.equals("UPDATE")) {
                flagEntity.setFlagUpdateComment(incomingFlagDetail.getFlagComment());
                flagEntity.setFlagUpdateCommentWelsh(incomingFlagDetail.getFlagCommentCy());
            }

            flagEntity.setOtherDescription(incomingFlagDetail.getOtherDescription());
            flagEntity.setOtherDescriptionWelsh(incomingFlagDetail.getOtherDescriptionCy());

            setFlagPath(incomingFlagDetail, existingFlagPathIds, flagEntity);

            mergedFlagDetails.add(flagEntity);
        }

        if (!refDataFlagEntities.isEmpty()) {
            refDataFlagsRepository.saveAll(refDataFlagEntities);
        }

        return mergedFlagDetails;
    }

    private RefDataFlagEntity getRefDataEntity(Flags incomingCaseFlags, FlagDetail incomingFlagDetail) {

        RefDataFlagEntity refDataFlagsEntity = refDataFlagsRepository.findByFlagCode(
            incomingFlagDetail.getFlagCode()).orElse(null);

        if (refDataFlagsEntity == null) {
            refDataFlagsEntity = new RefDataFlagEntity();
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
                                             BaseCaseFlag flagEntity) {
        if (incomingFlagDetail.getPath() != null
            && !(existingFlagPathIds.containsAll(getIncomingFlagPathIds(incomingFlagDetail)))) {

            for (ListValue<String> path : incomingFlagDetail.getPath()) {
                FlagPathEntity flagPathEntity = FlagPathEntity.builder()
                    .path(path.getValue())
                    .build();

                CaseFlagEntity caseFlagEntity = null;
                CasePartyFlagEntity casePartyFlagEntity = null;
                if (flagEntity instanceof  CaseFlagEntity caseFlagEntityInstance) {
                    caseFlagEntity =  caseFlagEntityInstance;
                    flagPathEntity.setCaseFlagEntity(caseFlagEntity);
                    caseFlagEntity.getCaseFlagPaths().add(flagPathEntity);
                } else if (flagEntity instanceof CasePartyFlagEntity casePartyFlagEntityInstance) {
                    casePartyFlagEntity = casePartyFlagEntityInstance;
                    flagPathEntity.setCasePartyFlagEntity(casePartyFlagEntity);
                    casePartyFlagEntity.getCasePartyFlagPaths().add(flagPathEntity);
                }
            }
        }
    }

    private List<String> getIncomingFlagPathIds(FlagDetail incomingFlagDetail) {

        return incomingFlagDetail.getPath().stream().map(ListValue::getId).toList();
    }

    private Set<String> getExistingPathIds(Map<UUID, BaseCaseFlag> existingFlagEntities) {
        Set<String> pathIds = new HashSet<>();
        for (BaseCaseFlag flagDetails : existingFlagEntities.values()) {
            List<FlagPathEntity> flagPathEntities = new ArrayList<>();
            if (flagDetails instanceof CaseFlagEntity) {
                flagPathEntities =  flagDetails.getCaseFlagPaths();
            } else if (flagDetails instanceof CasePartyFlagEntity) {
                flagPathEntities =  flagDetails.getCasePartyFlagPaths();
            }
            for (FlagPathEntity flagPathEntity : flagPathEntities) {
                pathIds.add(flagPathEntity.getId().toString());
            }
        }
        return pathIds;
    }
}

