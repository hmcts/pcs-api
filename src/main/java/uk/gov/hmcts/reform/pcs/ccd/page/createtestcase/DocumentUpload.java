package uk.gov.hmcts.reform.pcs.ccd.page.createtestcase;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

public class DocumentUpload implements CcdPageConfiguration {
    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("Document Upload")
            .pageLabel("Upload supporting documents")
            .mandatory(PCSCase::getSupportingDocuments);
    }
}
