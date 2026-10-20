package uk.gov.hmcts.reform.pcs.enforcement.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea;

/**
 * The time is a pre-formatted string because the case view has no date and time format matching the
 * "1 Feb 2026 16:20pm" layout the bailiffs use.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BailiffNote {

    @CCD(label = "Note", typeOverride = TextArea)
    private String note;

    @CCD(label = "Added by")
    private String addedBy;

    @CCD(label = "Date and time")
    private String addedOn;
}
