package uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicMultiSelectStringList;
import java.util.Set;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.DynamicMultiSelectList;

@Builder
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
@NoArgsConstructor
@AllArgsConstructor
public class WarrantDetails {

    @JsonUnwrapped
    @CCD
    private AdditionalInformation additionalInformation;

    @JsonUnwrapped
    @CCD
    private NameAndAddressForEviction nameAndAddressForEviction;

    @JsonUnwrapped
    @CCD
    private PeopleToEvict peopleToEvict;

    @CCD(
        label = "Who do you want to evict?",
        typeOverride = DynamicMultiSelectList
    )
    private DynamicMultiSelectStringList selectedDefendants;

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

    @CCD(
        searchable = false
    )
    private YesOrNo showChangeNameAddressPage;

    @CCD(
        searchable = false
    )
    private YesOrNo showPeopleWhoWillBeEvictedPage;

    @CCD(
        searchable = false
    )
    private YesOrNo showPeopleYouWantToEvictPage;

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

    @JsonUnwrapped
    @CCD
    private MoneyOwedByDefendants moneyOwedByDefendants;

    @JsonUnwrapped
    @CCD
    private LandRegistryFees landRegistryFees;

    @JsonUnwrapped
    @CCD
    private RepaymentCosts repaymentCosts;

    @CCD(
            label = "Is your order a suspended order?",
            hint = "If your order is suspended, you will see a different version of the statement of truth on the "
                    + "next page. If you do not know if your order is suspended: save your application as a draft, "
                    + "return to the case summary page, and then check the tab named ‘Case File View’"
    )
    private VerticalYesNo isSuspendedOrder;
}
