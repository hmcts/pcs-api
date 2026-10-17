package uk.gov.hmcts.reform.pcs.enforcement.view;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.enforcement.domain.BailiffNote;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.entity.BailiffNoteEntity;
import uk.gov.hmcts.reform.pcs.enforcement.service.BailiffNoteService;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;

/**
 * Shows the current enforcement's bailiff notes on the case, newest first.
 */
@Component
@RequiredArgsConstructor
public class BailiffNoteView {

    /**
     * "1 Feb 2026 16:20pm": a 24 hour time followed by a lower case am / pm marker, as in the designs.
     */
    static final DateTimeFormatter ADDED_ON_FORMAT = new DateTimeFormatterBuilder()
        .appendPattern("d MMM yyyy HH:mm")
        .appendText(ChronoField.AMPM_OF_DAY, Map.of(0L, "am", 1L, "pm"))
        .toFormatter(Locale.UK)
        .withZone(UK_ZONE_ID);

    private final BailiffNoteService bailiffNoteService;

    /**
     * Leaves the field empty when the current enforcement has no notes, or the claim has no enforcement. It
     * is cleared rather than left alone, because a draft "Enforce the order" may have replaced the enforcement
     * order on the case view with one carrying an old copy of the notes.
     */
    public void setBailiffNotes(PcsCaseEntity pcsCaseEntity, PCSCase pcsCase) {
        if (pcsCaseEntity == null || pcsCase == null) {
            return;
        }
        List<BailiffNoteEntity> notes = bailiffNoteService.getCurrentEnforcementNotes(pcsCaseEntity);
        if (notes.isEmpty()) {
            if (pcsCase.getEnforcementOrder() != null) {
                pcsCase.getEnforcementOrder().setBailiffNotes(null);
            }
            return;
        }
        enforcementOrderOf(pcsCase).setBailiffNotes(notes.stream().map(BailiffNoteView::toListValue).toList());
    }

    static String formatAddedOn(Instant createdAt) {
        return ADDED_ON_FORMAT.format(createdAt);
    }

    private static ListValue<BailiffNote> toListValue(BailiffNoteEntity entity) {
        BailiffNote note = BailiffNote.builder()
            .note(entity.getNote())
            .addedBy(authorName(entity))
            .addedOn(formatAddedOn(entity.getCreatedAt()))
            .build();
        return new ListValue<>(entity.getId() == null ? null : entity.getId().toString(), note);
    }

    private static String authorName(BailiffNoteEntity entity) {
        return Stream.of(entity.getAuthorFirstName(), entity.getAuthorLastName())
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(part -> !part.isEmpty())
            .collect(Collectors.joining(" "));
    }

    private static EnforcementOrder enforcementOrderOf(PCSCase pcsCase) {
        if (pcsCase.getEnforcementOrder() == null) {
            pcsCase.setEnforcementOrder(new EnforcementOrder());
        }
        return pcsCase.getEnforcementOrder();
    }
}
