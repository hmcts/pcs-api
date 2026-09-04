package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseNote;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseReviewDate;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.ReviewReason;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseNoteEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseReviewDateEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;

import java.time.Instant;
import java.time.LocalDate;
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

    @Test
    void shouldMapReviewDateEntitiesToCaseReviewDatesWithNewestFirst() {
        CaseReviewDateEntity olderReviewDate = CaseReviewDateEntity.builder()
            .reviewDateNumber(1)
            .createdBy("Older Worker")
            .createdDate(LocalDateTime.of(2026, 1, 15, 12, 0))
            .date(LocalDate.of(2026, 8, 20))
            .reason(ReviewReason.GENERAL_ORDER)
            .description("older review")
            .build();
        CaseReviewDateEntity newerReviewDate = CaseReviewDateEntity.builder()
            .reviewDateNumber(2)
            .createdBy("Newer Worker")
            .createdDate(LocalDateTime.of(2026, 4, 22, 22, 0))
            .date(LocalDate.of(2026, 9, 15))
            .reason(ReviewReason.OTHER)
            .description("newer review")
            .build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .caseNotes(List.of())
            .reviewDates(List.of(olderReviewDate, newerReviewDate))
            .build();
        PCSCase pcsCase = PCSCase.builder().build();

        caseNoteView.setCaseFields(pcsCase, pcsCaseEntity);

        List<ListValue<CaseReviewDate>> reviewDates = pcsCase.getCaseReviewDates();
        assertThat(reviewDates).hasSize(2);
        assertThat(reviewDates.getFirst().getId()).isEqualTo("Review date 2");
        assertThat(reviewDates.getFirst().getValue().getCreatedBy()).isEqualTo("Newer Worker");
        assertThat(reviewDates.getFirst().getValue().getCreatedDate())
            .isEqualTo(LocalDateTime.of(2026, 4, 22, 22, 0));
        assertThat(reviewDates.getFirst().getValue().getDate()).isEqualTo(newerReviewDate.getDate());
        assertThat(reviewDates.getFirst().getValue().getReason()).isEqualTo(ReviewReason.OTHER);
        assertThat(reviewDates.getFirst().getValue().getDescription()).isEqualTo("newer review");

        assertThat(reviewDates.getLast().getId()).isEqualTo("Review date 1");
        assertThat(reviewDates.getLast().getValue().getCreatedBy()).isEqualTo("Older Worker");

        assertThat(pcsCase.getCaseReviewDatesMarkdown()).containsSubsequence(
            "### Review date 2",
            "| Created by | Newer Worker |",
            "| Created date | 22 Apr 2026, 10:00:00 PM |",
            "| Date of review | 15 Sep 2026 |",
            "| Reason | Other |",
            "| Description of review | newer review |",
            "### Review date 1",
            "| Created by | Older Worker |"
        );
    }
}
