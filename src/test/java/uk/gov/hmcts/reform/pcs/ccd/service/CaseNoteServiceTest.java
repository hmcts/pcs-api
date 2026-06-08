package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseNoteEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseNoteServiceTest {

    @Mock
    private PcsCaseService pcsCaseService;

    @Mock
    private PcsCaseRepository pcsCaseRepository;

    @Mock
    private SecurityContextService securityContextService;

    @Mock
    private Clock utcClock;

    @InjectMocks
    private CaseNoteService caseNoteService;

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-22T21:05:30Z");

    @BeforeEach
    void setUp() {
        when(utcClock.instant()).thenReturn(FIXED_INSTANT);
    }

    @Test
    void shouldSaveNewCaseNoteWithNoPreexistingCaseNotes() {
        // Given
        ClaimEntity claimEntity = ClaimEntity.builder().build();
        List<ClaimEntity> claimEntities = new ArrayList<>();
        claimEntities.add(claimEntity);

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .claims(claimEntities)
            .build();

        String note = "Note";
        String name = "Name";

        UserInfo userInfo = UserInfo.builder()
            .name(name)
            .build();

        PCSCase pcsCase = PCSCase.builder()
            .note(note)
            .build();

        long caseReference = 12345L;
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);
        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);

        // When
        caseNoteService.addCaseNote(caseReference, pcsCase);

        ArgumentCaptor<PcsCaseEntity> pcsCaseEntityCaptor = ArgumentCaptor.forClass(PcsCaseEntity.class);
        verify(pcsCaseRepository).save(pcsCaseEntityCaptor.capture());

        PcsCaseEntity persistedCaseEntity = pcsCaseEntityCaptor.getValue();
        assertThat(persistedCaseEntity.getCaseNotes()).hasSize(1);
        CaseNoteEntity persistedCaseNote = persistedCaseEntity.getCaseNotes().getFirst();
        assertThat(persistedCaseNote.getPcsCase()).isEqualTo(persistedCaseEntity);
        assertThat(persistedCaseNote.getNote()).isEqualTo(note);
        assertThat(persistedCaseNote.getCreatedBy()).isEqualTo(name);
        assertThat(persistedCaseNote.getCreatedOn()).isEqualTo(FIXED_INSTANT);
    }

    @Test
    void shouldSaveNewCaseNoteWithPreexistingCaseNotes() {
        // Given
        String preExistingNote = "Note 1";
        String preExistingAuthor = "Name 1";
        Instant preExistingInstant = Instant.parse("2026-04-21T10:00:00Z");

        CaseNoteEntity preExistingCaseNote = CaseNoteEntity.builder()
            .note(preExistingNote)
            .createdBy(preExistingAuthor)
            .createdOn(preExistingInstant)
            .build();
        List<CaseNoteEntity> caseNoteEntities = new ArrayList<>();
        caseNoteEntities.add(preExistingCaseNote);

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity
            .builder()
            .caseNotes(caseNoteEntities)
            .build();

        String newNote = "New Note";
        String newAuthor = "New Author";

        UserInfo userInfo = UserInfo.builder()
            .name(newAuthor)
            .build();

        PCSCase pcsCase = PCSCase.builder()
            .note(newNote)
            .build();

        long caseReference = 12345L;
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);
        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);

        // When
        caseNoteService.addCaseNote(caseReference, pcsCase);

        // Then
        ArgumentCaptor<PcsCaseEntity> pcsCaseEntityCaptor = ArgumentCaptor.forClass(PcsCaseEntity.class);
        verify(pcsCaseRepository).save(pcsCaseEntityCaptor.capture());

        PcsCaseEntity persistedCaseEntity = pcsCaseEntityCaptor.getValue();

        // Verify the collection now has 2 notes
        assertThat(persistedCaseEntity.getCaseNotes()).hasSize(2);

        // Verify the pre-existing note is still there
        CaseNoteEntity firstCaseNote = persistedCaseEntity.getCaseNotes().getFirst();
        assertThat(firstCaseNote.getNote()).isEqualTo(preExistingNote);
        assertThat(firstCaseNote.getCreatedBy()).isEqualTo(preExistingAuthor);
        assertThat(firstCaseNote.getCreatedOn()).isEqualTo(preExistingInstant);

        // Verify the new note was added
        CaseNoteEntity secondCaseNote = persistedCaseEntity.getCaseNotes().getLast();
        assertThat(secondCaseNote.getNote()).isEqualTo(newNote);
        assertThat(secondCaseNote.getCreatedBy()).isEqualTo(newAuthor);
        assertThat(secondCaseNote.getCreatedOn()).isEqualTo(FIXED_INSTANT);
    }

    @Test
    void shouldMaintainOrderOfCaseNotesWhenAddingNewNote() {
        // Given
        List<CaseNoteEntity> existingNotes = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            CaseNoteEntity note = CaseNoteEntity.builder()
                    .note("Existing Note " + i)
                    .createdBy("Author " + i)
                    .createdOn(Instant.parse("2026-04-" + (20 + i) + "T10:00:00Z"))
                    .build();
            existingNotes.add(note);
        }

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .caseNotes(existingNotes)
            .build();

        String newNote = "Latest Note";
        String newAuthor = "Latest Author";

        UserInfo userInfo = UserInfo.builder()
                .name(newAuthor)
                .build();

        PCSCase pcsCase = PCSCase.builder()
                .note(newNote)
                .build();

        long caseReference = 12345L;
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);
        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);

        // When
        caseNoteService.addCaseNote(caseReference, pcsCase);

        // Then
        ArgumentCaptor<PcsCaseEntity> pcsCaseEntityCaptor = ArgumentCaptor.forClass(PcsCaseEntity.class);
        verify(pcsCaseRepository).save(pcsCaseEntityCaptor.capture());

        PcsCaseEntity persistedCaseEntity = pcsCaseEntityCaptor.getValue();

        assertThat(persistedCaseEntity.getCaseNotes()).hasSize(4);

        assertThat(persistedCaseEntity.getCaseNotes().get(0).getNote()).isEqualTo("Existing Note 1");
        assertThat(persistedCaseEntity.getCaseNotes().get(1).getNote()).isEqualTo("Existing Note 2");
        assertThat(persistedCaseEntity.getCaseNotes().get(2).getNote()).isEqualTo("Existing Note 3");

        assertThat(persistedCaseEntity.getCaseNotes().getLast().getNote()).isEqualTo(newNote);
        assertThat(persistedCaseEntity.getCaseNotes().getLast().getCreatedBy()).isEqualTo(newAuthor);
        assertThat(persistedCaseEntity.getCaseNotes().getLast().getCreatedOn()).isEqualTo(FIXED_INSTANT);
    }
}
