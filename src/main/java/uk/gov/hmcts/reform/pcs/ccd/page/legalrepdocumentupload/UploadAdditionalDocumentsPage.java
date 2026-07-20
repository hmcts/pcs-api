package uk.gov.hmcts.reform.pcs.ccd.page.legalrepdocumentupload;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.legalrepdocumentupload.LegalRepDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.legalrepdocumentupload.LegalRepDocumentUploadDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.CcdPage;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

@Component
@AllArgsConstructor
public class UploadAdditionalDocumentsPage implements CcdPageConfiguration, CcdPage {

    public static final String DOCUMENT_DETAILS_CONTENT  = """
                    <p class="govuk-body">
                        You should only upload documents that are relevant to the claim.
                    </p>

                    <p class="govuk-body">
                        Before you upload the document, give it a name that tells the court what it is,
                        for example  ‘witness statement’, or ‘tenancy agreement’.
                    </p>

                    <p class="govuk-body">
                       You can upload the following file types: DOC/DOCX (Word), XLS/XLSM
                       (Excel), PPT/PPTX (PowerPoint), PDF, RTF, TXT, CSV, JPG/JPEG, PNG, BMP,
                       TIF/TIFF.
                    </p>

                    <p class="govuk-body govuk-!-font-weight-bold govuk-!-margin-0">
                        Before you upload your documents
                    </p>

                    <p class="govuk-body">
                        Give your document a name that explains what it is.
                    </p>

                    """;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        String pageKey = getPageKey();
        pageBuilder
            .page(pageKey)
            .pageLabel("Upload additional documents")
            .label(pageKey + "-line-separator", "---")
            .label(pageKey + "-content", DOCUMENT_DETAILS_CONTENT)
            .complex(PCSCase::getLegalRepDocumentUploadDetails)
            .readonly(LegalRepDocumentUploadDetails::getIsWales, NEVER_SHOW)
            .list(LegalRepDocumentUploadDetails::getLegalRepDocuments)
            .mandatory(LegalRepDocument::getLegalRepDocumentType, "isWales = \"No\"")
            .mandatory(LegalRepDocument::getLegalRepDocumentTypeWales, "isWales = \"Yes\"")
            .mandatory(LegalRepDocument::getDocument)
            .mandatory(LegalRepDocument::getDescription)
            .done()
            .build();
    }

    @Override
    public String getPageKey() {
        return CcdPage.derivePageKey(this.getClass());
    }
}
