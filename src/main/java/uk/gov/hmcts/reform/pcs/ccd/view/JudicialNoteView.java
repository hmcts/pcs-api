package uk.gov.hmcts.reform.pcs.ccd.view;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.JudicialNote;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.JudicialNoteEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.UserNameEntity;
import uk.gov.hmcts.reform.pcs.ccd.renderer.tabs.JudicialNoteRenderer;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;

@AllArgsConstructor
@Component
public class JudicialNoteView {

    private final JudicialNoteRenderer judicialNoteRenderer;

    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        setJudicialNoteFields(pcsCase, pcsCaseEntity.getJudicialNotes());
    }

    private void setJudicialNoteFields(PCSCase pcsCase, List<JudicialNoteEntity> judicialNoteEntities) {
        List<JudicialNote> judicialNotes = judicialNoteEntities.stream()
            .sorted(Comparator.comparing(
                JudicialNoteEntity::getCreatedOn,
                Comparator.nullsLast(Comparator.reverseOrder())
            ))
            .map(this::covertToJudicialNote).toList();

        pcsCase.setJudicialNotesMarkdown(judicialNoteRenderer.render(judicialNotes));
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
