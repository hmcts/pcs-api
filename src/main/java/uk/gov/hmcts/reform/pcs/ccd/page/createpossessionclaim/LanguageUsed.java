package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

/**
 * CCD page configuration for the Language used screen.
 * Allows users to indicate whether any part of their application was completed in Welsh.
 */
public class LanguageUsed implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("languageUsed")
            .pageLabel("Language used")
            .label("languageUsedMainContent",
                   "<h1 class=\"govuk-heading-l\">Language used</h1>"
                       + "<p class=\"govuk-body govuk-!-font-size-19\">Did you complete all or part of this claim in Welsh?</p>")
            .mandatory(PCSCase::getWelshUsed);
    }
}
