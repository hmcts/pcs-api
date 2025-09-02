package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

/**
 * Page for selecting rent arrears grounds for possession.
 */
public class GroundForPossessionRentArrears implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("groundForPossessionRentArrears")
                .pageLabel("Grounds for possession")
                .showCondition("groundsForPossession=\"YES\"")
                .label("groundForPossessionRentArrears-info", """
                ---
                <p class="govuk-body">You may have already given the defendants notice of your intention to begin possession proceedings.
                    If you have, you should have written the grounds you're making your claim under. ]
                    You should select these grounds here.</p>
                <h3 class="govuk-heading-m  govuk-!-margin-bottom-1">Serious rent arrears(ground 8)</h3>
                <p class="govuk-body">Mandatory ground. Can be used if the defendants owe at least:</p>
                <ul class="govuk-list govuk-list--bullet">
                    <li class="govuk-list govuk-!-font-size-19">eight weeks' rent if they pay weekly or forthnightly</li>
                    <li class="govuk-list govuk-!-font-size-19">two months' rent if they pay monthly</li>
                    <li class="govuk-list govuk-!-font-size-19">three months' rent if they pay quarterly or yearly</li>
                </ul>
                <p class="govuk-body">The defendants must owe the rent when the notice is served and the time of the hearing.</p>
                <h3 class="govuk-heading-m  govuk-!-margin-bottom-1">Rent arrears(ground 10)</h3>
                <p class="govuk-body">Discretionary ground. The defendants are in any amount of arrears.</p>
                <h3 class="govuk-heading-m  govuk-!-margin-bottom-1">Persistent delay in paying rent (ground 11)</h3>
                <p class="govuk-body">Discretionary ground. The defendants have persistently delayed paying their rent.</p>
                """)
                .mandatory(PCSCase::getRentArrearsGrounds)
                .mandatory(PCSCase::getHasOtherGrounds)
                .done();
    }
}
