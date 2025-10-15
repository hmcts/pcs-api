package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.api.ComplexType;
import uk.gov.hmcts.ccd.sdk.type.DynamicMultiSelectList;

import java.time.LocalDate;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@ComplexType(name="DefendantDOBDynamicDetails", generate = true)
public class DefendantDOBDynamicDetails {

    @CCD(
        label = "Defendant’s name"
    )
    private String name;

    @CCD(
        label = "What is this Defendant's date of birth?",
        hint = "For example, 16 4 2021"
    )
    private LocalDate dob;
}

