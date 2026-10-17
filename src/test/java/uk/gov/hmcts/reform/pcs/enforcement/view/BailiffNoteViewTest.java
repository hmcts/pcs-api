package uk.gov.hmcts.reform.pcs.enforcement.view;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.enforcement.domain.BailiffNote;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.entity.BailiffNoteEntity;
import uk.gov.hmcts.reform.pcs.enforcement.service.BailiffNoteService;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BailiffNoteViewTest {

    private final PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();

    @Mock
    private BailiffNoteService bailiffNoteService;

    @InjectMocks
    private BailiffNoteView underTest;

    @ParameterizedTest
    @CsvSource({
        // GMT
        "2026-02-01T16:20:00Z, 1 Feb 2026 16:20pm",
        "2026-02-03T09:05:00Z, 3 Feb 2026 09:05am",
        "2026-02-03T00:00:00Z, 3 Feb 2026 00:00am",
        "2026-02-03T12:00:00Z, 3 Feb 2026 12:00pm",
        // BST: shown in UK time, an hour ahead of UTC
        "2026-07-14T15:20:00Z, 14 Jul 2026 16:20pm",
        "2026-07-14T23:30:00Z, 15 Jul 2026 00:30am"
    })
    void shouldFormatTheTimeInUkTimeAsTheDesignsShow(String createdAt, String expected) {
        assertThat(BailiffNoteView.formatAddedOn(Instant.parse(createdAt))).isEqualTo(expected);
    }

    @Test
    void shouldShowTheNotesInTheOrderGivenWithAuthorAndTime() {
        BailiffNoteEntity newer = note("Rear gate is locked", "Sam", "Jones", "2026-02-03T10:00:00Z");
        BailiffNoteEntity older = note("Dog on site", "Alex", "Smith", "2026-02-01T16:20:00Z");
        when(bailiffNoteService.getCurrentEnforcementNotes(pcsCaseEntity)).thenReturn(List.of(newer, older));
        PCSCase pcsCase = PCSCase.builder().build();

        underTest.setBailiffNotes(pcsCaseEntity, pcsCase);

        List<ListValue<BailiffNote>> notes = pcsCase.getEnforcementOrder().getBailiffNotes();
        assertThat(notes).extracting(ListValue::getId)
            .containsExactly(newer.getId().toString(), older.getId().toString());
        assertThat(notes).extracting(ListValue::getValue).containsExactly(
            new BailiffNote("Rear gate is locked", "Sam Jones", "3 Feb 2026 10:00am"),
            new BailiffNote("Dog on site", "Alex Smith", "1 Feb 2026 16:20pm"));
    }

    @Test
    void shouldShowWhicheverPartsOfTheAuthorsNameIdamGave() {
        when(bailiffNoteService.getCurrentEnforcementNotes(pcsCaseEntity)).thenReturn(List.of(
            note("a", "Sam", null, "2026-02-01T10:00:00Z"),
            note("b", " ", "Jones", "2026-02-01T10:00:00Z"),
            note("c", null, null, "2026-02-01T10:00:00Z")));
        PCSCase pcsCase = PCSCase.builder().build();

        underTest.setBailiffNotes(pcsCaseEntity, pcsCase);

        assertThat(pcsCase.getEnforcementOrder().getBailiffNotes())
            .extracting(note -> note.getValue().getAddedBy())
            .containsExactly("Sam", "Jones", "");
    }

    @Test
    void shouldClearAnOldCopyOfTheNotesWhenThereAreNone() {
        when(bailiffNoteService.getCurrentEnforcementNotes(pcsCaseEntity)).thenReturn(List.of());
        PCSCase pcsCase = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder()
                                  .bailiffNotes(List.of(new ListValue<>("1", new BailiffNote())))
                                  .build())
            .build();

        underTest.setBailiffNotes(pcsCaseEntity, pcsCase);

        assertThat(pcsCase.getEnforcementOrder().getBailiffNotes()).isNull();
    }

    @Test
    void shouldLeaveTheCaseAloneWhenThereAreNoNotesAndNoEnforcementOrder() {
        when(bailiffNoteService.getCurrentEnforcementNotes(pcsCaseEntity)).thenReturn(List.of());
        PCSCase pcsCase = PCSCase.builder().build();

        underTest.setBailiffNotes(pcsCaseEntity, pcsCase);

        assertThat(pcsCase.getEnforcementOrder()).isNull();
    }

    @Test
    void shouldDoNothingWithoutACase() {
        underTest.setBailiffNotes(null, PCSCase.builder().build());
        underTest.setBailiffNotes(pcsCaseEntity, null);

        verifyNoInteractions(bailiffNoteService);
    }

    private static BailiffNoteEntity note(String text, String firstName, String lastName, String createdAt) {
        return BailiffNoteEntity.builder()
            .id(UUID.randomUUID())
            .note(text)
            .authorFirstName(firstName)
            .authorLastName(lastName)
            .createdAt(Instant.parse(createdAt))
            .build();
    }
}
