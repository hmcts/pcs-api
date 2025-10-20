package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import java.util.ArrayList;
import java.util.List;

public class PoliceOrSocialServicesProperty implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("evictionPoliceOrSocialServicesPropertyDetails", this::midEvent)
                .pageLabel("Their history of police or social services visits to the property")
                .showCondition("enforcementRiskCategoriesCONTAINS\"AGENCY_VISITS\"")
                .label("policeOrSocialServicesProperty-line-separator", "---")
                .label("policeOrSocialServicesProperty-label","""
                <h3 tabindex="0">Why did the police or social services visit the property?</h3>
                """)
                .mandatory(PCSCase::getEnforcementPoliceOrSocialServicesDetails);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> before) {
        PCSCase data = details.getData();
        List<String> errors = new ArrayList<>();

        String txt = data.getEnforcementPoliceOrSocialServicesDetails();
        if (txt == null || txt.isBlank()) {
            errors.add("Enter details");
        } else if (txt.length() > 6800) {
            errors.add("""
                In 'Why did the police or social services visit the property?',
                you have entered more than the maximum number of characters (6800)
                """);
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data(data)
                .errors(errors.isEmpty() ? null : errors)
                .build();
    }
}
