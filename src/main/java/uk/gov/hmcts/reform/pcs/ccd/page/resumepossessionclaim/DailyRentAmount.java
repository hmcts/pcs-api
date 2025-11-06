package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.LabelHolder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

public class DailyRentAmount implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("dailyRentAmount")
                .pageLabel("Daily rent amount")
                .readonly(PCSCase::getFormattedCalculatedDailyRentChargeAmount, NEVER_SHOW)
                .complex(PCSCase::getRentLabelHolder)
                .readonly(LabelHolder::getLabel1)
                .readonlyWithLabel(LabelHolder::getLabel2,
                    """
                        ---
                        Label 2:

                        Based on your previous answers, the amount per day that unpaid rent should be charged at is:
                        <span class="govuk-body govuk-!-font-weight-bold">
                        ${formattedCalculatedDailyRentChargeAmount}
                        </span>
                        """
                )
                .done()
                .readonly(PCSCase::getLabel3)
                .readonlyWithLabel(PCSCase::getLabel4,
                   """
                       ---
                       Label 4:

                       Based on your previous answers, the amount per day that unpaid rent should be charged at is:
                       <span class="govuk-body govuk-!-font-weight-bold">
                       ${formattedCalculatedDailyRentChargeAmount}
                       </span>
                       """
                )
                .label("dailyRentAmount-content",
                       """
                                ---
                                <section tabindex="0">
                                    <p class="govuk-body">
                                        Based on your previous answers, the amount per day that unpaid
                                        rent should be charged at is:
                                        <span class="govuk-body govuk-!-font-weight-bold">
                                            ${formattedCalculatedDailyRentChargeAmount}
                                        </span>
                                    </p>
                                </section>
                                """, "formattedCalculatedDailyRentChargeAmount=\"*\""
                )
                .mandatory(PCSCase::getRentPerDayCorrect)
                .mandatory(PCSCase::getAmendedDailyRentChargeAmount, "rentPerDayCorrect=\"NO\"");
    }
}
