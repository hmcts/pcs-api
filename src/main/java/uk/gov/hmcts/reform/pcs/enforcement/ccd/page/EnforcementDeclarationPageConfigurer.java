package uk.gov.hmcts.reform.pcs.enforcement.ccd.page;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.common.PageConfigurer;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.warrant.LanguageUsedPage;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.warrant.StatementOfTruthPage;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.writ.LanguageUsedWritPage;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.writ.StatementOfTruthWritPage;

/**
 * The pages that close every enforcement application. A page's position in the journey is where it is added,
 * and the restitution journeys reuse the warrant and writ pairs, so these are registered last.
 */
@Component
@AllArgsConstructor
public class EnforcementDeclarationPageConfigurer implements PageConfigurer {

    @Override
    public void configurePages(PageBuilder pageBuilder) {
        pageBuilder
            .add(new LanguageUsedPage())
            .add(new StatementOfTruthPage())
            .add(new LanguageUsedWritPage())
            .add(new StatementOfTruthWritPage());
    }
}
