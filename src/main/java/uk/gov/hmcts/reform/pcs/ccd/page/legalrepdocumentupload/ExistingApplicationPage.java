package uk.gov.hmcts.reform.pcs.ccd.page.legalrepdocumentupload;

import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.legalrepdocumentupload.LegalRepDocumentUploadDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.CcdPage;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

public class ExistingApplicationPage implements CcdPageConfiguration, CcdPage {

    private static final String WITHOUT_NOTICE_APPLICATION_GUIDANCE  = """
                    <p class="govuk-body">
                        We usually share anything you upload with the other parties, for example other defendants,
                        the defendant’s landlord, housing association or mortgage provider.
                    </p>
                     <p class="govuk-body">
                        If your application is ‘without notice’ (where you have asked us to consider your application
                        without telling the other party) we will not share anything with them.
                    </p>
                    """;

    static final String ERROR_MESSAGE =
        "Confirm if these documents relate to an existing application";

    @Override
    public void addTo(PageBuilder pageBuilder) {
        String pageKey = getPageKey();
        pageBuilder
            .page(pageKey, this::midEvent)
            .pageLabel("Confirm if these documents relate to an existing application")
            .showCondition("showExistingApplicationPage=\"Yes\"")
            .complex(PCSCase::getLegalRepDocumentUploadDetails)
            .readonly(LegalRepDocumentUploadDetails::getShowExistingApplicationPage, NEVER_SHOW)
            .done()
            .label(pageKey + "-line-separator", "---")
            .label(pageKey + "-content", WITHOUT_NOTICE_APPLICATION_GUIDANCE)
            .complex(PCSCase::getLegalRepDocumentUploadDetails)
            .mandatory(LegalRepDocumentUploadDetails::getValidCategories);
    }

    @Override
    public String getPageKey() {
        return CcdPage.derivePageKey(this.getClass());
    }

    public AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                 CaseDetails<PCSCase, State> before) {
        PCSCase data = details.getData();
        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(data)
            .errorMessageOverride(ERROR_MESSAGE)
            .build();
    }
}
