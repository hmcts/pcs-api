package uk.gov.hmcts.reform.pcs.enforcement.ccd.page.allocatebailiff;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.common.PageConfigurer;
import uk.gov.hmcts.reform.pcs.enforcement.service.BailiffDirectoryService;

@Component
@RequiredArgsConstructor
public class AllocateBailiffConfigurer implements PageConfigurer {

    private final BailiffDirectoryService bailiffDirectoryService;

    @Override
    public void configurePages(PageBuilder pageBuilder) {
        pageBuilder.add(new FindBailiffPage(bailiffDirectoryService));
    }
}
