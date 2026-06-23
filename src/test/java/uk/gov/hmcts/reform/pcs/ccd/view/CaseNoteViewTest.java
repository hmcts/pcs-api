package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseNote;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseNoteEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;

class CaseNoteViewTest {

    private static final Instant SUMMER_INSTANT = Instant.parse("2026-04-22T21:00:00Z");

    private static final Instant WINTER_INSTANT = Instant.parse("2026-01-15T12:00:00Z");

    private CaseNoteView caseNoteView;

    @BeforeEach
    void setUp() {
        caseNoteView = new CaseNoteView();
    }

    @Test
    void shouldMapCaseNoteEntityToCaseNoteDuringSummer() {
        // Given
        String note = "Summer note";
        String name = "John Smith";

        CaseNoteEntity caseNoteEntity = CaseNoteEntity.builder()
            .note(note)
            .createdBy(name)
            .createdOn(SUMMER_INSTANT)
            .build();
        List<CaseNoteEntity> caseNoteEntities = new ArrayList<>();
        caseNoteEntities.add(caseNoteEntity);
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .caseNotes(caseNoteEntities)
            .build();

        PCSCase pcsCase = PCSCase.builder().build();

        // When
        caseNoteView.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        List<ListValue<CaseNote>> caseNotes = pcsCase.getCaseNotes();
        assertThat(caseNotes).hasSize(1);

        CaseNote caseNote = caseNotes.getFirst().getValue();
        assertThat(caseNote.getNote()).isEqualTo(note);
        assertThat(caseNote.getCreatedBy()).isEqualTo(name);

        LocalDateTime expectedCreatedOn = LocalDateTime.ofInstant(SUMMER_INSTANT, UK_ZONE_ID);
        assertThat(caseNote.getCreatedOn()).isEqualTo(expectedCreatedOn);

        assertThat(caseNote.getCreatedOn().getHour()).isEqualTo(22);
        assertThat(caseNote.getCreatedOn().getYear()).isEqualTo(2026);
        assertThat(caseNote.getCreatedOn().getMonthValue()).isEqualTo(4);
        assertThat(caseNote.getCreatedOn().getDayOfMonth()).isEqualTo(22);

        ZonedDateTime zonedDateTime = SUMMER_INSTANT.atZone(UK_ZONE_ID);
        assertThat(zonedDateTime.getOffset()).isEqualTo(ZoneOffset.ofHours(1));
    }

    @Test
    void shouldMapCaseNoteEntityToCaseNoteInPCSCaseDuringWinter() {
        String note = "Winter note";
        String name = "Joe Bloggs";

        CaseNoteEntity caseNoteEntity = CaseNoteEntity.builder()
                .note(note)
                .createdBy(name)
                .createdOn(WINTER_INSTANT)
                .build();
        List<CaseNoteEntity> caseNoteEntities = new ArrayList<>();
        caseNoteEntities.add(caseNoteEntity);
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
                .caseNotes(caseNoteEntities)
                .build();

        PCSCase pcsCase = PCSCase.builder().build();

        // When
        caseNoteView.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        List<ListValue<CaseNote>> caseNotes = pcsCase.getCaseNotes();
        assertThat(caseNotes).hasSize(1);

        CaseNote caseNote = caseNotes.getFirst().getValue();
        assertThat(caseNote.getNote()).isEqualTo(note);
        assertThat(caseNote.getCreatedBy()).isEqualTo(name);

        LocalDateTime expectedCreatedOn = LocalDateTime.ofInstant(WINTER_INSTANT, UK_ZONE_ID);
        assertThat(caseNote.getCreatedOn()).isEqualTo(expectedCreatedOn);

        assertThat(caseNote.getCreatedOn().getHour()).isEqualTo(12);
        assertThat(caseNote.getCreatedOn().getYear()).isEqualTo(2026);
        assertThat(caseNote.getCreatedOn().getMonthValue()).isEqualTo(1);
        assertThat(caseNote.getCreatedOn().getDayOfMonth()).isEqualTo(15);

        ZonedDateTime zonedDateTime = WINTER_INSTANT.atZone(UK_ZONE_ID);
        assertThat(zonedDateTime.getOffset()).isEqualTo(ZoneOffset.UTC);
    }
}
