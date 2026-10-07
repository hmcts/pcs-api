package uk.gov.hmcts.reform.pcs.ccd.page.claimantmakeanapplication;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.XuiGenAppRequest;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

@Slf4j
@AllArgsConstructor
public class ChooseAnApplication implements CcdPageConfiguration {

    private static final String INFO_MARKDOWN = """
        <p class="govuk-body">You cannot apply to suspend (stop or delay) an eviction online.</p>
        <p class="govuk-body govuk-!-margin-bottom-1">You must apply by post:</p>
        <ul class="govuk-list govuk-list--bullet">
            <li class="govuk-!-font-size-19">
                <a href="https://www.gov.uk/repossession/delay-eviction" target="_blank" rel="noopener noreferrer"
                class="govuk-link">read the guidance explaining how to suspend an eviction
                 (GOV.UK, opens in a new tab)</a>
            </li>
            <li class="govuk-!-font-size-19">
                <a href="https://www.gov.uk/government/publications/form-n244-application-notice"
                target="_blank" rel="noopener noreferrer" class="govuk-link">fill in the N244 form</a>
            </li>
            <li class="govuk-!-font-size-19"><a href="https://www.gov.uk/find-court-tribunal" target="_blank"
                rel="noopener noreferrer" class="govuk-link">find your local court</a>
            </li>
            <li class="govuk-!-font-size-19">
                send the completed form to the court, or deliver it by hand
            </li>
        </ul>
        """;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("chooseAnApplication", this::midEvent)
            .pageLabel("Choose an application")
            .label("chooseAnApplication-lineSeparator", "---")
            .label("chooseAnApplication-info", INFO_MARKDOWN)
            .complex(PCSCase::getXuiGenAppRequest)
            .readonly(XuiGenAppRequest::getStandardFee, NEVER_SHOW, true)
            .readonly(XuiGenAppRequest::getMaxFee, NEVER_SHOW, true)
            .readonly(XuiGenAppRequest::getApplicationType, NEVER_SHOW, true)
            .mandatory(XuiGenAppRequest::getClaimantGenAppType)
            .done();
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {

        PCSCase caseData = details.getData();
        XuiGenAppRequest xuiGenAppRequest = caseData.getXuiGenAppRequest();

        xuiGenAppRequest.setApplicationType(xuiGenAppRequest.getClaimantGenAppType().toGenAppType());
        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }

}
