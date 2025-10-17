package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import java.util.Set;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

/**
 * Page for selecting additional grounds for possession.
 */
public class RentArrearsGroundForPossessionAdditionalGrounds implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("groundForPossessionAdditionalGrounds", this::midEvent)
            .pageLabel("What are your grounds for possession?")
            .showCondition("hasOtherAdditionalGrounds=\"Yes\""
                           + " AND typeOfTenancyLicence=\"ASSURED_TENANCY\""
                           + " AND groundsForPossession=\"Yes\"")
            .readonly(PCSCase::getShowRentArrearsGroundReasonPage, NEVER_SHOW)
            .label("groundForPossessionAdditionalGrounds-info", """
            ---
            <p class="govuk-body">You may have already given the defendants notice of your intention to begin
                possession proceedings. If you have, you should have written the grounds you're making your
                claim under. You should select these grounds here and any extra grounds you'd like to add to
                your claim, if you need to.</p>
            """)
            .mandatory(PCSCase::getRentArrearsMandatoryGrounds)
            .mandatory(PCSCase::getRentArrearsDiscretionaryGrounds)
            .done();
    }

    public AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                 CaseDetails<PCSCase, State> detailsBefore) {

        PCSCase caseData = details.getData();
        Set<RentArrearsMandatoryGrounds> mandatoryGrounds = caseData.getRentArrearsMandatoryGrounds();
        Set<RentArrearsDiscretionaryGrounds> discretionaryGrounds = caseData.getRentArrearsDiscretionaryGrounds();

        boolean hasOtherMandatoryGrounds = mandatoryGrounds
            .stream()
            .anyMatch(ground -> ground != RentArrearsMandatoryGrounds.SERIOUS_RENT_ARREARS_GROUND8);

        boolean hasOtherDiscretionaryGrounds =  discretionaryGrounds
            .stream()
            .anyMatch(ground -> ground != RentArrearsDiscretionaryGrounds.RENT_ARREARS_GROUND10
                && ground != RentArrearsDiscretionaryGrounds.PERSISTENT_DELAY_GROUND11);

        boolean shouldShowReasonsPage = hasOtherDiscretionaryGrounds || hasOtherMandatoryGrounds;

        caseData.setShowRentArrearsGroundReasonPage(YesOrNo.from(shouldShowReasonsPage));

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }
}
