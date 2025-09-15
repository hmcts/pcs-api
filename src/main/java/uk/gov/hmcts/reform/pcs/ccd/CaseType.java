package uk.gov.hmcts.reform.pcs.ccd;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import static java.lang.System.getenv;
import static java.util.Optional.ofNullable;
import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;
import static uk.gov.hmcts.reform.pcs.ccd.domain.State.AWAITING_FURTHER_CLAIM_DETAILS;

/**
 * Setup some common possessions case type configuration.
 */
@Component
public class CaseType implements CCDConfig<PCSCase, State, UserRole> {

    private static final String CASE_TYPE_ID = "PCS";
    private static final String CASE_TYPE_NAME = "Civil Possessions";
    private static final String CASE_TYPE_DESCRIPTION = "Civil Possessions Case Type";
    private static final String JURISDICTION_ID = "PCS";
    private static final String JURISDICTION_NAME = "Possessions";
    private static final String JURISDICTION_DESCRIPTION = "Possessions Jurisdiction";

    public static String getCaseType() {
        return withChangeId(CASE_TYPE_ID, "-");
    }

    public static String getCaseTypeName() {
        return withChangeId(CASE_TYPE_NAME, " ");
    }

    private static String withChangeId(String base, String separator) {
        return ofNullable(getenv().get("CHANGE_ID"))
            .map(changeId -> base + separator + changeId)
            .orElse(base);
    }

    @Override
    public void configure(final ConfigBuilder<PCSCase, State, UserRole> builder) {
        builder.setCallbackHost(getenv().getOrDefault("CASE_API_URL", "http://localhost:3206"));

        builder.decentralisedCaseType(getCaseType(), getCaseTypeName(), CASE_TYPE_DESCRIPTION);
        builder.jurisdiction(JURISDICTION_ID, JURISDICTION_NAME, JURISDICTION_DESCRIPTION);

        String paymentLabel = "Payment Status";

        builder.searchInputFields()
            .caseReferenceField()
            .field(PCSCase::getPaymentStatus, paymentLabel);

        builder.searchCasesFields()
            .caseReferenceField()
            .field(PCSCase::getPaymentStatus, paymentLabel);

        builder.searchResultFields()
            .caseReferenceField()
            .field(PCSCase::getPaymentStatus, paymentLabel);

        builder.workBasketInputFields()
            .caseReferenceField()
            .field(PCSCase::getClaimantName, "Claimant Name");

        builder.workBasketResultFields()
            .caseReferenceField()
            .field(PCSCase::getPropertyAddress, "Property Address");

        builder.tab("nextSteps", "Next steps")
            .showCondition(ShowConditions.stateEquals(AWAITING_FURTHER_CLAIM_DETAILS))
            .label("nextStepsMarkdownLabel", null, "${nextStepsMarkdown}")
            .field("nextStepsMarkdown", NEVER_SHOW);

        builder.tab("summary", "Property Details")
            .showCondition(ShowConditions.stateNotEquals(AWAITING_FURTHER_CLAIM_DETAILS))
            .field(PCSCase::getPropertyAddress);

        builder.tab("CaseHistory", "History")
            .showCondition(ShowConditions.stateNotEquals(AWAITING_FURTHER_CLAIM_DETAILS))
            .field("caseHistory");

        builder.tab("ClaimPayment", "Payment")
            .showCondition(ShowConditions.stateNotEquals(AWAITING_FURTHER_CLAIM_DETAILS))
            .showCondition("claimPaymentTabMarkdown!=\"\"")
            .label("claimPaymentTabMarkdownLabel", null, "${claimPaymentTabMarkdown}")
            .field("claimPaymentTabMarkdown", NEVER_SHOW);

        builder.tab("defendantDetails", "Defendant Details")
            .showCondition("defendantsSummaryMarkdown!=\"\"")
            .label("defendantsSummaryMarkdownLabel", null, "${defendantsSummaryMarkdown}")
            .field("defendantsSummaryMarkdown", NEVER_SHOW)
            .label("button-label", "", """
                <br>
               <br>
                <a href="/cases/case-details/${[CASE_REFERENCE]}/trigger/addDefendants" role="button" class="govuk-button">
                  Add Defendant
              </a>
                """);

        builder.tab("hidden", "HiddenFields")
            .showCondition(NEVER_SHOW)
            .field(PCSCase::getPageHeadingMarkdown);
    }
}
