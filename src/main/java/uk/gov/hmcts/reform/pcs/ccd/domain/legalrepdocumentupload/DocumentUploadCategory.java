package uk.gov.hmcts.reform.pcs.ccd.domain.legalrepdocumentupload;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@AllArgsConstructor
@Getter
public enum DocumentUploadCategory implements HasLabel {

    ADJOURN_HEARING_APPLICATION(
        "Yes, the documents I'm uploading relate to the application to adjourn the hearing - submitted on %s",
        true),
    SUSPEND_EVICTION_APPLICATION(
        "Yes, the documents I'm uploading relate to an application to suspend the eviction - submitted on %s",
        true),
    SET_ASIDE_ORDER_APPLICATION(
        "Yes, the documents I'm uploading relate to an application to set aside the order - submitted on %s",
        true),
    GENERAL_APPLICATION(
        "Yes, the documents I'm uploading relate to an application submitted on %s",
        true),
    MAIN_CLAIM_OR_COUNTERCLAIM(
        "No, the documents I'm uploading relate to the main claim or counterclaim",
        false);

    private final String label;
    private final boolean requiresDate;

    private static final DateTimeFormatter LABEL_DATE_FORMAT =
        DateTimeFormatter.ofPattern("EEEE d MMM uuuu", Locale.UK);

    public String getLabel(LocalDateTime date) {
        if (!requiresDate) {
            return label;
        }
        String dateText = date == null ? "" : date.format(LABEL_DATE_FORMAT);
        return String.format(label, dateText);
    }
}
