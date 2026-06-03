package uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.XuiGenAppRequest;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

@Slf4j
@AllArgsConstructor
public class WhichLanguage implements CcdPageConfiguration {

    private static final String INFO_MARKDOWN = """
        <p class="govuk-body">If someone else helped you to answer a question in this service,
        ask them if they answered any questions in Welsh. We’ll use this to make sure
        your claim is processed correctly.</p>
        """;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("whichLanguage", this::midEvent)
            .pageLabel("Which language did you use to complete this service?")
            .label("whichLanguage-lineSeparator", "---")
            .label("whichLanguage-info", INFO_MARKDOWN)
            .readonly(PCSCase::getCurrentRepresentedPartyId, NEVER_SHOW, true)
            .complex(PCSCase::getXuiGenAppRequest)
            .mandatory(XuiGenAppRequest::getLanguageUsed)
            .done();
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {

        PCSCase caseData = details.getData();

        if (caseData.getCurrentRepresentedPartyId() == null) {
            DynamicListElement selectedPartyElement = caseData.getRepresentedPartyNames().getValue();
            caseData.setCurrentRepresentedPartyId(selectedPartyElement.getCode().toString());
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }


}
