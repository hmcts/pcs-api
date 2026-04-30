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
import uk.gov.hmcts.reform.pcs.ccd.entity.RefDataFlagsEntity;
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

        Map<UUID, CaseFlagEntity> existingCaseFlagEntities =
            pcsCaseEntity.getCaseFlags().stream()
                .collect(Collectors.toMap(CaseFlagEntity::getId, Function.identity()));

        List<CaseFlagEntity> mergedFlagDetails = new ArrayList<>();

        List<String> existingFlagPathIds = getExistingPathIds(existingCaseFlagEntities);

        for (ListValue<FlagDetail> incomingFlagDetailListValue : incomingCaseFlags.getDetails()) {
            FlagDetail incomingFlagDetail = incomingFlagDetailListValue.getValue();

            RefDataFlagsEntity refDataFlagsEntity = refDataFlagsRepository.findByFlagCode((
                incomingFlagDetail.getFlagCode())).orElse(null);

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

            refDataFlagsRepository.save(refDataFlagsEntity);

            String flagId = incomingFlagDetailListValue.getId();
            CaseFlagEntity caseFlagEntity = existingCaseFlagEntities.remove(UUID.fromString(flagId));

            if (caseFlagEntity == null) {
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
        pcsCaseEntity.getCaseFlags().clear();
        pcsCaseEntity.getCaseFlags().addAll(mergedFlagDetails);
    }

    private void setFlagPath(FlagDetail incomingFlagDetail, List<String> existingFlagPathIds,
                                             CaseFlagEntity caseFlagEntity) {
        if (incomingFlagDetail.getPath() != null
            && !(new HashSet<>(existingFlagPathIds).containsAll(getIncomingFlagPathIds(incomingFlagDetail)))) {
            for (ListValue<String> path : incomingFlagDetail.getPath()) {
                FlagPathEntity flagPathEntity = FlagPathEntity.builder()
                    .caseFlagEntity(caseFlagEntity)
                    .path(path.getValue())
                    .build();
                caseFlagEntity.getPaths().add(flagPathEntity);
            }
        }
    }

    private Boolean getBooleanValue(YesOrNo yesOrNoValue) {
        return YesOrNoConverter.toBoolean(yesOrNoValue);
    }

    private List<String> getIncomingFlagPathIds(FlagDetail incomingFlagDetail) {

        return incomingFlagDetail.getPath().stream().map(ListValue::getId).toList();
    }

    private List<String> getExistingPathIds(Map<UUID, CaseFlagEntity> existingCaseFlagEntities) {
        List<String> list = new ArrayList<>();
        for (CaseFlagEntity flagDetails : existingCaseFlagEntities.values()) {
            for (FlagPathEntity flagPathEntity : flagDetails.getPaths()) {
                list.add(flagPathEntity.getId().toString());
            }
        }
        return list;
    }
}

