package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.Flags;
import uk.gov.hmcts.ccd.sdk.type.FlagDetail;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.ccd.sdk.type.FlagVisibility;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseFlagEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagPathEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.RefDataFlagEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.BaseCaseFlag;
import uk.gov.hmcts.reform.pcs.ccd.repository.RefDataFlagsRepository;
import uk.gov.hmcts.reform.pcs.ccd.util.YesOrNoConverter;

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

    private RefDataFlagsRepository refDataFlagsRepository;

    public void mergeCaseFlags(Flags incomingCaseFlags, PcsCaseEntity pcsCaseEntity, String flow) {

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
            BaseCaseFlag flagEntity = existingFlagEntities.remove(UUID.fromString(flagId));

            if (flagEntity == null) {
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

            List<String> existingFlagPathIds = getExistingPathIds(existingFlagEntities);
            setFlagPath(incomingFlagDetail, existingFlagPathIds, flagEntity);

            mergedFlagDetails.add(flagEntity);
        }
        refDataFlagsRepository.saveAll(refDataFlagEntities);
        pcsCaseEntity.getCaseFlags().clear();
        pcsCaseEntity.getCaseFlags().addAll(mergedFlagDetails);
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

    private void setFlagPath(FlagDetail incomingFlagDetail, List<String> existingFlagPathIds,
                                             BaseCaseFlag flagEntity) {
        if (incomingFlagDetail.getPath() != null
            && !(new HashSet<>(existingFlagPathIds).containsAll(getIncomingFlagPathIds(incomingFlagDetail)))) {
            for (ListValue<String> path : incomingFlagDetail.getPath()) {
                FlagPathEntity flagPathEntity = FlagPathEntity.builder()
                    .caseFlagEntity(flagEntity)
                    .path(path.getValue())
                    .build();
                flagEntity.getPaths().add(flagPathEntity);
            }
        }
    }

    private Boolean getBooleanValue(YesOrNo yesOrNoValue) {
        return YesOrNoConverter.toBoolean(yesOrNoValue);
    }

    private List<String> getIncomingFlagPathIds(FlagDetail incomingFlagDetail) {

        return incomingFlagDetail.getPath().stream().map(ListValue::getId).toList();
    }

    private List<String> getExistingPathIds(Map<UUID, BaseCaseFlag> existingFlagEntities) {
        List<String> list = new ArrayList<>();
        for (BaseCaseFlag flagDetails : existingFlagEntities.values()) {
            for (FlagPathEntity flagPathEntity : flagDetails.getPaths()) {
                list.add(flagPathEntity.getId().toString());
            }
        }
        return list;
    }
}

