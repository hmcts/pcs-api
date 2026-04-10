package uk.gov.hmcts.reform.pcs.ccd.service;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.Flags;
import uk.gov.hmcts.ccd.sdk.type.FlagDetail;
import uk.gov.hmcts.ccd.sdk.type.FlagVisibility;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagsEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagDetailsEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CaseFlagService {

    public void mergeCaseFlags(Flags incomingCaseFlags, PcsCaseEntity pcsCaseEntity) {

        FlagsEntity mergedFlagsEntity = new FlagsEntity();
        if (incomingCaseFlags.getDetails() != null && !incomingCaseFlags.getDetails().isEmpty()) {
            mergedFlagsEntity = mergeFlags(incomingCaseFlags);
        }

        if (pcsCaseEntity.getCaseFlags() != null) {
            pcsCaseEntity.getCaseFlags().add(mergedFlagsEntity);
        } else {
            pcsCaseEntity.setCaseFlags(List.of(mergedFlagsEntity));
        }

    }

    private FlagsEntity mergeFlags(Flags incomingCaseFlags) {
        FlagsEntity flagsEntity = new FlagsEntity();

        flagsEntity.setVisibility(FlagVisibility.INTERNAL.getValue());

        if (!incomingCaseFlags.getDetails().isEmpty()) {
            for (ListValue<FlagDetail> incomingFlagDetail : incomingCaseFlags.getDetails()) {
                FlagDetailsEntity flagDetailsEntity = FlagDetailsEntity.builder()
                    .flagCode(incomingFlagDetail.getValue().getFlagCode())
                    .name(incomingFlagDetail.getValue().getName())
                    .nameWelsh(incomingFlagDetail.getValue().getNameCy())
                    .flagComment(incomingFlagDetail.getValue().getFlagComment())
                    .flagCommentWelsh(incomingFlagDetail.getValue().getFlagCommentCy())
                    .availableExternally(convertYesOrNoToBoolean(
                        incomingFlagDetail.getValue().getAvailableExternally()))
                    .dateTimeCreated(LocalDateTime.now())
                    .defaultStatus(incomingFlagDetail.getValue().getStatus())
                    .hearingRelevant(convertYesOrNoToBoolean(incomingFlagDetail.getValue().getHearingRelevant()))
                    .subTypeKey(incomingFlagDetail.getValue().getSubTypeKey())
                    .subTypeValue(incomingFlagDetail.getValue().getSubTypeValue())
                    .subTypeValueWelsh(incomingFlagDetail.getValue().getSubTypeValueCy())
                    .flagUpdateComment(incomingFlagDetail.getValue().getFlagUpdateComment())
                    .build();
                flagsEntity.getFlagDetails().add(flagDetailsEntity);
            }
        }

        return flagsEntity;
    }

    private Boolean convertYesOrNoToBoolean(YesOrNo yesOrNo) {

        return yesOrNo == YesOrNo.YES;
    }
}

