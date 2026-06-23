package uk.gov.hmcts.reform.pcs.ccd.page.deletedraftclaim;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.common.PageConfigurer;

@Component
public class DeleteDraftClaimConfigurer implements PageConfigurer {

    @Override
    public void configurePages(PageBuilder pageBuilder) {
        pageBuilder.add(new DeleteDraftClaimPage());
    }
}
