package uk.gov.hmcts.reform.pcs.ccd.domain.legalrepdocumentupload;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum DocumentUploadCategory implements HasLabel {

    ADJOURN_HEARING_APPLICATION(
        "Yes, the documents I'm uploading relate to the application to adjourn the hearing - submitted on "
            + "Monday 1 Feb 2026"),
    SUSPEND_EVICTION_APPLICATION(
        "Yes, the documents I'm uploading relate to an application to suspend the eviction - submitted on "
            + "Tuesday 2 April 2026"),
    SET_ASIDE_ORDER_APPLICATION(
        "Yes, the documents I'm uploading relate to an application to set aside the order - submitted on "
            + "Tuesday 2 April 2026"),
    GENERAL_APPLICATION(
        "Yes, the documents I'm uploading relate to an application submitted on Tuesday 2 April 2026"),
    MAIN_CLAIM_OR_COUNTERCLAIM(
        "No, the documents I'm uploading relate to the main claim or counterclaim");

    private final String label;
}
