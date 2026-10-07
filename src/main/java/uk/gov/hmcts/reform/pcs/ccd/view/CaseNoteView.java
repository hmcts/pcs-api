package uk.gov.hmcts.reform.pcs.ccd.view;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseNote;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseNoteEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;

import java.util.List;

@Component
public class CaseNoteView {

    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        setCaseNoteFields(pcsCase, pcsCaseEntity.getCaseNotes());
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
}
