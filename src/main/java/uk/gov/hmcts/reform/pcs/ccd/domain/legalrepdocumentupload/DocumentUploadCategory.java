package uk.gov.hmcts.reform.pcs.ccd.domain.legalrepdocumentupload;

import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

@AllArgsConstructor
public enum DocumentUploadCategory {

    ADJOURN_HEARING_APPLICATION(
        "Yes, the documents I’m uploading relate to %s: the application to adjourn the hearing - submitted on %s",
        true),
    SET_ASIDE_ORDER_APPLICATION(
        "Yes, the documents I’m uploading relate to %s: an application to set aside the order - submitted on %s",
        true),
    GENERAL_APPLICATION(
        "Yes, the documents I’m uploading relate to %s: an application submitted on %s",
        true),
    MAIN_CLAIM_OR_COUNTERCLAIM(
        "No, the documents I’m uploading relate to the main claim or counterclaim",
        false);

    private final String label;
    private final boolean requiresDate;

    private static final DateTimeFormatter LABEL_DATE_FORMAT =
        DateTimeFormatter.ofPattern("EEEE d MMMM uuuu", Locale.UK);

    public String getLabel() {
        if (requiresDate) {
            throw new IllegalArgumentException("Existing application labels require submitted application details");
        }
        return label;
    }

    public String getLabel(LocalDateTime dateTime, String relatedEntityLabel) {
        if (!requiresDate) {
            throw new IllegalArgumentException("Main claim or counterclaim label does not use submitted application details");
        }
        Objects.requireNonNull(relatedEntityLabel, "relatedEntityLabel must not be null");
        Objects.requireNonNull(dateTime, "dateTime must not be null");
        String dateText = dateTime.format(LABEL_DATE_FORMAT);
        return String.format(label, relatedEntityLabel, dateText);
    }
}
