package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;

public class DailyRentAmount implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("dailyRentAmount")
                .pageLabel("Daily rent amount")
                .showCondition("groundsForPossession=\"Yes\" AND rentFrequency!=\"OTHER\""
                                + " OR showRentDetailsPage=\"Yes\"")
                .readonly(PCSCase::getFormattedCalculatedDailyRentChargeAmount, NEVER_SHOW)
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
                                """)
                .mandatory(PCSCase::getRentPerDayCorrect)
                .mandatory(PCSCase::getAmendedDailyRentChargeAmount, "rentPerDayCorrect=\"NO\"")
                .label("dailyRentAmount-save-and-return", SAVE_AND_RETURN);
    }
}
