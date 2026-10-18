package uk.gov.hmcts.reform.pcs.enforcement.ccd.page.bailiffnote;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.page.CcdPage;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

/**
 * The one page of "Add a bailiff note", under the case's "claimant vs defendants" name. The note is mandatory
 * and limited to 6,800 characters on the field, so XUI shows its standard errors. The page is the first of the
 * event, so "Previous" is disabled.
 */
public class AddBailiffNotePage implements CcdPageConfiguration, CcdPage {

    public static final String PAGE_LABEL = "Add bailiff note";

    @Override
    public void addTo(PageBuilder pageBuilder) {
        String pageKey = getPageKey();
        pageBuilder
            .page(pageKey)
            .pageLabel(PAGE_LABEL)
            .complex(PCSCase::getEnforcementOrder)
            // Placed before the label, because XUI fills a label's ${...} only from the case view and the fields
            // placed before it on the form. The start callback sets it.
            .readonly(EnforcementOrder::getFormattedCaseName, NEVER_SHOW, true)
            .label(pageKey + "-parties",
                   "<p class=\"govuk-!-font-size-24 govuk-!-margin-top-0\">${formattedCaseName}</p>")
            .label(pageKey + "-line-separator", "---")
            .mandatory(EnforcementOrder::getBailiffNoteText)
            .done();
    }

    @Override
    public String getPageKey() {
        return CcdPage.derivePageKey(this.getClass());
    }
}
