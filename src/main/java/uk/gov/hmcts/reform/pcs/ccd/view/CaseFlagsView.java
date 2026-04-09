package uk.gov.hmcts.reform.pcs.ccd.view;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.FlagDetail;
import uk.gov.hmcts.ccd.sdk.type.FlagVisibility;
import uk.gov.hmcts.ccd.sdk.type.Flags;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagsEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;

import java.util.List;

@Component
public class CaseFlagsView {

    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        if (pcsCaseEntity.getCaseFlags() != null) {
            mapBasicCaseFlagFields(pcsCase, pcsCaseEntity);
        }
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

    private List<ListValue<FlagDetail>> mapFlagDetails(List<FlagsEntity> flagsEntities) {
        if (flagsEntities.isEmpty()) {
            return List.of();
        }

        return flagsEntities.getFirst().getFlagDetails().stream()
            .map(flagDetailsEntity -> ListValue.<FlagDetail>builder()
                .id(flagDetailsEntity.getId().toString())
                .value(FlagDetail.builder()
                           .flagCode(flagDetailsEntity.getFlagCode())
                           .flagComment(flagDetailsEntity.getFlagComment())
                           .nameCy(flagDetailsEntity.getName())
                           .name(flagDetailsEntity.getNameWelsh())
                           .flagCommentCy(flagDetailsEntity.getFlagCommentWelsh())
                           .status(flagDetailsEntity.getDefaultStatus())
                           .subTypeKey(flagDetailsEntity.getSubTypeKey())
                           .build())
                .build())
            .toList();
    }
}
