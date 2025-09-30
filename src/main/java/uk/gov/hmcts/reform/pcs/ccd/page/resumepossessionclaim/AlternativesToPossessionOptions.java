package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

public class AlternativesToPossessionOptions implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("alternativesToPossession")
            .pageLabel("Alternatives to possession")
            .label("alternativesToPossession-info", """
                    ---
                    <p class="govuk-body govuk-!-margin-bottom-1" tabindex="0">
                      If a judge decides that possession is not reasonable at this time, they may instead decide
                      to order a demotion of tenancy (demotion order) or a suspension of the defendants’ right
                      to buy (suspension order), if they're not already in place.
                    </p>

                    <h2 class="govuk-heading-l govuk-!-margin-top-1" tabindex="0">Suspension of right to buy</h2>

                    <p class="govuk-body govuk-!-margin-bottom-1" tabindex="0">
                      A suspension order means that the defendants will not have a right to buy the premises
                      during the suspension.
                    </p>

                    <h2 class="govuk-heading-l govuk-!-margin-top-1" tabindex="0">Demotion of tenancy</h2>

                    <p class="govuk-body" tabindex="0">
                      A demotion order means that the defendants’ current tenancy will be replaced with a
                      demoted tenancy. During this period (usually 12 months) they will lose some rights they
                      currently have. The claimant is able to propose a new set of terms that will be put in
                      place by the demotion order.
                    </p>
                    """)
            .optional(PCSCase::getAlternativesToPossession);
    }
}
