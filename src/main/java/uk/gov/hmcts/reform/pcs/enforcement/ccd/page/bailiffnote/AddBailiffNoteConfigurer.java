package uk.gov.hmcts.reform.pcs.enforcement.ccd.page.bailiffnote;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.common.PageConfigurer;

@Component
public class AddBailiffNoteConfigurer implements PageConfigurer {

    @Override
    public void configurePages(PageBuilder pageBuilder) {
        pageBuilder.add(new AddBailiffNotePage());
    }
}
