package uk.gov.hmcts.reform.pcs.ccd.view;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.FlagDetail;
import uk.gov.hmcts.ccd.sdk.type.FlagVisibility;
import uk.gov.hmcts.ccd.sdk.type.Flags;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseFlagEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.RefDataFlagsRepository;
import uk.gov.hmcts.reform.pcs.ccd.util.YesOrNoConverter;

import java.util.List;

@Component
@AllArgsConstructor
public class CaseFlagsView {

    private final RefDataFlagsRepository refDataFlagsRepository;

    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {

        mapBasicCaseFlagFields(pcsCase, pcsCaseEntity);
    }

    private void mapBasicCaseFlagFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        Flags caseFlags = pcsCaseEntity.getCaseFlags().isEmpty()
            ? Flags.builder().build()
            : Flags.builder()
            .visibility(FlagVisibility.INTERNAL)
            .details(mapFlagDetails(pcsCaseEntity.getCaseFlags()))
            .build();
        pcsCase.setCaseFlags(caseFlags);
    }

    private List<ListValue<FlagDetail>> mapFlagDetails(List<CaseFlagEntity> flagsEntities) {

        return flagsEntities.stream()
            .map(caseFlagEntity -> ListValue.<FlagDetail>builder()
                .id(caseFlagEntity.getId().toString())
                .value(FlagDetail.builder()
                   .flagCode(caseFlagEntity.getFlagCode())
                   .name(refDataFlagsRepository.findByFlagCode(
                       caseFlagEntity.getFlagCode()).orElseThrow().getFlagName())
                   .nameCy(refDataFlagsRepository.findByFlagCode(
                       caseFlagEntity.getFlagCode()).orElseThrow().getFlagNameWelsh())
                   .flagComment(caseFlagEntity.getFlagComment())
                   .flagCommentCy(caseFlagEntity.getFlagCommentWelsh())
                   .status(caseFlagEntity.getDefaultStatus())
                   .subTypeKey(caseFlagEntity.getSubTypeKey())
                   .subTypeValue(caseFlagEntity.getSubTypeValue())
                   .subTypeValueCy(caseFlagEntity.getSubTypeValueWelsh())
                   .flagUpdateComment(caseFlagEntity.getFlagUpdateComment() != null
                                      ? caseFlagEntity.getFlagUpdateComment()
                                      : caseFlagEntity.getFlagUpdateCommentWelsh())
                   .dateTimeCreated(caseFlagEntity.getDateTimeCreated())
                   .dateTimeModified(caseFlagEntity.getDateTimeModified())
                   .otherDescription(caseFlagEntity.getOtherDescription())
                   .otherDescriptionCy(caseFlagEntity.getOtherDescriptionWelsh())
                   .hearingRelevant(YesOrNoConverter.toYesOrNo(refDataFlagsRepository.findByFlagCode(
                       caseFlagEntity.getFlagCode()).orElseThrow().getHearingRelevant()))
                   .availableExternally(YesOrNoConverter.toYesOrNo(refDataFlagsRepository.findByFlagCode(
                       caseFlagEntity.getFlagCode()).orElseThrow().getAvailableExternally()))
                   .path(caseFlagEntity.getPaths().stream()
                             .map(pathEntity -> ListValue.<String>builder()
                                .id(pathEntity.getId().toString())
                                .value(pathEntity.getPath())
                                .build())
                             .toList())
                   .build())
                .build())
            .toList();
    }
}
