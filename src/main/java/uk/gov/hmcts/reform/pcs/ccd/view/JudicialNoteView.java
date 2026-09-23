package uk.gov.hmcts.reform.pcs.ccd.view;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.JudicialNote;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.JudicialNoteEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.UserNameEntity;

import java.time.LocalDateTime;
import java.util.List;

import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;

@Component
public class JudicialNoteView {

    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        setJudicialNoteFields(pcsCase, pcsCaseEntity.getJudicialNotes());
    }

    private void setJudicialNoteFields(PCSCase pcsCase, List<JudicialNoteEntity> judicialNoteEntities) {
        List<ListValue<JudicialNote>> judicialNotes = judicialNoteEntities.stream().map(
            judicialNoteEntity -> {
                JudicialNote judicialNote = covertToJudicialNote(judicialNoteEntity);
                ListValue<JudicialNote> judicialNoteListValue = new ListValue<>();
                judicialNoteListValue.setValue(judicialNote);
                return judicialNoteListValue;
            }
        ).toList();

        pcsCase.setJudicialNotes(judicialNotes);
    }

    private JudicialNote covertToJudicialNote(JudicialNoteEntity entity) {
        LocalDateTime ukDateTime = LocalDateTime.ofInstant(
            entity.getCreatedOn(),
            UK_ZONE_ID
        );

        UserNameEntity userNameEntity = entity.getUser();

        return JudicialNote.builder()
            .note(entity.getNote())
            .createdBy(userNameEntity.getName())
            .createdOn(ukDateTime)
            .build();
    }

}
