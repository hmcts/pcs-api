package uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.writ;

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
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.LandRegistryFees;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.LegalCosts;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.MoneyOwedByDefendants;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.RepaymentCosts;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.StatementOfTruthDetailsEnforcement;

@Builder
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
@NoArgsConstructor
@AllArgsConstructor
public class WritDetails {

    @JsonUnwrapped
    @CCD
    private NameAndAddressForEviction nameAndAddressForEviction;

    @CCD (
        label = "Is the defendant currently in a breathing space?"
    )
    private YesNoNotSure defendantInBreathingSpace;

    @CCD(
        label = "Have the defendants missed any payments?",
        hint = "These are payments outlined in the suspended order for possession.  "
            + "They were ordered to make these payments by the judge."
    )
    private VerticalYesNo defendantMissedPayments;

    @CCD(searchable = false)
    private YesOrNo showChangeNameAddressPage;

    @CCD(
        label = "Have you hired a High Court enforcement officer?"
    )
    private VerticalYesNo hasHiredHighCourtEnforcementOfficer;

    @CCD(
        label = "Name of your High Court enforcement officer",
        hint = "If you do not know their name, use the name of the organisation they work for",
        max = 120
    )
    private String hceoDetails;

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
        label = "Which language did you use to complete this service?",
        hint = "If someone else helped you to answer a question in this service, "
            + "ask them if they answered any questions in Welsh. We’ll use this to "
            + "make sure your claim is processed correctly"
    )
    @JsonProperty("EnforcementLanguageUsed")
    private LanguageUsed languageUsed;

    @CCD(
        searchable = false,
        label = "TEMPORARY TEST ONLY – Has the claim been transferred to the High Court?"
    )
    private YesOrNo hasClaimTransferredToHighCourt;

    @CCD(
        searchable = false,
        label = "TEMPORARY TEST ONLY – Was the general application to transfer to the High Court successful?"
    )
    private YesOrNo wasGeneralApplicationToTransferToHighCourtSuccessful;

    @JsonUnwrapped
    @CCD
    private StatementOfTruthDetailsEnforcement statementOfTruth;
}
