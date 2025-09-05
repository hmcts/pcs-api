package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

public class DailyRentAmount implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("dailyRentAmount", this::midEvent)
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

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
            CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();
        List<String> errors = new ArrayList<>();

        if (caseData.getRentPerDayCorrect() == VerticalYesNo.NO) {
            BigDecimal amendedAmountInPence = new BigDecimal(caseData.getAmendedDailyRentChargeAmount());
            if (amendedAmountInPence.compareTo(BigDecimal.ZERO) < 0) {
                errors.add("Amended daily rent amount cannot be negative");
            }
        }

        if (!errors.isEmpty()) {
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                    .errors(errors)
                    .build();
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data(caseData)
                .build();
    }
}
