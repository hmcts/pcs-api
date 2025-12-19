package uk.gov.hmcts.reform.pcs.ccd.domain;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;

import java.util.Set;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.MultiSelectList;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class RentArrearsGroundsForPossession {

    @CCD(
        typeOverride = MultiSelectList,
        typeParameterOverride = "RentArrearsGround"
    )
    private Set<RentArrearsGround> copyOfGrounds;

    // Rent arrears grounds checkboxes
    @CCD(
        label = "What are your grounds for possession?",
        hint = "Select all that apply",
        typeOverride = MultiSelectList,
        typeParameterOverride = "RentArrearsGround"
    )
    private Set<RentArrearsGround> grounds;

    @CCD(
        label = "Do you have any other additional grounds for possession?"
    )
    private YesOrNo hasOtherAdditionalGrounds;

}
