package uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrantofrestitution;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.EnforcementRiskDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.PropertyAccessDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.RiskCategory;

import java.util.Set;

@Builder
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
@NoArgsConstructor
@AllArgsConstructor
public class WarrantRestDetails {

    @CCD(
            label = "Does anyone living at the property pose a risk to the bailiff?"
    )
    private YesNoNotSure anyRiskToBailiff;

    @CCD(
            label = "What kind of risks do they pose to the bailiff?",
            hint = "Include any risks posed by the defendants and also anyone else living at the property",
            typeOverride = FieldType.MultiSelectList,
            typeParameterOverride = "RiskCategory"
    )
    @JsonProperty("EnforcementRiskCategories")
    private Set<RiskCategory> riskCategories;

    @JsonUnwrapped
    @CCD(
            label = "Risk details"
    )
    private EnforcementRiskDetails riskDetails;

    @JsonUnwrapped
    @CCD
    private PropertyAccessDetails propertyAccessDetails;
}
