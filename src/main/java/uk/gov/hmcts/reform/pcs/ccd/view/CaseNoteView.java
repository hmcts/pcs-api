package uk.gov.hmcts.reform.pcs.ccd.view;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseReviewDate;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseNote;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseNoteEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseReviewDateEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;

import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

@Component
public class CaseNoteView {

    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        setCaseNoteFields(pcsCase, pcsCaseEntity.getCaseNotes());
        setReviewDateFields(pcsCase, pcsCaseEntity.getReviewDates());
    }

    private void setCaseNoteFields(PCSCase pcsCase, List<CaseNoteEntity> caseNoteEntities) {
        List<ListValue<CaseNote>> caseNotes = caseNoteEntities.stream().map(caseNoteEntity -> {
            CaseNote caseNote = CaseNote.builder()
                .note(caseNoteEntity.getNote())
                .createdOn(CaseNoteEntity.fromEntity(caseNoteEntity).getCreatedOn())
                .createdBy(caseNoteEntity.getCreatedBy())
                .build();

            ListValue<CaseNote> listValue = new ListValue<>();
            listValue.setValue(caseNote);

            return listValue;
        }).toList();

        pcsCase.setCaseNotes(caseNotes);
    }

    private void setReviewDateFields(PCSCase pcsCase, List<CaseReviewDateEntity> reviewDateEntities) {
        List<CaseReviewDateEntity> orderedReviewDateEntities = reviewDateEntities.stream()
            .sorted(Comparator.comparing(
                CaseReviewDateEntity::getCreatedDate,
                Comparator.nullsLast(Comparator.reverseOrder())
            ))
            .toList();

        List<ListValue<CaseReviewDate>> reviewDates = IntStream.range(0, orderedReviewDateEntities.size())
            .mapToObj(index -> toListValue(orderedReviewDateEntities.get(index), index))
            .toList();

        pcsCase.setCaseReviewDates(reviewDates);
    }

    private ListValue<CaseReviewDate> toListValue(CaseReviewDateEntity reviewDateEntity, int index) {
        CaseReviewDate reviewDate = CaseReviewDate.builder()
            .createdBy(reviewDateEntity.getCreatedBy())
            .createdDate(reviewDateEntity.getCreatedDate())
            .date(reviewDateEntity.getDate())
            .reason(reviewDateEntity.getReason())
            .description(reviewDateEntity.getDescription())
            .build();

        return new ListValue<>("Review date " + (index + 1), reviewDate);
    }
}
