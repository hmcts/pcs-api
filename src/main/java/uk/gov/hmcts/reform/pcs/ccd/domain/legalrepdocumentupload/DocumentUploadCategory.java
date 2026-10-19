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
    private final boolean requiresParams;

    private static final DateTimeFormatter LABEL_DATE_FORMAT =
        DateTimeFormatter.ofPattern("EEEE d MMMM uuuu", Locale.UK);

    public String getLabel() {
        if (requiresParams) {
            throw new IllegalArgumentException("Label requires parameters");
        }
        return label;
    }

    public String getLabel(LocalDateTime dateTime, String relatedEntityLabel) {
        if (!requiresParams) {
            throw new IllegalArgumentException("Label does not use parameters");
        }
        Objects.requireNonNull(relatedEntityLabel, "relatedEntityLabel must not be null");
        Objects.requireNonNull(dateTime, "dateTime must not be null");
        String dateText = dateTime.format(LABEL_DATE_FORMAT);
        return String.format(label, relatedEntityLabel, dateText);
    }
}
