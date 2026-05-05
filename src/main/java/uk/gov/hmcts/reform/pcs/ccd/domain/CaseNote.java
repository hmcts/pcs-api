package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;

import java.time.LocalDateTime;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaseNote {

    @CCD
    private String createdBy;

    @CCD
    private LocalDateTime createdOn;

    @CCD(
        label = "Note",
        typeOverride = TextArea
    )
    private String note;
}
