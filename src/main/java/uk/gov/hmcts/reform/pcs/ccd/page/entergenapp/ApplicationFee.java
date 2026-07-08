package uk.gov.hmcts.reform.pcs.ccd.page.entergenapp;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;

/**
 * Placeholder page — the ACs reference navigation to an "Application fee" screen from both the
 * Adjourn (via Hearing date) and Set aside paths, but don't specify its fields yet. Revisit once
 * that part of the ticket/AC is defined.
 */
public class ApplicationFee implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("applicationFee")
            .pageLabel("Application fee")
            .label("applicationFee-lineSeparator", "---")
            .label("applicationFee-placeholder", "Application fee details(placeholder)");
    }

}
