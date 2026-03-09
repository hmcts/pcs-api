package uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common;

import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.FieldType;

import java.time.LocalDate;

public class CapturingDob {

    @CCD(label = """
                ---
               """, typeOverride = FieldType.Label)
    private String sectionLabel;

    @CCD(
        label = "Dates that I cannot attend",
        hint = "For example, 01 02 2025"
    )
    private LocalDate dateCannotAttend;
}
