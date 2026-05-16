package uk.gov.hmcts.reform.pcs.ccd.page.legalrepdocumentupload;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.legalrepdocumentupload.LegalRepDocumentUpload;
import uk.gov.hmcts.reform.pcs.ccd.page.CcdPage;

public class ExistingApplicationPage implements CcdPageConfiguration, CcdPage {

    public static final String EVICTION_DETAILS_CONTENT  = """
                    <p class="govuk-body">
                        We usually share anything you upload with the other party, for example your landlord, housing
                        association or mortgage provider.
                    </p>
                     <p class="govuk-body">
                        If your application is 'without notice' (where you have asked us to consider your application
                        without telling the other party) we will not share anything with them.
                    </p>
                    """;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        String pageKey = getPageKey();
        pageBuilder
            .page(pageKey)
            .pageLabel("Confirm if these documents relate to an existing application")
            .label(pageKey + "-line-separator", "---")
            .label(pageKey + "-content", EVICTION_DETAILS_CONTENT)
            .complex(PCSCase::getLegalRepDocumentUpload)
            .mandatory(LegalRepDocumentUpload::getUploadCategories);
    }

    @Override
    public String getPageKey() {
        return CcdPage.derivePageKey(this.getClass());
    }
}
