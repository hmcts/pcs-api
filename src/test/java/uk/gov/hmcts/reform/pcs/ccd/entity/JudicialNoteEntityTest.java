package uk.gov.hmcts.reform.pcs.ccd.entity;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.domain.JudicialNote;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JudicialNoteEntityTest {

    @Test
    void shouldCreateJudicialNoteFromEntity() {
        // Given
        UserNameEntity userNameEntity = mock(UserNameEntity.class);
        JudicialNoteEntity judicialNoteEntity = JudicialNoteEntity.builder()
            .note("Note")
            .createdOn(LocalDateTime.of(2026, 2, 1, 9, 0, 0).toInstant(ZoneOffset.UTC))
            .user(userNameEntity)
            .build();

        when(userNameEntity.getName()).thenReturn("John Smith");

        // When
        JudicialNote note = JudicialNoteEntity.fromEntity(judicialNoteEntity);
        assertThat(note).isNotNull();
        assertThat(note.getNote()).isEqualTo("Note");
        assertThat(note.getCreatedOn()).isEqualTo(LocalDateTime.of(2026, 2, 1, 9, 0, 0));
        assertThat(note.getCreatedBy()).isEqualTo("John Smith");
    }
}
