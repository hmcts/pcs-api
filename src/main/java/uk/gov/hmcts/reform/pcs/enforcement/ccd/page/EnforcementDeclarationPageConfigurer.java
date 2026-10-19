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
 * The "Language used" and "Statement of truth" pages that close every enforcement application (HDPI-7555). The
 * warrant pair serves the warrant of restitution journey as well as the warrant of possession one, and the writ
 * pair the writ of restitution journey, so they are registered after every type's own questions rather than
 * within them: a page's position in the journey is where it is added.
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
