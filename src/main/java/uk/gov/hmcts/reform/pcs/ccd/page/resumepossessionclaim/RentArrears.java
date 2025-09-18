package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

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
                .showCondition("groundsForPossession=\"Yes\" AND noticeServed = \"No\" AND rentFrequency=\"OTHER\"")
                .pageLabel("Details of rent arrears")
                .label("rentArrears-lineSeparator", "---");
    }
}
