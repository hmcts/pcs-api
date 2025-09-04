package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsOrBreachOfTenancy;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

public class RentArrearsOrBreachOfTenancyGround implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("rentArrearsOrBreachOfTenancyGround",this::midEvent)
            .pageLabel("Rent arrears or breach of tenancy (ground 1)")
            .showCondition("typeOfTenancyLicence=\"SECURE_TENANCY\" OR typeOfTenancyLicence=\"FLEXIBLE_TENANCY\""
                               + " AND selectedSecureOrFlexibleDiscretionaryGroundsCONTAINS"
                               + "\"RENT_ARREARS_OR_BREACH_OF_TENANCY\"")
            .label("rentArrearsOrBreachOfTenancyGround-lineSeparator", "---")
            .mandatory(PCSCase::getRentAreasOrBreachOfTenancy);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();
        if (caseData.getRentAreasOrBreachOfTenancy().getValue().stream()
                .anyMatch(item -> RentArrearsOrBreachOfTenancy.BREACH_OF_TENANCY.getLabel()
                    .equals(item.getLabel()))) {

            caseData.setShowBreachOfTenancyTextarea(YesOrNo.YES);
        } else {
            caseData.setShowBreachOfTenancyTextarea(YesOrNo.NO);
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }

}
