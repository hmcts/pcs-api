package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.Flags;
import uk.gov.hmcts.ccd.sdk.type.FlagDetail;
import uk.gov.hmcts.ccd.sdk.type.FlagVisibility;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseFlagEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagRefDataEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.BaseCaseFlag;
import uk.gov.hmcts.reform.pcs.ccd.repository.FlagRefDataRepository;
import uk.gov.hmcts.reform.pcs.ccd.util.YesOrNoConverter;
import uk.gov.hmcts.reform.pcs.ccd.view.CaseFlagsView;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
@AllArgsConstructor
public class CaseFlagService {

    private FlagRefDataRepository flagRefDataRepository;

    public List<BaseCaseFlag> mergeCaseFlags(Flags incomingCaseFlags, PcsCaseEntity pcsCaseEntity, String flow) {
        Map<UUID, BaseCaseFlag> existingFlagEntities =
            pcsCaseEntity.getCaseFlags().stream()
                .collect(Collectors.toMap(BaseCaseFlag::getId, Function.identity()));

        List<BaseCaseFlag> mergedFlagDetails = new ArrayList<>();
        Set<FlagRefDataEntity> flagRefDataEntities = new HashSet<>();

        for (ListValue<FlagDetail> incomingFlagDetailListValue : incomingCaseFlags.getDetails()) {
            FlagDetail incomingFlagDetail = incomingFlagDetailListValue.getValue();

            FlagRefDataEntity flagRefDataEntity = getRefDataEntity(incomingCaseFlags, incomingFlagDetail);
            flagRefDataEntities.add(flagRefDataEntity);

            BaseCaseFlag flagEntity = existingFlagEntities.remove(UUID.fromString(incomingFlagDetailListValue.getId()));

            if (flagEntity == null) {
                flagEntity = new CaseFlagEntity();
                flagEntity.setPcsCase(pcsCaseEntity);
            }

            if ("CREATE".equals(flow)) {
                flagEntity.setFlagComment(incomingFlagDetail.getFlagComment());
                flagEntity.setFlagCommentWelsh(incomingFlagDetail.getFlagCommentCy());
            } else {
                if ("Inactive".equals(flagEntity.getDefaultStatus())) {
                    flagEntity.setFlagUpdateComment(incomingFlagDetail.getFlagUpdateComment());
                } else {
                    flagEntity.setFlagUpdateComment(incomingFlagDetail.getFlagComment());
                }
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
            incomingFlagDetail.getFlagCode()).orElse(new FlagRefDataEntity());

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
            StringBuilder paths = new StringBuilder();
            incomingFlagDetail.getPath().stream()
                .map(pathListValue -> pathListValue.getId() + CaseFlagsView.PATH_DELIMITER + pathListValue.getValue())
                .forEach(path -> paths.append(path).append(CaseFlagsView.PATHS_DELIMITER));

            paths.deleteCharAt(paths.lastIndexOf(CaseFlagsView.PATHS_DELIMITER));
            flagEntity.setPaths(paths.toString());
        }
    }
}

