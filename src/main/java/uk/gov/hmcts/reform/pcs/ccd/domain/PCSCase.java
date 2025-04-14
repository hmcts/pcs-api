package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;

import java.time.LocalDate;

/**
 * The CCD domain model representing a possessions case.
 */
@Builder
@Data
public class PCSCase {
    @CCD(label = "Description of this case")
    private String caseDescription;

    @CCD(label = "Hearing Date",
    typeOverride = FieldType.Date)
    private LocalDate hearingDate;

    @CCD(label = "Have documents been provided?")
    private YesOrNo documentsProvided;

    private HearingFee hearingFee;
}
