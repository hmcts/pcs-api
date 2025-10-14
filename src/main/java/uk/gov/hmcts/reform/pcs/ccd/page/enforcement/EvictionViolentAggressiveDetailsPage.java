package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import java.util.ArrayList;
import java.util.List;

@Component
public class EvictionViolentAggressiveDetailsPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("evictionViolentAggressiveDetails", this::midEvent)
            .pageLabel("Their violent or aggressive behaviour")
            .showCondition("enforcementRiskCategoriesCONTAINS\"VIOLENT_OR_AGGRESSIVE\"")
            .optional(PCSCase::getEnforcementViolentDetails);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> before) {
        PCSCase data = details.getData();
        List<String> errors = new ArrayList<>();

        String txt = data.getEnforcementViolentDetails();
        if (txt == null || txt.isBlank()) {
            errors.add("Enter details");
        } else if (txt.length() > 6800) {
            errors.add("In 'How have they been violent or aggressive?', you have entered more than the maximum number of characters (6800)");
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(data)
            .errors(errors.isEmpty() ? null : errors)
            .build();
    }
}


