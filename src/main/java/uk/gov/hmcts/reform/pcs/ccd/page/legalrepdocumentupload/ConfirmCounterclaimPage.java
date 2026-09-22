package uk.gov.hmcts.reform.pcs.ccd.page.legalrepdocumentupload;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.legalrepdocumentupload.LegalRepDocumentUploadDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.CcdPage;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;
import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.fieldEquals;

public class ConfirmCounterclaimPage implements CcdPageConfiguration, CcdPage {

    private static final String GUIDANCE_CONTENT = """
                    <p class="govuk-body">
                        We usually share anything you upload with the other parties, for example
                        a tenant, landlord, housing association, or mortgage lender.
                    </p>
                    <p class="govuk-body">
                        If your application is ‘without notice’ (where you have asked us to consider your application
                        without telling the other party) we will not share anything with them.
                    </p>
                    """;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        String pageKey = getPageKey();
        pageBuilder
            .page(pageKey)
            .pageLabel("Confirm if these documents relate to a counterclaim")
            .showCondition(fieldEquals("lrDocUpload_ShowCounterclaimPage", VerticalYesNo.YES))
            .complex(PCSCase::getLegalRepDocumentUploadDetails)
            .readonly(LegalRepDocumentUploadDetails::getShowCounterclaimPage, NEVER_SHOW)
            .readonly(LegalRepDocumentUploadDetails::getCounterclaimDocumentLinks, NEVER_SHOW)
            .done()
            .label(pageKey + "-line-separator", "---")
            .label(pageKey + "-content", GUIDANCE_CONTENT)
            .label(pageKey + "-links", "${lrDocUpload_CounterclaimDocumentLinks}")
            .complex(PCSCase::getLegalRepDocumentUploadDetails)
            .mandatory(LegalRepDocumentUploadDetails::getValidCounterclaims)
            .done();
    }

    @Override
    public String getPageKey() {
        return CcdPage.derivePageKey(this.getClass());
    }
}
