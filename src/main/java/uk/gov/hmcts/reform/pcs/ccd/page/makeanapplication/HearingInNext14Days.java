package uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.XuiGenAppRequest;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.fieldEquals;

@Slf4j
@AllArgsConstructor
public class HearingInNext14Days implements CcdPageConfiguration {

    private static final String INFO_MARKDOWN = """
        <p class="govuk-body">The ‘other parties’ are the other people or organisations named on the claim form.
        For example a tenant, landlord, housing association, or mortgage lender.</p>
        <p class="govuk-body">Every other party will need to agree to let the defendant make this application,
        if they don’t agree, the defendant may need to pay a higher fee.</p>
        """;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("hearingInNext14Days", this::midEvent)
            .pageLabel("Is the defendant’s court hearing in the next 14 days?")
            .showCondition(fieldEquals("xui_genapp_ApplicationType", GenAppType.ADJOURN))
            .label("hearingInNext14Days-lineSeparator", "---")
            .label("hearingInNext14Days-info", INFO_MARKDOWN)
            .complex(PCSCase::getXuiGenAppRequest)
            .mandatory(XuiGenAppRequest::getWithin14Days)
            .done();
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {

        PCSCase caseData = details.getData();
        XuiGenAppRequest xuiGenAppRequest = caseData.getXuiGenAppRequest();

        xuiGenAppRequest.setShowHwfScreens(VerticalYesNo.YES);

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }

}
