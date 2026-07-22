package uk.gov.hmcts.reform.pcs.ccd.page.caseworker.entergenapp;

import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.EnterGenAppRequest;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.fieldEquals;

public class ApplicationFee implements CcdPageConfiguration {

    private static final String YOU_MUST_REQUEST_PAYMENT = """
        <p class="govuk-body govuk-!-font-weight-bold">You must request payment from the applicant
        before entering this application</p>
        """;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("applicationFee", this::midEvent)
            .pageLabel("Application fee")
            .label("applicationFee-lineSeparator", "---")
            .complex(PCSCase::getEnterGenAppRequest)
            .mandatory(EnterGenAppRequest::getFeeReceived)
            .mandatory(EnterGenAppRequest::getFeeAmountReceived,
                fieldEquals("enter_genapp_FeeReceived", VerticalYesNo.YES))
            .label("applicationFee-yourMustRequestPayment", YOU_MUST_REQUEST_PAYMENT,
                fieldEquals("enter_genapp_FeeReceived", VerticalYesNo.NO))
            .mandatory(EnterGenAppRequest::getAppliedForHwf)
            .mandatory(EnterGenAppRequest::getHwfReference,
                       fieldEquals("enter_genapp_AppliedForHwf", VerticalYesNo.YES))
            .done();
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {


        PCSCase caseData = details.getData();
        EnterGenAppRequest enterGenAppRequest = caseData.getEnterGenAppRequest();

        if (enterGenAppRequest.getFeeReceived() == VerticalYesNo.NO) {
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .errorMessageOverride("You must request payment from the applicant before entering this application")
                .build();
        } else {
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data(caseData)
                .build();
        }

    }

}
