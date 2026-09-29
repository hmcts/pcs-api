package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;

import java.time.LocalDate;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDate {

    @CCD(ignore = true)
    public static final String DESCRIPTION_LABEL = "Description of review";

    @CCD(
        label = "Date of review",
        hint = "For example 16 4 2021"
    )
    private LocalDate date;

    @CCD(label = "Reason")
    private ReviewReason reason;

    @CCD(
        label = DESCRIPTION_LABEL,
        hint = "You can enter up to 500 characters",
        typeOverride = TextArea
    )
    private String description;
}
