package uk.gov.hmcts.reform.pcs.ccd.page.addReviewDate;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.common.PageConfigurer;

@Component
@AllArgsConstructor
public class AddCaseReviewDateConfigurer implements PageConfigurer {

    private final AddCaseReviewDatePage addCaseReviewDatePage;

    @Override
    public void configurePages(PageBuilder pageBuilder) {
        pageBuilder.add(addCaseReviewDatePage);
    }
}
