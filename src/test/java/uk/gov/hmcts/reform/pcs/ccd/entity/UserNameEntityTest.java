package uk.gov.hmcts.reform.pcs.ccd.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class UserNameEntityTest {

    @Test
    void shouldAddJudicialNote() {
        // Given
        UserNameEntity userNameEntity = new UserNameEntity();
        JudicialNoteEntity judicialNoteEntity = mock(JudicialNoteEntity.class);

        // When
        userNameEntity.addJudicialNote(judicialNoteEntity);

        // Then
        assertThat(userNameEntity.getJudicialNotes()).hasSize(1);
        assertThat(userNameEntity.getJudicialNotes().getFirst()).isEqualTo(judicialNoteEntity);
        verify(judicialNoteEntity).setUser(userNameEntity);
    }

}
