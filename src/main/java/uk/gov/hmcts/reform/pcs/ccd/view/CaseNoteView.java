package uk.gov.hmcts.reform.pcs.ccd.view;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseNote;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseNoteEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseReviewDateEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.renderer.tabs.CaseReviewDatesRenderer;

import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CaseNoteView {

    private final CaseReviewDatesRenderer caseReviewDatesRenderer;

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

        pcsCase.setCaseReviewDatesMarkdown(caseReviewDatesRenderer.render(orderedReviewDateEntities));
    }

}
