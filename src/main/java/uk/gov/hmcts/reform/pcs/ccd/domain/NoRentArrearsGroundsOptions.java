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
public class NoRentArrearsGroundsOptions {

    @CCD(
        label = "Mandatory grounds",
        hint = "Select all that apply",
        typeOverride = MultiSelectList,
        typeParameterOverride = "AssuredMandatoryGrounds"
    )
    private Set<AssuredMandatoryGrounds> mandatoryGrounds;

    @CCD(
        label = "Discretionary grounds",
        hint = "Select all that apply",
        typeOverride = MultiSelectList,
        typeParameterOverride = "AssuredDiscretionaryGrounds"
    )
    private Set<AssuredDiscretionaryGrounds> discretionaryGrounds;
    private YesOrNo showGroundReasonPage;

}
