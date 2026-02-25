package uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.writ;

import com.fasterxml.jackson.annotation.JsonInclude;
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

    @CCD(searchable = false)
    private YesOrNo showChangeNameAddressPage;

    @CCD(searchable = false)
    private YesOrNo showPeopleWhoWillBeEvictedPage;

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
    private LanguageUsed enforcementLanguageUsed;
    @CCD(
        searchable = false,
        label = "Has the claim been transferred to the High Court?"
    )
    private YesOrNo hasClaimTransferredToHighCourt;

    @JsonUnwrapped
    @CCD
    private StatementOfTruthDetailsEnforcement statementOfTruth;
}
