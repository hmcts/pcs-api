package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

public class WantToUploadDocuments implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("wantToUploadDocuments")
            .pageLabel("Upload additional documents")
            .label("wantToUploadDocuments-separator", "---")
            .label("wantToUploadDocuments-heading",
                """
                <h2 class="govuk-heading-m">Do you want to upload any additional documents?</h2>
                """)
            .mandatory(PCSCase::getWantToUploadDocuments);
    }
}
