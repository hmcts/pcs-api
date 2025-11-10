package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsOrBreachOfTenancy;
import uk.gov.hmcts.reform.pcs.ccd.domain.SecureOrFlexibleDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

public class RentArrearsOrBreachOfTenancyGround implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("rentArrearsOrBreachOfTenancyGround", this::midEvent)
            .pageLabel(SecureOrFlexibleDiscretionaryGrounds.RENT_ARREARS_OR_BREACH_OF_TENANCY.getLabel())
            .showCondition("typeOfTenancyLicence=\"SECURE_TENANCY\" OR typeOfTenancyLicence=\"FLEXIBLE_TENANCY\""
                               + " AND secureOrFlexibleDiscretionaryGroundsCONTAINS"
                               + "\"RENT_ARREARS_OR_BREACH_OF_TENANCY\"")
            .label("rentArrearsOrBreachOfTenancyGround-lineSeparator", "---")
            .mandatory(PCSCase::getRentArrearsOrBreachOfTenancy);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();
        if (caseData.getRentArrearsOrBreachOfTenancy().contains(RentArrearsOrBreachOfTenancy.BREACH_OF_TENANCY)) {
            caseData.setShowBreachOfTenancyTextarea(YesOrNo.YES);
        } else {
            caseData.setShowBreachOfTenancyTextarea(YesOrNo.NO);
        }

        // Show Rent Details page only when user has selected "Rent arrears" option
        boolean hasRentArrears = caseData.getRentArrearsOrBreachOfTenancy()
            .contains(RentArrearsOrBreachOfTenancy.RENT_ARREARS);
        caseData.setShowRentDetailsPage(YesOrNo.from(hasRentArrears));

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data(caseData)
                .build();
    }

}
