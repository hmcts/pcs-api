package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementRiskDetails;

import java.util.ArrayList;
import java.util.List;

public class VerbalOrWrittenThreatsRiskPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("evictionVerbalOrWrittenThreatsDetails", this::midEvent)
                .pageLabel("Their verbal or written threats")
                .showCondition("enforcementRiskCategoriesCONTAINS\"VERBAL_OR_WRITTEN_THREATS\"")
                .label("verbalOrWrittenThreatsPage-line-separator", "---")
                .label("verbalOrWrittenThreatsPage-label","""
                <h3 tabindex="0"> What kind of verbal or written threats have they made?</h3>
                """)
                .complex(PCSCase::getEnforcementOrder)
                .complex(EnforcementOrder::getRiskDetails)
                .mandatory(EnforcementRiskDetails::getEnforcementVerbalOrWrittenThreatsDetails);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> before) {
        PCSCase data = details.getData();
        List<String> errors = new ArrayList<>();

        String txt = data.getEnforcementOrder().getRiskDetails().getEnforcementVerbalOrWrittenThreatsDetails();
        if (txt == null || txt.isBlank()) {
            errors.add("Enter details");
        } else if (txt.length() > 6800) {
            errors.add("""
                In 'What kind of verbal or written threats have they made?', 
                you have entered more than the maximum number of characters (6800)
                """);
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data(data)
                .errors(errors.isEmpty() ? null : errors)
                .build();
    }
}
