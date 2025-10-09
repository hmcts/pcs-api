package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.FieldType;

import java.time.LocalDate;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DefendantDOBDetails {

    @CCD(label = "Defendant's first name")
    private String firstName;

    @CCD(label = "Defendant's last name")
    private String lastName;

    @CCD(
        label = "What is this Defendant's date of birth?",
        hint = "For example, 16 4 2021"
    )
    private LocalDate dob;
}

