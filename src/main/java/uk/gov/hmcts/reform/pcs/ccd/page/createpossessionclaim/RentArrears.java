package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;

/**
 * Page configuration for the Rent Arrears section.
 * This page is only shown when the user selects "Other" as the rent payment frequency.
 */
public class RentArrears implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("rentArrears")
                .pageLabel("Details of rent arrears")
                .showCondition("rentFrequency=\"OTHER\"")
                .label("rentArrears-lineSeparator", "---");
    }
}
