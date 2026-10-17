package uk.gov.hmcts.reform.pcs.enforcement.ccd.page.allocatebailiff;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.common.PageConfigurer;
import uk.gov.hmcts.reform.pcs.enforcement.service.BailiffDirectoryService;

/**
 * "Allocate case to bailiff" has one page, then check your answers. There is no "Choose a role type" or
 * "Confirm length of assignment" page: the role is always allocated-bailiff and it has no end date.
 */
@Component
@RequiredArgsConstructor
public class AllocateBailiffConfigurer implements PageConfigurer {

    private final BailiffDirectoryService bailiffDirectoryService;

    @Override
    public void configurePages(PageBuilder pageBuilder) {
        pageBuilder.add(new FindBailiffPage(bailiffDirectoryService));
    }
}
