package uk.gov.hmcts.reform.pcs.ccd.page.caseworker.entercounterclaim;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;

@Component
public class UploadCounterClaimForm implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("uploadCounterClaimForm")
            .pageLabel("Upload counterclaim form")
            .label("uploadCounterClaimForm-placeholder", "Placeholder - to be implemented");
    }
}
