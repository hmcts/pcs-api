package uk.gov.hmcts.reform.pcs.ccd.view;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseReviewDate;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseNote;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseNoteEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseReviewDateEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Component
public class CaseNoteView {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("d MMM yyyy, h:mm:ss a", Locale.ENGLISH);

    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        setCaseNoteFields(pcsCase, pcsCaseEntity.getCaseNotes());
        setReviewDateFields(pcsCase, pcsCaseEntity.getReviewDates());
    }

    private void setCaseNoteFields(PCSCase pcsCase, List<CaseNoteEntity> caseNoteEntities) {
        List<ListValue<CaseNote>> caseNotes = caseNoteEntities.stream().map(caseNoteEntity -> {
            CaseNote caseNote = CaseNote.builder()
                .note(caseNoteEntity.getNote())
                .createdOn(CaseNoteEntity.fromEntity(caseNoteEntity).getCreatedOn())
                .createdBy(caseNoteEntity.getCreatedBy())
                .build();

            ListValue<CaseNote> listValue = new ListValue<>();
            listValue.setValue(caseNote);

            return listValue;
        }).toList();

        pcsCase.setCaseNotes(caseNotes);
    }

    private void setReviewDateFields(PCSCase pcsCase, List<CaseReviewDateEntity> reviewDateEntities) {
        List<CaseReviewDateEntity> orderedReviewDateEntities = reviewDateEntities.stream()
            .sorted(Comparator.comparing(
                CaseReviewDateEntity::getCreatedDate,
                Comparator.nullsLast(Comparator.reverseOrder())
            ))
            .toList();

        List<ListValue<CaseReviewDate>> reviewDates = orderedReviewDateEntities.stream()
            .map(this::toListValue)
            .toList();

        pcsCase.setCaseReviewDates(reviewDates);
        pcsCase.setCaseReviewDatesMarkdown(toMarkdown(orderedReviewDateEntities));
    }

    private ListValue<CaseReviewDate> toListValue(CaseReviewDateEntity reviewDateEntity) {
        CaseReviewDate reviewDate = CaseReviewDate.builder()
            .createdBy(reviewDateEntity.getCreatedBy())
            .createdDate(reviewDateEntity.getCreatedDate())
            .date(reviewDateEntity.getDate())
            .reason(reviewDateEntity.getReason())
            .description(reviewDateEntity.getDescription())
            .build();

        return new ListValue<>("Review date " + reviewDateEntity.getRank(), reviewDate);
    }

    private String toMarkdown(List<CaseReviewDateEntity> reviewDateEntities) {
        if (reviewDateEntities.isEmpty()) {
            return "";
        }

        return reviewDateEntities.stream()
            .map(this::toMarkdown)
            .reduce((first, second) -> first + "\n\n" + second)
            .orElse("");
    }

    private String toMarkdown(CaseReviewDateEntity reviewDateEntity) {
        return """
            ### Review date %s

            |  |  |
            | --- | --- |
            | Created by | %s |
            | Created date | %s |
            | Date of review | %s |
            | Reason | %s |
            | Description of review | %s |
            """.formatted(
                reviewDateEntity.getRank(),
                escape(reviewDateEntity.getCreatedBy()),
                formatDateTime(reviewDateEntity.getCreatedDate()),
                formatDate(reviewDateEntity.getDate()),
                reviewDateEntity.getReason() == null ? "" : reviewDateEntity.getReason().getLabel(),
                escape(reviewDateEntity.getDescription())
            );
    }

    private String formatDate(LocalDate date) {
        return date == null ? "" : date.format(DATE_FORMATTER);
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "" : dateTime.format(DATE_TIME_FORMATTER);
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("|", "\\|").replace("\n", "<br>");
    }
}
