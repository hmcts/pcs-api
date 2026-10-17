package uk.gov.hmcts.reform.pcs.enforcement.ccd.page.warrantreturn;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.common.PageConfigurer;

@Component
@RequiredArgsConstructor
public class RecordWarrantReturnConfigurer implements PageConfigurer {

    private final RecordWarrantReturnPage recordWarrantReturnPage;

    @Override
    public void configurePages(PageBuilder pageBuilder) {
        pageBuilder.add(recordWarrantReturnPage);
    }
}
