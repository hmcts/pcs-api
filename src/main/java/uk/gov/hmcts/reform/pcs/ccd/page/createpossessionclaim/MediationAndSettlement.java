package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

/**
 * Page configuration for the Mediation and Settlement section.
 * Allows claimants to indicate whether they're willing to try mediation or settlement
 * with optional additional information fields that appear conditionally.
 */
public class MediationAndSettlement implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("mediationAndSettlement")
                .pageLabel("Mediation and settlement")
                .label("mediationAndSettlement-content",
                        """
                        ---
                        <section tabindex="0">
                            <p class="govuk-body">
                                Mediation is when an impartial professional (the mediator) helps both sides work out \
                                an agreement.
                            </p>
                        </section>
                        """)
                .mandatory(PCSCase::getMediationAttempted)
                .mandatory(PCSCase::getMediationAttemptedDetails)
                .label("settlement-section",
                        """
                        ---
                        <section tabindex="0">
                            <p class="govuk-body">
                                If your claim is on the grounds of rent arrears, this includes any steps you've taken \
                                to recover the arrears or to agree a repayment plan.
                            </p>
                        </section>
                        """)
                .mandatory(PCSCase::getSettlementAttempted)
                .mandatory(PCSCase::getSettlementAttemptedDetails);
    }
}
