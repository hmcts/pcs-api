package uk.gov.hmcts.reform.pcs.enforcement.ccd.page.confirmeviction;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.common.PageConfigurer;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.warrant.WarrantRiskPagesConfigurer;

/**
 * The attendance question, then the warrant application's risk pages, pre-filled with the claimant's current
 * answers so they can change them (HDPI-7552).
 */
@Component
@AllArgsConstructor
public class ConfirmEvictionConfigurer implements PageConfigurer {

    private final WarrantRiskPagesConfigurer warrantRiskPagesConfigurer;

    @Override
    public void configurePages(PageBuilder pageBuilder) {
        pageBuilder
            .add(new ConfirmEvictionDetailsPage())
            .add(new EvictionDatePage());
        warrantRiskPagesConfigurer.configurePages(pageBuilder);
    }
}
