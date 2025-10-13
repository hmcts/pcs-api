package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementOrder;

public class NameAndAddressForEvictionPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("enforcementEvictionNameAndAddress")
            .pageLabel("The name and address for the eviction")
            .label(
                "enforcementNameAndAddressPage-details-check",
                """
                ---
                <h2 class="govuk-heading-m">Check the name and address for the eviction</h2>
                """)

            .label(
                "enforcementNameAndAddressPage-details-confirmation",
                """
                ---
                <h2 class="govuk-heading-m">Is this the correct name and address for the eviction?</h2>
                """)
            .complex(PCSCase::getEnforcementOrder)
            .mandatory(EnforcementOrder::getNameAndAddressForEviction);

    }

}
