package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

public class DailyRentAmount implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("dailyRentAmount")
                .pageLabel("Daily rent amount")
                .readonly(PCSCase::getCalculatedDailyRentChargeAmount, NEVER_SHOW)
                .label("dailyRentAmount-content",
                        """
                                ---
                                <section tabindex="0">
                                    <p class="govuk-body">
                                        Based on your previous answers, the amount per day that unpaid 
                                        rent should be charged at is: 
                                        <strong>${calculatedDailyRentChargeAmount}</strong>
                                    </p>
                                </section>
                                """)
                .mandatory(PCSCase::getRentPerDayCorrect)
                .mandatory(PCSCase::getAmendedDailyRentChargeAmount, "rentPerDayCorrect=\"NO\"")
                .label("settlement-section",
                        """
                                ---
                                <section tabindex="0">
                                    <p class="govuk-body">
                                        Enter the amount per day that unpaid rent should be charged at.
                                    </p>
                                </section>
                                """);
    }
}
