package uk.gov.hmcts.reform.pcs.enforcement.ccd.page.decideenforcement;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementDecision;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;
import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.fieldEquals;

public class DecideEnforcementPage implements CcdPageConfiguration {

    public static final String PAGE_ID = "decideEnforcement";
    public static final String REJECTED = fieldEquals("enforcementDecision", EnforcementDecision.REJECT);

    static final String CASE_DETAILS = """
        <dl class="govuk-summary-list">
          <div class="govuk-summary-list__row">
            <dt class="govuk-summary-list__key">Case number</dt>
            <dd class="govuk-summary-list__value">${[CASE_REFERENCE]}</dd>
          </div>
          <div class="govuk-summary-list__row">
            <dt class="govuk-summary-list__key">Property address</dt>
            <dd class="govuk-summary-list__value">${formattedPropertyAddress}</dd>
          </div>
          <div class="govuk-summary-list__row">
            <dt class="govuk-summary-list__key">Parties</dt>
            <dd class="govuk-summary-list__value">${formattedCaseName}</dd>
          </div>
        </dl>
        """;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page(PAGE_ID)
            .pageLabel("Decide enforcement")
            .complex(PCSCase::getEnforcementOrder)
            // XUI fills a label's ${...} only from fields placed before it, so these must precede the label
            .readonly(EnforcementOrder::getFormattedPropertyAddress, NEVER_SHOW, true)
            .readonly(EnforcementOrder::getFormattedCaseName, NEVER_SHOW, true)
            .label(PAGE_ID + "-caseDetails", CASE_DETAILS, null, true)
            .mandatory(EnforcementOrder::getEnforcementDecision)
            .mandatory(EnforcementOrder::getEnforcementRejectionReason, REJECTED)
            .done();
    }
}
