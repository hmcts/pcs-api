package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsOrBreachOfTenancy;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;

public class RentArrearsOrBreachOfTenancyGround implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("rentArrearsOrBreachOfTenancyGround", this::midEvent)
            .pageLabel(SecureOrFlexibleDiscretionaryGrounds.RENT_ARREARS_OR_BREACH_OF_TENANCY.getLabel())
            .showCondition("tenancy_TypeOfTenancyLicence=\"SECURE_TENANCY\""
                               + " OR tenancy_TypeOfTenancyLicence=\"FLEXIBLE_TENANCY\""
                               + " AND secureOrFlexibleDiscretionaryGroundsCONTAINS"
                               + "\"RENT_ARREARS_OR_BREACH_OF_TENANCY\""
                               + " AND legislativeCountry=\"England\"")
            .label("rentArrearsOrBreachOfTenancyGround-lineSeparator", "---")
            .mandatory(PCSCase::getRentArrearsOrBreachOfTenancy)
            .label("rentArrearsOrBreachOfTenancyGround-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();
        if (caseData.getRentArrearsOrBreachOfTenancy().contains(RentArrearsOrBreachOfTenancy.BREACH_OF_TENANCY)) {
            caseData.setShowBreachOfTenancyTextarea(YesOrNo.YES);
        } else {
            caseData.setShowBreachOfTenancyTextarea(YesOrNo.NO);
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data(caseData)
                .build();
    }

}
