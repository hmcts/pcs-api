package uk.gov.hmcts.reform.pcs.ccd.domain.enforcement;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import java.util.Set;
/**
 * The main domain model representing an enforcement order.
 */

@Builder
@Data
public class EnforcementOrder {

    @CCD(
        label = "What do you want to apply for?"
    )
    private SelectEnforcementType selectEnforcementType;

    @JsonUnwrapped
    @CCD
    private AdditionalInformation additionalInformation;

    @JsonUnwrapped
    private NameAndAddressForEviction nameAndAddressForEviction;

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
    private Set<RiskCategory> enforcementRiskCategories;

    @JsonUnwrapped
    @CCD(
        label = "Risk details"
    )
    private EnforcementRiskDetails riskDetails;

    @CCD(
        label = "Is anyone living at the property vulnerable?"
    )
    private YesNoNotSure vulnerablePeopleYesNo;
    
    private VulnerableAdultsChildren vulnerableAdultsChildren;
    
    @JsonUnwrapped
    @CCD
    private PropertyAccessDetails propertyAccessDetails;
}
