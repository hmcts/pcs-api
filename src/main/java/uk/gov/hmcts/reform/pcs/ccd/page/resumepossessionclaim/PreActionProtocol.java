package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

public class PreActionProtocol implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("preActionProtocol")
                .pageLabel("Pre-action protocol")
                .label("preActionProtocol-info",
                        """
                  ---
                  <section tabindex="0">
                    <p class="govuk-body">
                    Registered providers of social housing should follow the pre-action protocol
                    before making a housing possession claim. You should have:
                    </p>

                    <ul class="govuk-list govuk-list--bullet">
                        <li class="govuk-!-font-size-19">Contacted, or attempted to contact, the defendants.</li>
                        <li class="govuk-!-font-size-19">Tried to agree a repayment plan.</li>
                        <li class="govuk-!-font-size-19">Applied for arrears to be paid by the
                        Department for Work and Pensions (DWP) by deductions from the defendants' benefits.</li>
                        <li class="govuk-!-font-size-19">Offered to assist the defendants in a claim for
                        housing benefit or Universal Credit.</li>
                    </ul>

                    <div class="govuk-warning-text">
                        <span class="govuk-warning-text__icon" aria-hidden="true">!</span>
                        <strong class="govuk-warning-text__text">
                            <span class="govuk-visually-hidden">Warning</span>
                            Your case could be delayed or rejected if you have not followed
                            the pre-action protocol and completed all the steps.
                        </strong>
                    </div>
                  </section>

                  """)
                .mandatoryWithLabel(PCSCase::getPreActionProtocolCompleted,
                        "Have you followed the pre-action protocol?");
    }
}
