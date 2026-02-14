package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.AssuredRentArrearsPossessionGrounds;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;

/**
 * Page for selecting rent arrears grounds for possession.
 */
@Component
public class RentArrearsGroundsForPossessionPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("groundForPossessionRentArrears")
                .pageLabel("Grounds for possession")
                .showCondition("claimDueToRentArrears=\"Yes\""
                               +  " AND tenancy_TypeOfTenancyLicence=\"ASSURED_TENANCY\""
                               + " AND legislativeCountry=\"England\"")
                .complex(PCSCase::getAssuredRentArrearsPossessionGrounds)
                .label("groundForPossessionRentArrears-info", """
                ---
                <p class="govuk-body">You may have already given the defendants notice of your intention to begin
                    possession proceedings and written which grounds you’re making the possession claim under.
                     You should select these grounds here.</p>
                <h2 class="govuk-!-font-size-19 govuk-!-margin-bottom-1">Serious rent arrears (ground 8)</h2>
                <p class="govuk-body">Mandatory ground. Can be used if the defendants owe at least:</p>
                <ul class="govuk-list govuk-list--bullet">
                    <li class="govuk-list govuk-!-font-size-19 govuk-!-margin-0">eight weeks’ rent if they pay
                        weekly or fortnightly</li>
                    <li class="govuk-list govuk-!-font-size-19 govuk-!-margin-0">two months’ rent if they pay
                        monthly</li>
                    <li class="govuk-list govuk-!-font-size-19 govuk-!-margin-0">three months’ rent if they pay
                        quarterly or yearly</li>
                </ul>
                <p class="govuk-body">The defendants must owe the rent when the notice is served and at the time of the
                    hearing.</p>
                <h2 class="govuk-!-font-size-19 govuk-!-margin-bottom-1">Rent arrears (ground 10)</h2>
                <p class="govuk-body">Discretionary ground. The defendants are in any amount of arrears.</p>
                <h2 class="govuk-!-font-size-19 govuk-!-margin-bottom-1">Persistent delay in paying rent
                    (ground 11)</h2>
                <p class="govuk-body">Discretionary ground. The defendants have persistently delayed paying their
                    rent.</p>
                """)
                .mandatory(AssuredRentArrearsPossessionGrounds::getRentArrearsGrounds)
                .done()
                .mandatory(PCSCase::getHasOtherAdditionalGrounds)
                .label("groundForPossessionRentArrears-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

}
