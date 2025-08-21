package uk.gov.hmcts.reform.pcs.ccd.page.uploadsupportingdocs;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

public class DocumentUploadCategoryA implements CcdPageConfiguration {
    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("documentUpload")
                .pageLabel("Upload supporting documents")
                .label("uploadLimits", """
                    Documents should be:
                    * uploaded separately, not as one large file
                    * a maximum of 100MB in size (larger files must be split)
                    * clearly labelled, e.g. applicant-name-evidence.pdf
                    """)
                .optional(PCSCase::getSupportingDocumentsCategoryA);
    }
}
