package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

/**
 * Placeholder page configuration for the Grounds for Possession section.
 * Full implementation will be done in another ticket - responses not captured at the moment.
 */
public class GroundsForPossession implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("groundsForPossession")
            .pageLabel("Grounds for possession")
            .label("groundsForPossession-lineSeparator", "---")
            .showCondition("typeOfTenancyLicence!=\"SECURE_TENANCY\" "
                    + "AND typeOfTenancyLicence!=\"FLEXIBLE_TENANCY\" "
                    + "AND typeOfTenancyLicence!=\"INTRODUCTORY_TENANCY\" "
                    + "AND typeOfTenancyLicence!=\"DEMOTED_TENANCY\" "
                    + "AND typeOfTenancyLicence!=\"OTHER\"")
            .mandatory(PCSCase::getGroundsForPossession);

    }
}
