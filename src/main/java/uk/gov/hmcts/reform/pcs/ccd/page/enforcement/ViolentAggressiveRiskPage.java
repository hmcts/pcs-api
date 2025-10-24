package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementRiskDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.RiskCategory;

import java.util.ArrayList;
import java.util.List;

public class ViolentAggressiveRiskPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("violentAggressiveRisk", this::midEvent)
            .pageLabel("Their violent or aggressive behaviour")
            .showCondition("enforcementRiskCategoriesCONTAINS\"VIOLENT_OR_AGGRESSIVE\"")
            .label("violentAggressiveRisk-line-separator", "---")
            .complex(PCSCase::getEnforcementOrder)
            .complex(EnforcementOrder::getRiskDetails)
            .mandatory(EnforcementRiskDetails::getEnforcementViolentDetails)
            .done()
            .label("violentAggressiveRisk-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> before) {
        PCSCase data = details.getData();
        List<String> errors = new ArrayList<>();

        String txt = data.getEnforcementOrder().getRiskDetails().getEnforcementViolentDetails();
        // TODO: Refactor validation logic to use TextAreaValidationService from PR #751 when merged
        if (txt.length() > EnforcementRiskValidationUtils.getCharacterLimit()) {
            // TODO: Use TextAreaValidationService from PR #751 when merged
            errors.add(EnforcementRiskValidationUtils
                    .getCharacterLimitErrorMessage(RiskCategory.VIOLENT_OR_AGGRESSIVE));
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(data)
            .errors(errors)
            .build();
    }

}


