package uk.gov.hmcts.reform.pcs.ccd.page.documentamend;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.documentamend.DocumentAmendDetails;

@Component
public class AmendDocumentDetailsPage implements CcdPageConfiguration {

    private static final String PAGE_ID = "amendDocumentDetails";

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page(PAGE_ID)
            .pageLabel("Amend document details")
            .label(PAGE_ID + "-separator", "---")
            .complex(PCSCase::getDocumentAmendDetails)
                .mandatory(DocumentAmendDetails::getAmendedFileName)
                .optional(DocumentAmendDetails::getIssueDate)
            .done();
    }
}
