package uk.gov.hmcts.reform.pcs.ccd.domain.wales;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.FieldType;

import java.util.Set;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class SecureContractGroundsForPossessionWales {
    @CCD(
        label = "Discretionary grounds",
        hint = "Select all that apply",
        typeOverride = FieldType.MultiSelectList,
        typeParameterOverride = "SecureContractDiscretionaryGroundsWales"
    )
    private Set<SecureContractDiscretionaryGroundsWales> discretionaryGrounds;

    @CCD(
        label = "Mandatory grounds",
        hint = "Select all that apply",
        typeOverride = FieldType.MultiSelectList,
        typeParameterOverride = "SecureContractMandatoryGroundsWales"
    )
    private Set<SecureContractMandatoryGroundsWales> mandatoryGrounds;

    @CCD(
        label = "Estate management grounds",
        typeOverride = FieldType.MultiSelectList,
        typeParameterOverride = "EstateManagementGroundsWales"
    )
    private Set<EstateManagementGroundsWales> estateManagementGrounds;
}
