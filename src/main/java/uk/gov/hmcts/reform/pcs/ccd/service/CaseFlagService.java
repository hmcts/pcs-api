package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.Flags;
import uk.gov.hmcts.ccd.sdk.type.FlagDetail;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagDetailsEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagPathEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
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

    public void mergeCaseFlags(Flags incomingCaseFlags, PcsCaseEntity pcsCaseEntity) {

        Map<UUID, FlagDetailsEntity> existingFlagDetails =
            pcsCaseEntity.getCaseFlags().stream()
                .collect(Collectors.toMap(FlagDetailsEntity::getId, Function.identity()));

        List<FlagDetailsEntity> mergedFlagDetails = new ArrayList<>();

        List<String> existingFlagPathIds = getExistingPathIds(existingFlagDetails);

        for (ListValue<FlagDetail> incomingFlagDetailListValue : incomingCaseFlags.getDetails()) {
            FlagDetail incomingFlagDetail = incomingFlagDetailListValue.getValue();
            String flagId = incomingFlagDetailListValue.getId();

            FlagDetailsEntity flagDetailsEntity = existingFlagDetails.remove(UUID.fromString(flagId));


            if (flagDetailsEntity == null) {
                flagDetailsEntity = new FlagDetailsEntity();
                flagDetailsEntity.setPcsCase(pcsCaseEntity);
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

            setFlagPath(incomingFlagDetail, existingFlagPathIds, flagDetailsEntity);

            mergedFlagDetails.add(flagDetailsEntity);
        }
        pcsCaseEntity.getCaseFlags().clear();
        pcsCaseEntity.getCaseFlags().addAll(mergedFlagDetails);
    }

    private void setFlagPath(FlagDetail incomingFlagDetail, List<String> existingFlagPathIds,
                                             FlagDetailsEntity flagDetailsEntity) {
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

