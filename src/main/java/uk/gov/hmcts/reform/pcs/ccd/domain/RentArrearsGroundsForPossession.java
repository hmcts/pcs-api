package uk.gov.hmcts.reform.pcs.ccd.domain;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;

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
    private Set<RentArrearsGround> copyOfRentArrearsGrounds;

    @CCD(
        label = "What are your grounds for possession?",
        hint = "Select all that apply",
        typeOverride = MultiSelectList,
        typeParameterOverride = "RentArrearsGround"
    )
    private Set<RentArrearsGround> rentArrearsGrounds;

    @CCD(
        label = "Mandatory grounds",
        hint = "Select all that apply",
        typeOverride = MultiSelectList,
        typeParameterOverride = "RentArrearsMandatoryGrounds"
    )
    private Set<RentArrearsMandatoryGrounds> mandatoryGrounds;

    // Additional grounds checkboxes - Discretionary
    @CCD(
        label = "Discretionary grounds",
        hint = "Select all that apply",
        typeOverride = MultiSelectList,
        typeParameterOverride = "RentArrearsDiscretionaryGrounds"
    )
    private Set<RentArrearsDiscretionaryGrounds> discretionaryGrounds;
}

