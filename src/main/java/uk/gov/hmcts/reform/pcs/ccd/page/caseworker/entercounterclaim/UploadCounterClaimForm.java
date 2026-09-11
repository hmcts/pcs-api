package uk.gov.hmcts.reform.pcs.ccd.page.caseworker.entercounterclaim;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

@Component
public class UploadCounterClaimForm implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("uploadCounterClaimForm")
            .pageLabel("Upload counterclaim form")
            .label("uploadCounterClaimForm-lineSeparator", "---")
            .mandatory(PCSCase::getCounterclaimForm);
    }
}
