package uk.gov.hmcts.reform.pcs.ccd.page.legalrepdocumentupload;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.legalrepdocumentupload.LegalRepDocumentUpload;
import uk.gov.hmcts.reform.pcs.ccd.page.CcdPage;

public class UploadAdditionalDocumentsPage implements CcdPageConfiguration, CcdPage {

    public static final String DOCUMENT_DETAILS_CONTENT  = """
                    <p class="govuk-body">
                        You should only upload documents that are relevant to your application.
                    </p>

                    <p class="govuk-body">
                        Before you upload the document, give it a name that tells the court what it is, for example
                        ‘witness statement’, or ‘tenancy agreement’.
                    </p>

                    <p class="govuk-body">
                       You can upload the following file types: DOC/DOCX (Word), XLS/XLSM (Excel), PPT/PPTX (PowerPoint)
                       , PDF, RTF, TXT, CSV, JPG/JPEG, PNG, BMP, TIF/TIFF.
                    </p>
                    """;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        String pageKey = getPageKey();
        pageBuilder
            .page(pageKey)
            .pageLabel("Upload your documents")
            .label(pageKey + "-line-separator", "---")
            .label(pageKey + "-content", DOCUMENT_DETAILS_CONTENT)
            .complex(PCSCase::getLegalRepDocumentUpload)
            .mandatory(LegalRepDocumentUpload::getAdditionalDocuments)
            .done()
            .build();
    }

    @Override
    public String getPageKey() {
        return CcdPage.derivePageKey(this.getClass());
    }
}
