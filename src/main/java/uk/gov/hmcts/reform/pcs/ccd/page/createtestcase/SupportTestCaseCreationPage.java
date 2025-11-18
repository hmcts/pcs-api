package uk.gov.hmcts.reform.pcs.ccd.page.createtestcase;

import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

public class SupportTestCaseCreationPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("supportTestCaseCreationPage", this::midEvent)
            .label("mainContent",
                   "<h1 class=\"govuk-heading-l\">Support Test Case Creation</h1>"
                       + "<p class=\"govuk-body\">For creating test cases."
                       + "</p>"
            );
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        Long id = details.getId();

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder().build();
    }

}
