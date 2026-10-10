package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.JudicialNoteEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.UserNameEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;

import java.time.Clock;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class JudicialNoteServiceTest {

    @Mock
    private PcsCaseService pcsCaseService;

    @Mock
    private PcsCaseRepository pcsCaseRepository;

    @Mock
    private UserNameService userNameService;

    @Mock
    private Clock utcClock;

    @InjectMocks
    private JudicialNoteService judicialNoteService;

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-22T21:05:30Z");

    @BeforeEach
    void setUp() {
        when(utcClock.instant()).thenReturn(FIXED_INSTANT);
    }

    @Test
    void shouldSaveNewJudicialNoteWithNoPreexistingJudicialNotes() {
        // Given
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().build();

        String note = "Note";
        PCSCase pcsCase = PCSCase.builder()
            .judicialNote(note)
            .build();

        UserNameEntity userNameEntity = UserNameEntity.builder().build();

        long caseReference = 12345L;
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);
        when(userNameService.getOrCreateUserNameEntity()).thenReturn(userNameEntity);

        // When
        judicialNoteService.addJudicialNote(caseReference, pcsCase);

        // Then
        ArgumentCaptor<PcsCaseEntity> pcsCaseEntityCaptor = ArgumentCaptor.forClass(PcsCaseEntity.class);
        verify(pcsCaseRepository).save(pcsCaseEntityCaptor.capture());

        PcsCaseEntity persistedCaseEntity = pcsCaseEntityCaptor.getValue();
        assertThat(persistedCaseEntity.getJudicialNotes()).hasSize(1);
        JudicialNoteEntity judicialNoteEntity = persistedCaseEntity.getJudicialNotes().getFirst();
        assertThat(judicialNoteEntity.getNote()).isEqualTo(note);
        assertThat(judicialNoteEntity.getCreatedOn()).isEqualTo(FIXED_INSTANT);
        assertThat(judicialNoteEntity.getPcsCase()).isEqualTo(pcsCaseEntity);
        assertThat(judicialNoteEntity.getUser()).isEqualTo(userNameEntity);
    }

    @Test
    void shouldSaveNewJudicialNoteWithPreexistingJudicialNotes() {
        // Given
        String preExistingNote = "Old note";
        Instant preExistingInstant = Instant.parse("2026-04-21T10:00:00Z");
        UserNameEntity preExistingUser = UserNameEntity.builder().build();

        JudicialNoteEntity preExistingJudicialNote = JudicialNoteEntity.builder()
            .note(preExistingNote)
            .createdOn(preExistingInstant)
            .build();
        preExistingUser.addJudicialNote(preExistingJudicialNote);

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().build();
        pcsCaseEntity.addJudicialNote(preExistingJudicialNote);

        String note = "New note";
        PCSCase pcsCase = PCSCase.builder()
            .judicialNote(note)
            .build();

        UserNameEntity userNameEntity = UserNameEntity.builder().build();

        long caseReference = 12345L;
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);
        when(userNameService.getOrCreateUserNameEntity()).thenReturn(userNameEntity);

        // When
        judicialNoteService.addJudicialNote(caseReference, pcsCase);

        // Then
        ArgumentCaptor<PcsCaseEntity> pcsCaseEntityCaptor = ArgumentCaptor.forClass(PcsCaseEntity.class);
        verify(pcsCaseRepository).save(pcsCaseEntityCaptor.capture());

        PcsCaseEntity persistedCaseEntity = pcsCaseEntityCaptor.getValue();
        assertThat(persistedCaseEntity.getJudicialNotes()).hasSize(2);

        JudicialNoteEntity judicialNoteEntity1 = persistedCaseEntity.getJudicialNotes().getFirst();
        assertThat(judicialNoteEntity1.getNote()).isEqualTo(preExistingNote);
        assertThat(judicialNoteEntity1.getCreatedOn()).isEqualTo(preExistingInstant);
        assertThat(judicialNoteEntity1.getPcsCase()).isEqualTo(pcsCaseEntity);
        assertThat(judicialNoteEntity1.getUser()).isEqualTo(preExistingUser);

        JudicialNoteEntity judicialNoteEntity2 = persistedCaseEntity.getJudicialNotes().getLast();
        assertThat(judicialNoteEntity2.getNote()).isEqualTo(note);
        assertThat(judicialNoteEntity2.getCreatedOn()).isEqualTo(FIXED_INSTANT);
        assertThat(judicialNoteEntity2.getPcsCase()).isEqualTo(pcsCaseEntity);
        assertThat(judicialNoteEntity2.getUser()).isEqualTo(userNameEntity);
    }
}
