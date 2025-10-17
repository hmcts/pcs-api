package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;

import java.util.ArrayList;
import java.util.List;

@Component
public class EvictionFirearmsPossessionDetailsPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("evictionFirearmsPossessionDetails", this::midEvent)
            .pageLabel("Their history of firearm possession")
            .showCondition("enforcementRiskCategoriesCONTAINS\"FIREARMS_POSSESSION\"")
            .label("evictionFirearmsPossessionDetails-line-separator", "---")
            .mandatory(PCSCase::getEnforcementFirearmsDetails)
            .label("evictionFirearmsPossessionDetails-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> before) {
        PCSCase data = details.getData();
        List<String> errors = new ArrayList<>();

        String txt = data.getEnforcementFirearmsDetails();
        if (txt == null || txt.isBlank()) {
            errors.add("Enter details");
        } else if (txt.length() > 6800) {
            errors.add("In 'What is their history of firearm possession?', you have entered more than the maximum number of characters (6800)");
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(data)
            .errors(errors.isEmpty() ? null : errors)
            .build();
    }
}


