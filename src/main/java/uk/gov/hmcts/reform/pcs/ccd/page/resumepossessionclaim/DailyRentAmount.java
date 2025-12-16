package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;

public class DailyRentAmount implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("dailyRentAmount", this::midEvent)
                .pageLabel("Daily rent amount")
                .readonly(PCSCase::getRentSectionPaymentFrequency, NEVER_SHOW)
                .showCondition("showRentSectionPage=\"Yes\" AND rentSectionPaymentFrequency!=\"OTHER\"")
                .complex(PCSCase::getRentSection)
                    .readonly(RentDetails::getFormattedCalculatedDailyCharge, NEVER_SHOW)
                    .label("dailyRentAmount-content",
                            """
                                    ---
                                    <section tabindex="0">
                                        <p class="govuk-body">
                                            Based on your previous answers, the amount per day that unpaid
                                            rent should be charged at is:
                                            <span class="govuk-body govuk-!-font-weight-bold">
                                                ${rentDetails_FormattedCalculatedDailyCharge}
                                            </span>
                                        </p>
                                    </section>
                                    """)
                    .mandatory(RentDetails::getPerDayCorrect)
                    .mandatory(RentDetails::getAmendedDailyCharge, "rentDetails_PerDayCorrect=\"NO\"")
                .done()
                .label("dailyRentAmount-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                    CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();

        RentDetails rentDetails = caseData.getRentSection();
        // When user answers Yes/No on DailyRentAmount, set flag to show RentArrears
        if (rentDetails != null && rentDetails.getPerDayCorrect() != null) {
            caseData.setShowRentArrearsPage(YesOrNo.YES);
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data(caseData)
                .build();
    }
}
