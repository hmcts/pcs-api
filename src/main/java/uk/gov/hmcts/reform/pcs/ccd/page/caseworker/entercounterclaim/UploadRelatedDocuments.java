package uk.gov.hmcts.reform.pcs.ccd.page.caseworker.entercounterclaim;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.EnterCounterClaimDetails;

@Component
public class UploadRelatedDocuments implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("uploadRelatedDocuments")
            .pageLabel("Upload related documents")
            .label("uploadRelatedDocuments-lineSeparator", "---")
            .complex(PCSCase::getEnterCounterClaim)
            .optional(EnterCounterClaimDetails::getRelatedDocuments)
            .done();
    }

}
