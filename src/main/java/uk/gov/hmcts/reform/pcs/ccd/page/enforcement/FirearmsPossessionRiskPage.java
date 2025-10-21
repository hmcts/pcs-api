package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import org.springframework.stereotype.Component;
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

@Component
public class FirearmsPossessionRiskPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("evictionFirearmsPossessionDetails", this::midEvent)
            .pageLabel("Their history of firearm possession")
            .showCondition("enforcementRiskCategoriesCONTAINS\"FIREARMS_POSSESSION\"")
            .label("evictionFirearmsPossessionDetails-line-separator", "---")
            .complex(PCSCase::getEnforcementOrder)
            .complex(EnforcementOrder::getRiskDetails)
            .mandatory(EnforcementRiskDetails::getEnforcementFirearmsDetails)
            .done()
            .label("evictionFirearmsPossessionDetails-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> before) {
        PCSCase data = details.getData();
        List<String> errors = new ArrayList<>();

        String txt = data.getEnforcementOrder() != null && data.getEnforcementOrder().getRiskDetails() != null
            ? data.getEnforcementOrder().getRiskDetails().getEnforcementFirearmsDetails()
            : null;
        // TODO: Refactor validation logic to use TextAreaValidationService from PR #751 when merged
        if (txt == null || txt.isBlank()) {
            errors.add("Enter details");
        } else if (txt.length() > EnforcementRiskValidationUtils.getCharacterLimit()) {
            // TODO: Use TextAreaValidationService from PR #751 when merged
            errors.add(EnforcementRiskValidationUtils.getCharacterLimitErrorMessage(RiskCategory.FIREARMS_POSSESSION));
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(data)
            .errors(errors.isEmpty() ? null : errors)
            .build();
    }

}


