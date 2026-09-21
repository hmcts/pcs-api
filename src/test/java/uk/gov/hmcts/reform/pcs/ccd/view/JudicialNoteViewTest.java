package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.JudicialNote;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.JudicialNoteEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.UserNameEntity;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;

public class JudicialNoteViewTest {

    private static final Instant SUMMER_INSTANT = Instant.parse("2026-04-22T21:00:00Z");

    private static final Instant WINTER_INSTANT = Instant.parse("2026-01-15T12:00:00Z");

    private JudicialNoteView judicialNoteView;

    @BeforeEach
    void setUp() {
        judicialNoteView = new JudicialNoteView();
    }

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

        // When
        judicialNoteView.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        List<ListValue<JudicialNote>> judicialNotes = pcsCase.getJudicialNotes();
        assertThat(judicialNotes).hasSize(1);

        JudicialNote judicialNote = judicialNotes.getFirst().getValue();
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

        // When
        judicialNoteView.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        List<ListValue<JudicialNote>> judicialNotes = pcsCase.getJudicialNotes();
        assertThat(judicialNotes).hasSize(1);

        JudicialNote judicialNote = judicialNotes.getFirst().getValue();
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
    }

}
