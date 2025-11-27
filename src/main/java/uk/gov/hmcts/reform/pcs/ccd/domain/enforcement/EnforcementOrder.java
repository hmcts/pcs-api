package uk.gov.hmcts.reform.pcs.ccd.domain.enforcement;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import java.util.Set;
/**
 * The main domain model representing an enforcement order.
 */

@Builder
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
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
    private YesNoNotSure vulnerablePeoplePresent;

    private VulnerableAdultsChildren vulnerableAdultsChildren;

    @CCD(
        label = "Which language did you use to complete this service?",
        hint = "If someone else helped you to answer a question in this service, "
            + "ask them if they answered any questions in Welsh. We’ll use this to "
            + "make sure your claim is processed correctly"
    )
    private LanguageUsed enforcementLanguageUsed;

    @JsonUnwrapped
    @CCD
    private PropertyAccessDetails propertyAccessDetails;

    @JsonUnwrapped
    @CCD
    private LegalCosts legalCosts;
}
