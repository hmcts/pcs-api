package uk.gov.hmcts.reform.pcs.ccd.view;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.Hearing;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.HearingEntity;

import java.util.List;

@Component
public class HearingView {

    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        setHearingFields(pcsCase, pcsCaseEntity.getHearings());
    }

    private void setHearingFields(PCSCase pcsCase, List<HearingEntity> hearingEntities) {
        List<ListValue<Hearing>> hearings = hearingEntities.stream().map(hearingEntity -> {
            Hearing hearing = Hearing.builder()
                .type(hearingEntity.getType())
                .otherHearingType(hearingEntity.getOtherHearingType())
                .noticeWording(hearingEntity.getNoticeWording())
                .date(hearingEntity.getHearingDate())
                .durationHours(hearingEntity.getDurationHours())
                .durationMinutes(hearingEntity.getDurationMinutes())
                .notes(hearingEntity.getNotes())
                .noticeIssued(hearingEntity.getNoticeIssued())
                .isWithoutNotice(hearingEntity.getIsWithoutNotice())
                .additionalInformation(hearingEntity.getAdditionalInformation())
                .build();

            ListValue<Hearing> listValue = new ListValue<>();
            listValue.setValue(hearing);

            return listValue;
        }).toList();

        pcsCase.setHearingList(hearings);
    }
}
