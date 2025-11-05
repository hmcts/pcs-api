package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementRiskDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.RiskCategory;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;

import java.util.ArrayList;
import java.util.List;

public class PoliceOrSocialServicesRiskPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("policeOrSocialServicesRisk", this::midEvent)
                .pageLabel("Their history of police or social services visits to the property")
                .showCondition("anyRiskToBailiff=\"YES\" AND enforcementRiskCategoriesCONTAINS\"AGENCY_VISITS\"")
                .label("policeOrSocialServicesRisk-line-separator", "---")
                .complex(PCSCase::getEnforcementOrder)
                .complex(EnforcementOrder::getRiskDetails)
                .mandatory(EnforcementRiskDetails::getEnforcementPoliceOrSocialServicesDetails).done()
                .label("policeOrSocialServicesRisk-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> before) {
        PCSCase data = details.getData();
        List<String> errors = new ArrayList<>();

        String txt = data.getEnforcementOrder().getRiskDetails().getEnforcementPoliceOrSocialServicesDetails();

        // Refactor validation logic to use TextAreaValidationService from PR #751 when merged
        if (txt.length() > EnforcementRiskValidationUtils.getCharacterLimit()) {
            errors.add(EnforcementRiskValidationUtils
                    .getCharacterLimitErrorMessage(RiskCategory.AGENCY_VISITS));
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data(data)
                .errors(errors)
                .build();
    }
}
