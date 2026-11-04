package uk.gov.hmcts.reform.pcs.enforcement.ccd.page.bailiffnote;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.page.CcdPage;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

public class AddBailiffNotePage implements CcdPageConfiguration, CcdPage {

    public static final String PAGE_LABEL = "Add bailiff note";

    @Override
    public void addTo(PageBuilder pageBuilder) {
        String pageKey = getPageKey();
        pageBuilder
            .page(pageKey)
            .pageLabel(PAGE_LABEL)
            .complex(PCSCase::getEnforcementOrder)
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
