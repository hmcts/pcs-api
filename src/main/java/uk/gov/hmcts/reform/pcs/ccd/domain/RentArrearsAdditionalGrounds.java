package uk.gov.hmcts.reform.pcs.ccd.domain;

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
public class RentArrearsAdditionalGrounds {

    @CCD(
        label = "Mandatory grounds",
        hint = "Select all that apply",
        typeOverride = MultiSelectList,
        typeParameterOverride = "RentArrearsMandatoryGrounds"
    )

    private Set<RentArrearsMandatoryGrounds> mandatoryGrounds;

    @CCD(
        label = "Discretionary grounds",
        hint = "Select all that apply",
        typeOverride = MultiSelectList,
        typeParameterOverride = "RentArrearsDiscretionaryGrounds"
    )

    private Set<RentArrearsDiscretionaryGrounds> discretionaryGrounds;

    @CCD(
        label = "Mandatory grounds",
        hint = "Select all that apply",
        typeOverride = MultiSelectList,
        typeParameterOverride = "AssuredAdditionalMandatoryGrounds"
    )
    private Set<AssuredAdditionalMandatoryGrounds> assuredAdditionalMandatoryGrounds;

    @CCD(
        label = "Discretionary grounds",
        hint = "Select all that apply",
        typeOverride = MultiSelectList,
        typeParameterOverride = "AssuredAdditionalDiscretionaryGrounds"
    )
    private Set<AssuredAdditionalDiscretionaryGrounds> assuredAdditionalDiscretionaryGrounds;

}
