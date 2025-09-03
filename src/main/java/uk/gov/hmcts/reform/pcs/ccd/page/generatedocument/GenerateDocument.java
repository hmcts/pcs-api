package uk.gov.hmcts.reform.pcs.ccd.page.generatedocument;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;

public class GenerateDocument implements CcdPageConfiguration {
    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("generateDocument")
            .pageLabel("Generate documents")
            .label("generatingDocuments", """
                    This will generate a document using the case data and make it available in the case file view.
                    
                    The document will be:
                    * Generated using DocAssembly and Docmosis
                    * Stored in the Document Management Store
                    * Added to the case documents
                    * Visible in the case file view
                    
                    Click 'Continue' to proceed with document generation.
                    """)
            .label("confirmation", "Are you sure you want to generate a document?");
    }
}
