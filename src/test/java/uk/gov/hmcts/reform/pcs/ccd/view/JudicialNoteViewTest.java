package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.JudicialNote;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.JudicialNoteEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.UserNameEntity;
import uk.gov.hmcts.reform.pcs.ccd.renderer.tabs.JudicialNoteRenderer;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;

@ExtendWith(MockitoExtension.class)
public class JudicialNoteViewTest {

    @Mock
    private JudicialNoteRenderer judicialNoteRenderer;

    @InjectMocks
    private JudicialNoteView judicialNoteView;

    @Captor
    private ArgumentCaptor<List<JudicialNote>> judicialNotesCaptor;

    private static final Instant SUMMER_INSTANT = Instant.parse("2026-04-22T21:00:00Z");

    private static final Instant WINTER_INSTANT = Instant.parse("2026-01-15T12:00:00Z");

    @Test
    void shouldMapJudicialNoteEntityToJudicialNoteDuringSummer() {
        // Given
        String note = "Summer note";
        String name = "John Smith";
        UserNameEntity userNameEntity = UserNameEntity.builder().name(name).build();

        JudicialNoteEntity judicialNoteEntity = JudicialNoteEntity.builder()
            .note(note)
            .createdOn(SUMMER_INSTANT)
            .user(userNameEntity)
            .build();

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .judicialNotes(List.of(judicialNoteEntity))
            .build();

        PCSCase pcsCase = PCSCase.builder().build();

        when(judicialNoteRenderer.render(anyList())).thenReturn("Render");

        // When
        judicialNoteView.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        verify(judicialNoteRenderer).render(judicialNotesCaptor.capture());
        List<JudicialNote> judicialNotes = judicialNotesCaptor.getValue();
        assertThat(judicialNotes).hasSize(1);

        JudicialNote judicialNote = judicialNotes.getFirst();
        assertThat(judicialNote.getNote()).isEqualTo(note);
        assertThat(judicialNote.getCreatedBy()).isEqualTo(name);

        LocalDateTime expectedCreatedOn = LocalDateTime.ofInstant(SUMMER_INSTANT, UK_ZONE_ID);
        assertThat(judicialNote.getCreatedOn()).isEqualTo(expectedCreatedOn);

        assertThat(judicialNote.getCreatedOn().getHour()).isEqualTo(22);
        assertThat(judicialNote.getCreatedOn().getYear()).isEqualTo(2026);
        assertThat(judicialNote.getCreatedOn().getMonthValue()).isEqualTo(4);
        assertThat(judicialNote.getCreatedOn().getDayOfMonth()).isEqualTo(22);

        ZonedDateTime zonedDateTime = SUMMER_INSTANT.atZone(UK_ZONE_ID);
        assertThat(zonedDateTime.getOffset()).isEqualTo(ZoneOffset.ofHours(1));

        assertThat(pcsCase.getJudicialNotesMarkdown()).isEqualTo("Render");
    }

    @Test
    void shouldMapJudicialNoteEntityToJudicialNoteDuringWinter() {
        // Given
        String note = "Winter note";
        String name = "Joe Bloggs";
        UserNameEntity userNameEntity = UserNameEntity.builder().name(name).build();

        JudicialNoteEntity judicialNoteEntity = JudicialNoteEntity.builder()
            .note(note)
            .createdOn(WINTER_INSTANT)
            .user(userNameEntity)
            .build();

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .judicialNotes(List.of(judicialNoteEntity))
            .build();

        PCSCase pcsCase = PCSCase.builder().build();

        when(judicialNoteRenderer.render(anyList())).thenReturn("Render");

        // When
        judicialNoteView.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        verify(judicialNoteRenderer).render(judicialNotesCaptor.capture());
        List<JudicialNote> judicialNotes = judicialNotesCaptor.getValue();
        assertThat(judicialNotes).hasSize(1);

        JudicialNote judicialNote = judicialNotes.getFirst();
        assertThat(judicialNote.getNote()).isEqualTo(note);
        assertThat(judicialNote.getCreatedBy()).isEqualTo(name);

        LocalDateTime expectedCreatedOn = LocalDateTime.ofInstant(WINTER_INSTANT, UK_ZONE_ID);
        assertThat(judicialNote.getCreatedOn()).isEqualTo(expectedCreatedOn);

        assertThat(judicialNote.getCreatedOn().getHour()).isEqualTo(12);
        assertThat(judicialNote.getCreatedOn().getYear()).isEqualTo(2026);
        assertThat(judicialNote.getCreatedOn().getMonthValue()).isEqualTo(1);
        assertThat(judicialNote.getCreatedOn().getDayOfMonth()).isEqualTo(15);

        ZonedDateTime zonedDateTime = WINTER_INSTANT.atZone(UK_ZONE_ID);
        assertThat(zonedDateTime.getOffset()).isEqualTo(ZoneOffset.UTC);

        assertThat(pcsCase.getJudicialNotesMarkdown()).isEqualTo("Render");
    }

    @Test
    void shouldOrderJudicialNotesInByMostRecentCreationDate() {
        // Given
        String oldNote = "Old note";
        String newNote = "New note";
        String name = "John Smith";
        UserNameEntity userNameEntity = UserNameEntity.builder().name(name).build();

        JudicialNoteEntity oldJudicialNoteEntity = JudicialNoteEntity.builder()
            .note(oldNote)
            .createdOn(WINTER_INSTANT)
            .user(userNameEntity)
            .build();

        JudicialNoteEntity newJudicialNoteEntity = JudicialNoteEntity.builder()
            .note(newNote)
            .createdOn(SUMMER_INSTANT)
            .user(userNameEntity)
            .build();

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .judicialNotes(List.of(oldJudicialNoteEntity, newJudicialNoteEntity))
            .build();

        PCSCase pcsCase = PCSCase.builder().build();

        when(judicialNoteRenderer.render(anyList())).thenReturn("Render");

        // When
        judicialNoteView.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        verify(judicialNoteRenderer).render(judicialNotesCaptor.capture());
        List<JudicialNote> judicialNotes = judicialNotesCaptor.getValue();
        assertThat(judicialNotes).hasSize(2);

        JudicialNote judicialNote1 = judicialNotes.getFirst();
        assertThat(judicialNote1.getNote()).isEqualTo(newNote);

        JudicialNote judicialNote2 = judicialNotes.getLast();
        assertThat(judicialNote2.getNote()).isEqualTo(oldNote);

        assertThat(pcsCase.getJudicialNotesMarkdown()).isEqualTo("Render");
    }

}
