package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.IntroductoryDemotedOrOtherGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

public class IntroductoryDemotedOrOtherGroundsForPossession implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("introductoryDemotedOrOtherGroundsForPossession", this::midEvent)
            .pageLabel("Grounds for possession")
            .showCondition(
                "typeOfTenancyLicence=\"INTRODUCTORY_TENANCY\" "
                  + "OR typeOfTenancyLicence=\"DEMOTED_TENANCY\" "
                  + "OR typeOfTenancyLicence=\"OTHER\"")
            .readonly(PCSCase::getShowIntroductoryDemotedOtherGroundReasonPage,NEVER_SHOW)
            .label(
                "introductoryDemotedOrOtherGroundsForPossession-info",
                  """
                   ---
                   <p class="govuk-body" tabindex="0">In some cases, a claimant can make for possession of a property
                   without having to rely on a specific ground. If your claim meets these
                   requirements, you can select that you have no grounds for possession.

                   You may have already given the defendants notice of your intention to begin
                    possession proceedings.
                    If you have, you should have written the grounds you're making your claim under. You should select
                    these grounds here and any extra ground you'd like to add to your claim, if you need to.
                   </p>
                   """)
            .mandatory(PCSCase::getHasIntroductoryDemotedOtherGroundsForPossession)
            .mandatory(PCSCase::getIntroductoryDemotedOrOtherGrounds,
                    "hasIntroductoryDemotedOtherGroundsForPossession=\"YES\"")
            .mandatory(PCSCase::getOtherGroundsOfPossession,
                       "introductoryDemotedOrOtherGroundsCONTAINS\"OTHER\""
                        + "AND hasIntroductoryDemotedOtherGroundsForPossession=\"YES\"");
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();

        boolean hasOtherDiscretionaryGrounds = caseData.getIntroductoryDemotedOrOtherGrounds() == null ? false
            : caseData.getIntroductoryDemotedOrOtherGrounds()
            .stream()
            .anyMatch(ground -> ground != IntroductoryDemotedOrOtherGrounds.RENT_ARREARS
            );

        if (hasOtherDiscretionaryGrounds
                &&
            caseData.getHasIntroductoryDemotedOtherGroundsForPossession() == VerticalYesNo.YES) {
            caseData.setShowIntroductoryDemotedOtherGroundReasonPage(YesOrNo.YES);
        } else {
            caseData.setShowIntroductoryDemotedOtherGroundReasonPage(YesOrNo.NO);
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }

}
