package uk.gov.hmcts.reform.pcs.ccd.page.testcasesupport;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

@Component
public class TestBulkPrintDocumentsPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("testBulkPrintDocuments")
            .pageLabel("Test bulk print")
            .label("testBulkPrintDocumentsInfo", """
                Upload supporting documents of any type Possessions accepts. They are converted to the bulk
                print standard and posted to the claimant, behind a coversheet, as one letter.
                """)
            .mandatory(PCSCase::getTestBulkPrintDocuments);
    }

}
