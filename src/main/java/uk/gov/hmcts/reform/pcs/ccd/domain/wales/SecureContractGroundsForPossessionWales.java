package uk.gov.hmcts.reform.pcs.ccd.domain.wales;

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
public class SecureContractGroundsForPossessionWales {
    @CCD(
        label = "Discretionary grounds",
        hint = "Select all that apply",
        typeOverride = FieldType.MultiSelectList,
        typeParameterOverride = "SecureContractDiscretionaryGroundsWales"
    )
    private Set<SecureContractDiscretionaryGroundsWales> secureContractDiscretionaryGroundsWales;

    @CCD(
        label = "Mandatory grounds",
        hint = "Select all that apply",
        typeOverride = FieldType.MultiSelectList,
        typeParameterOverride = "SecureContractMandatoryGroundsWales"
    )
    private Set<SecureContractMandatoryGroundsWales> secureContractMandatoryGroundsWales;

    @CCD(
        label = "Estate management grounds",
        typeOverride = FieldType.MultiSelectList,
        typeParameterOverride = "EstateManagementGroundsWales"
    )
    private Set<EstateManagementGroundsWales> secureContractEstateManagementGroundsWales;
}
