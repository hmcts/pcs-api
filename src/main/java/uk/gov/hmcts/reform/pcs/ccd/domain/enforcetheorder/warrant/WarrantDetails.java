package uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicMultiSelectStringList;
import java.util.Set;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.DynamicMultiSelectList;

@Builder
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WarrantDetails {

    @JsonUnwrapped(prefix = "warrant")
    @CCD
    private AdditionalInformation additionalInformation;

    @JsonUnwrapped(prefix = "warrant")
    @CCD
    private NameAndAddressForEviction nameAndAddressForEviction;

    @JsonUnwrapped(prefix = "warrant")
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
    private VerticalYesNo showChangeNameAddressPage;

    @CCD(
        searchable = false
    )
    private VerticalYesNo showPeopleWhoWillBeEvictedPage;

    @CCD(
        searchable = false
    )
    private VerticalYesNo showPeopleYouWantToEvictPage;

    @JsonUnwrapped(prefix = "warrant")
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

    @JsonUnwrapped(prefix = "warrant")
    @CCD
    private PropertyAccessDetails propertyAccessDetails;

    @JsonUnwrapped(prefix = "warrant")
    @CCD
    private LegalCosts legalCosts;

    @JsonUnwrapped(prefix = "warrant")
    @CCD
    private MoneyOwedByDefendants moneyOwedByDefendants;

    @JsonUnwrapped(prefix = "warrant")
    @CCD
    private LandRegistryFees landRegistryFees;

    @JsonUnwrapped(prefix = "repayment")
    @CCD
    private RepaymentCosts repaymentCosts;

    @CCD
    @JsonUnwrapped(prefix = "warrant")
    private DefendantsDOB defendantsDOB;
}
