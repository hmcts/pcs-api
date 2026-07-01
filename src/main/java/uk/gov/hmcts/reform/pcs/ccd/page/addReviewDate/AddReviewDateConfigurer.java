package uk.gov.hmcts.reform.pcs.ccd.page.addReviewDate;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.common.PageConfigurer;

@Component
@AllArgsConstructor
public class AddReviewDateConfigurer implements PageConfigurer {

    private final AddReviewDatePage addReviewDatePage;

    @Override
    public void configurePages(PageBuilder pageBuilder) {
        pageBuilder.add(addReviewDatePage);
    }
}
