package uk.gov.hmcts.reform.pcs.ccd.domain.grounds;

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
public class SecureOrFlexiblePossessionGrounds {

    @CCD(
        label = "Discretionary grounds",
        hint = "Select all that apply",
        typeOverride = FieldType.MultiSelectList,
        typeParameterOverride = "SecureOrFlexibleDiscretionaryGrounds"
    )
    private Set<SecureOrFlexibleDiscretionaryGrounds> secureOrFlexibleDiscretionaryGrounds;

    @CCD(
        label = "Mandatory grounds",
        hint = "Select all that apply",
        typeOverride = FieldType.MultiSelectList,
        typeParameterOverride = "SecureOrFlexibleMandatoryGrounds"
    )
    private Set<SecureOrFlexibleMandatoryGrounds> secureOrFlexibleMandatoryGrounds;

    @CCD(
        label = "Discretionary grounds (if alternative accommodation available)",
        hint = "Select all that apply",
        typeOverride = FieldType.MultiSelectList,
        typeParameterOverride = "SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm"
    )
    private Set<SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm> secureOrFlexibleDiscretionaryGroundsAlt;

    @CCD(
        label = "Mandatory grounds (if alternative accommodation available)",
        hint = "Select all that apply",
        typeOverride = FieldType.MultiSelectList,
        typeParameterOverride = "SecureOrFlexibleMandatoryGroundsAlternativeAccomm"
    )
    private Set<SecureOrFlexibleMandatoryGroundsAlternativeAccomm> secureOrFlexibleMandatoryGroundsAlt;
}
