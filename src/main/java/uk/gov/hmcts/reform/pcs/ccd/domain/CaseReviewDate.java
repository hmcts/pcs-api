package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaseReviewDate {

    @CCD(label = "Created by")
    private String createdBy;

    @CCD(label = "Created on")
    private LocalDateTime createdOn;

    @CCD(label = "Date of review")
    private LocalDate date;

    @CCD(label = "Reason")
    private ReviewReason reason;

    @CCD(
        label = ReviewDate.DESCRIPTION_LABEL,
        typeOverride = TextArea
    )
    private String description;
}
