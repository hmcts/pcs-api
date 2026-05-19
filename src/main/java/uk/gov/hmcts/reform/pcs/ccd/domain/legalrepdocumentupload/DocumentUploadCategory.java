package uk.gov.hmcts.reform.pcs.ccd.domain.legalrepdocumentupload;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

import java.util.Arrays;
import java.util.List;

@AllArgsConstructor
@Getter
public enum DocumentUploadCategory implements HasLabel {

    ADJOURN_HEARING_APPLICATION(
        "Yes, the documents I'm uploading relate to the application to adjourn the hearing - submitted on %s"),
    SUSPEND_EVICTION_APPLICATION(
        "Yes, the documents I'm uploading relate to an application to suspend the eviction - submitted on %s"),
    SET_ASIDE_ORDER_APPLICATION(
        "Yes, the documents I'm uploading relate to an application to set aside the order - submitted on %s"),
    GENERAL_APPLICATION(
        "Yes, the documents I'm uploading relate to an application submitted on %s"),
    MAIN_CLAIM_OR_COUNTERCLAIM(
        "No, the documents I'm uploading relate to the main claim or counterclaim");

    private final String label;

    public static List<DocumentUploadCategory> existingApplicationCategories() {
        return Arrays.stream(values())
            .filter(DocumentUploadCategory::isExistingApplicationCategory)
            .toList();
    }

    public boolean isExistingApplicationCategory() {
        return this != MAIN_CLAIM_OR_COUNTERCLAIM && this != GENERAL_APPLICATION;
    }

    public String getLabel(String date) {
        return this == MAIN_CLAIM_OR_COUNTERCLAIM
            ? label
            : String.format(label, date);
    }
}
