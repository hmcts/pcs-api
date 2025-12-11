package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsSection;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;

public class RentArrears implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("rentArrears")
                .showCondition("showRentSectionPage=\"Yes\" AND showRentArrearsPage=\"Yes\"")
                .pageLabel("Details of rent arrears")
                .readonly(PCSCase::getShowRentArrearsPage, NEVER_SHOW)

                .complex(PCSCase::getRentArrears)
                    // ---------- Rent statement guidance ----------
                    .label("rentArrears-rentStatement-separator", "---")
                        .label("rentArrears-rentStatement-heading",
                               """
                               <h2 class="govuk-heading-m govuk-!-margin-bottom-2">Rent statement</h2>
                               """
                        )
                        .label("rentArrears-rentStatement-help",
                               """
                               <section tabindex="0">
                                 <h3 class="govuk-heading-s govuk-!-margin-top-1 govuk-!-margin-bottom-1">
                                   Upload the rent statement
                                 </h3>
                                 <p class="govuk-body">The rent statement must show:</p>
                                 <ul class="govuk-list govuk-list--bullet">
                                   <li class="govuk-!-font-size-19">
                                     every date when a payment was supposed to be made
                                   </li>
                                   <li class="govuk-!-font-size-19">the amount that was due on each of those dates</li>
                                   <li class="govuk-!-font-size-19">the actual payments that were made,
                                   and when they were made</li>
                                   <li class="govuk-!-font-size-19">the total rent arrears</li>
                                 </ul>

                                 <p class="govuk-body">It must cover the time period of either:</p>
                                 <ul class="govuk-list govuk-list--bullet">
                                   <li class="govuk-!-font-size-19">from the first date the defendants
                                   missed a payment, or</li>
                                   <li class="govuk-!-font-size-19">the last two years of payments, if the first date
                                   of their missed payment was more than two years ago</li>
                                 </ul>
                               </section>
                               """)
                    .mandatory(RentArrearsSection::getStatementDocuments)

                    // ---------- Total arrears ----------
                    .label("rentArrears-totalArrears-separator", "---")
                    .label("rentArrears-totalArrears-heading",
                            """
                            <h2 class="govuk-heading-m govuk-!-margin-bottom-0">Rent arrears</h2>
                            <h3 class="govuk-heading-s govuk-!-margin-top-0 govuk-!-margin-bottom-0">
                            How much are the total rent arrears as shown on the rent statement?</h3>
                            """)
                    .mandatory(RentArrearsSection::getTotal)

                    // ---------- Third-party payments ----------
                    .label("rentArrears-thirdPartyPayments-separator", "---")
                    .mandatory(RentArrearsSection::getThirdPartyPayments)

                    .mandatory(RentArrearsSection::getThirdPartyPaymentSources,
                            "rentArrears_ThirdPartyPayments=\"YES\"")

                    // "Other" free text is mandatory when OTHER is selected
                    .mandatory(RentArrearsSection::getThirdPartyPaymentSourceOther,
                            "rentArrears_ThirdPartyPayments=\"YES\" "
                            + "AND rentArrears_ThirdPartyPaymentSources CONTAINS \"OTHER\"")
                .done()
                .label("rentArrears-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }
}
