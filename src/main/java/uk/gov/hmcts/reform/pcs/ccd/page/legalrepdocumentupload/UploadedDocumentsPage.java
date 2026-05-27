package uk.gov.hmcts.reform.pcs.ccd.page.legalrepdocumentupload;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.page.CcdPage;

public class UploadedDocumentsPage implements CcdPageConfiguration, CcdPage {

    public static final String DOCUMENT_UPLOADED_CONTENT  = """
                    <div class="govuk-panel govuk-panel--confirmation govuk-!-padding-top-3 govuk-!-padding-bottom-3">
                        <span class="govuk-panel__title govuk-!-font-size-36">
                            Document Uploaded
                        </span>
                    </div>
                        <p class="govuk-body">
                            We have received the documents you uploaded.
                        </p>
                         <h3>What happens next</h3>
                        <p class="govuk-body">
                            You do not need to do anything else. We will review the documents.
                        </p>
                    """;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        String pageKey = getPageKey();
        pageBuilder
            .page(pageKey)
            .pageLabel("Document uploaded")
            .label(pageKey + "-line-separator", "---")
            .label(pageKey + "-content", DOCUMENT_UPLOADED_CONTENT);

    }

    @Override
    public String getPageKey() {
        return CcdPage.derivePageKey(this.getClass());
    }
}
