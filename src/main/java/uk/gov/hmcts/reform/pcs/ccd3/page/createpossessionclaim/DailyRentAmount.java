package uk.gov.hmcts.reform.pcs.ccd3.page.createpossessionclaim;

import static uk.gov.hmcts.reform.pcs.ccd3.ShowConditions.NEVER_SHOW;
import uk.gov.hmcts.reform.pcs.ccd3.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd3.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd3.domain.PCSCase;

public class DailyRentAmount implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("dailyRentAmount")
                .pageLabel("Daily rent amount")
                .showCondition("rentFrequency!=\"OTHER\"")
                .readonly(PCSCase::getCalculatedDailyRentChargeAmount, NEVER_SHOW)
                .label("dailyRentAmount-content",
                        """
                                ---
                                <section tabindex="0">
                                    <p class="govuk-body">
                                        Based on your previous answers, the amount per day that unpaid
                                        rent should be charged at is:
                                        <span class="govuk-body govuk-!-font-weight-bold">
                                            ${calculatedDailyRentChargeAmount}
                                        </span>
                                    </p>
                                </section>
                                """)
                .mandatory(PCSCase::getRentPerDayCorrect)
                .mandatory(PCSCase::getAmendedDailyRentChargeAmount, "rentPerDayCorrect=\"NO\"");
    }
}
