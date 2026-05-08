package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.Flags;
import uk.gov.hmcts.ccd.sdk.type.FlagDetail;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.ccd.sdk.type.FlagVisibility;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseFlagEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.CasePartyFlagEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.BaseCaseFlag;
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagRefDataEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.FlagRefDataRepository;
import uk.gov.hmcts.reform.pcs.ccd.util.YesOrNoConverter;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.view.CaseFlagsView;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;


@Service
@AllArgsConstructor
public class CaseFlagService {

    private FlagRefDataRepository flagRefDataRepository;

    public List<CaseFlagEntity> mergeCaseFlags(Flags incomingCaseFlags, PcsCaseEntity pcsCaseEntity, String flow) {

        if (incomingCaseFlags != null && incomingCaseFlags.getDetails() != null) {

            Map<UUID, CaseFlagEntity> existingCaseFlagEntitiesMap =
                pcsCaseEntity.getCaseFlags().stream()
                    .collect(Collectors.toMap(BaseCaseFlag::getId, Function.identity()));

            return mergeFlagDetails(
                        existingCaseFlagEntitiesMap, incomingCaseFlags, pcsCaseEntity,null, flow,
                        CaseFlagEntity::new);
        } else {
            return new ArrayList<>();
        }
    }

    public void mergePartyFlags(List<ListValue<Party>> incomingParties, PcsCaseEntity pcsCaseEntity,
                                             String flow) {
        if (incomingParties != null) {
            Map<UUID, PartyEntity> existingPartiesMap = pcsCaseEntity.getParties().stream()
                .collect(Collectors.toMap(
                    PartyEntity::getId,
                    Function.identity()
                ));

            for (ListValue<Party> incomingPartyValue : incomingParties) {
                Party incomingParty = incomingPartyValue.getValue();

                PartyEntity partyEntity = existingPartiesMap.get(UUID.fromString(incomingPartyValue.getId()));

                if (partyEntity != null && incomingParty.getRespondentFlags() != null
                    && !incomingParty.getRespondentFlags().getDetails().isEmpty()) {
                    mergePartyFlagGroup(incomingParty.getRespondentFlags(), partyEntity.getRespondentFlags(),
                        partyEntity, pcsCaseEntity, flow
                    );
                }
            }
        }
    }

    private void mergePartyFlagGroup(Flags incomingPartyFlags, List<CasePartyFlagEntity> existingFlags,
                                     PartyEntity partyEntity, PcsCaseEntity pcsCaseEntity, String flow) {

        Map<UUID, CasePartyFlagEntity> existingPartyFlagEntityMap =
            existingFlags.stream()
                .collect(Collectors.toMap(BaseCaseFlag::getId, Function.identity()));

        List<CasePartyFlagEntity> mergedCasePartyFlags = mergeFlagDetails(
            existingPartyFlagEntityMap, incomingPartyFlags, pcsCaseEntity,
            partyEntity, flow, CasePartyFlagEntity::new);

        partyEntity.getRespondentFlags().clear();
        partyEntity.getRespondentFlags().addAll(mergedCasePartyFlags);
    }

    private <T extends BaseCaseFlag> List<T>  mergeFlagDetails(
                        Map<UUID, T> existingCaseFlagEntitiesMap, Flags incomingCaseFlags, PcsCaseEntity pcsCaseEntity,
                        PartyEntity partyEntity, String flow, Supplier<T> flagEntitySupplier) {

        List<T> mergedFlagDetails = new ArrayList<>();
        Set<FlagRefDataEntity> flagRefDataEntities = new HashSet<>();

        for (ListValue<FlagDetail> incomingFlagDetailListValue : incomingCaseFlags.getDetails()) {
            FlagDetail incomingFlagDetail = incomingFlagDetailListValue.getValue();

            FlagRefDataEntity flagRefDataEntity = getRefDataEntity(incomingCaseFlags, incomingFlagDetail);
            flagRefDataEntities.add(flagRefDataEntity);

            T flagEntity = existingCaseFlagEntitiesMap.remove(UUID.fromString(incomingFlagDetailListValue.getId()));


            if (flagEntity == null) {
                flagEntity = flagEntitySupplier.get();
            }

            if (flagEntity instanceof CasePartyFlagEntity casePartyFlagEntity) {
                casePartyFlagEntity.setParty(partyEntity);
            } else if (flagEntity instanceof CaseFlagEntity caseFlagEntity) {
                caseFlagEntity.setPcsCase(pcsCaseEntity);
            }

            if ("CREATE".equals(flow)) {
                flagEntity.setFlagComment(incomingFlagDetail.getFlagComment());
                flagEntity.setFlagCommentWelsh(incomingFlagDetail.getFlagCommentCy());
            } else if ("UPDATE".equals(flow)) {
                flagEntity.setFlagUpdateComment(incomingFlagDetail.getFlagComment());
                flagEntity.setFlagUpdateCommentWelsh(incomingFlagDetail.getFlagCommentCy());
            }

            flagEntity.setDateTimeCreated(incomingFlagDetail.getDateTimeCreated());
            flagEntity.setDateTimeModified(incomingFlagDetail.getDateTimeModified());

            flagEntity.setDefaultStatus(incomingFlagDetail.getStatus());
            flagEntity.setSubTypeKey(incomingFlagDetail.getSubTypeKey());
            flagEntity.setSubTypeValue(incomingFlagDetail.getSubTypeValue());
            flagEntity.setSubTypeValueWelsh(incomingFlagDetail.getSubTypeValueCy());
            flagEntity.setFlagRefData(flagRefDataEntity);

            flagEntity.setOtherDescription(incomingFlagDetail.getOtherDescription());
            flagEntity.setOtherDescriptionWelsh(incomingFlagDetail.getOtherDescriptionCy());

            setFlagPath(incomingFlagDetail, flagEntity);

            mergedFlagDetails.add(flagEntity);
        }
        flagRefDataRepository.saveAll(flagRefDataEntities);

        return mergedFlagDetails;
    }

    private FlagRefDataEntity getRefDataEntity(Flags incomingCaseFlags, FlagDetail incomingFlagDetail) {

        FlagRefDataEntity refDataFlagsEntity = flagRefDataRepository.findByFlagCode(
            incomingFlagDetail.getFlagCode()).orElse(null);

        if (refDataFlagsEntity == null) {
            refDataFlagsEntity = new FlagRefDataEntity();
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

    private void setFlagPath(FlagDetail incomingFlagDetail, BaseCaseFlag flagEntity) {

        if (incomingFlagDetail.getPath() != null) {
            List<String> pathLists = incomingFlagDetail.getPath().stream()
                .map(pathList -> pathList.getId() + CaseFlagsView.PATH_DELIMITER + pathList.getValue())
                .toList();
            StringBuilder paths = new StringBuilder();
            pathLists.forEach(s -> paths.append(s).append(CaseFlagsView.PATHS_DELIMITER));
            paths.deleteCharAt(paths.lastIndexOf(CaseFlagsView.PATHS_DELIMITER));
            flagEntity.setPaths(paths.toString());
        }
    }
}

