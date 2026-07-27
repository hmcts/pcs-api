package uk.gov.hmcts.reform.pcs.ccd.page.documentamend;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;

@Component
public class AmendDocumentDetailsPlaceholderPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("amendDocumentDetails")
            .pageLabel("Amend document details")
            .label("amendDocumentDetails-selectedDocument", """
                ---
                Selected document: ${documentAmend_SelectedDocumentFileName}
                """);
    }
}
