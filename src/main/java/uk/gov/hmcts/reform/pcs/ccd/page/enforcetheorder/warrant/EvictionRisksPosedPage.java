package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant;

import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.EnforcementRiskDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;

import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;

public class EvictionRisksPosedPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("evictionRisksPosed", this::midEvent)
            .pageLabel("The risks posed by everyone at the property")
            .showCondition("anyRiskToBailiff=\"YES\" AND selectEnforcementType=\"WARRANT\"")
            .label("evictionRisksPosed-line-separator", "---")
            .complex(PCSCase::getEnforcementOrder)
            .complex(EnforcementOrder::getWarrantDetails)
            .mandatory(WarrantDetails::getEnforcementRiskCategories)
            .done()
            .label("evictionRisksPosed-save-and-return", SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> before) {
        PCSCase data = details.getData();
        WarrantDetails warrantDetails = data.getEnforcementOrder().getWarrantDetails();

        // Initialize risk details if null
        if (warrantDetails.getRiskDetails() == null) {
            warrantDetails.setRiskDetails(EnforcementRiskDetails.builder().build());
        }

        // Validate that at least one category is selected
        if (warrantDetails.getEnforcementRiskCategories() == null
            || warrantDetails.getEnforcementRiskCategories().isEmpty()) {
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data(data)
                .errors(java.util.List.of("Select at least one option"))
                .build();
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(data)
            .build();
    }
}
