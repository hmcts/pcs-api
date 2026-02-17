package uk.gov.hmcts.reform.pcs.ccd.page.addrentstatement;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

public class UploadDocument implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder.page("uploadDocument")
            .pageLabel("Upload document")
            .label("uploadDocument-info", """
                ---
                <p class="govuk-body">
                You can upload an amended rent statement here if you want.
                </p>
                """)
            .mandatory(PCSCase::getAmendedRentStatement)
            .mandatory(PCSCase::getIsAmendedDocument);
    }
}
