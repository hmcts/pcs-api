package uk.gov.hmcts.reform.pcs.ccd.page.caseworker.entergenapp;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.EnterGenAppRequest;

public class UploadRelatedEvidence implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("uploadRelatedEvidence")
            .pageLabel("Upload related evidence")
            .label("uploadRelatedEvidence-lineSeparator", "---")
            .complex(PCSCase::getEnterGenAppRequest)
            .optional(EnterGenAppRequest::getRelatedEvidence)
            .done();
    }

}
