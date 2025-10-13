package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementMultiLabel;
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
                    <hr />
                    <h2 class="govuk-heading-m">Check the name and address for the eviction</h2>
                    <div class="govuk-width-container">
                          <main class="govuk-main-wrapper">
                            <div class="govuk-grid-row">
                              <div class="govuk-grid-column-one-third">
                                <p class="govuk-body">Defendants</p>
                              </div>
                              <div class="govuk-grid-column-one-third">
                                <p class="govuk-body">defendant 1</p>
                              </div>
                            </div>
                            <hr />
                            <div class="govuk-grid-row">
                              <div class="govuk-grid-column-one-third">
                                <p class="govuk-body">Address</p>
                              </div>
                              <div class="govuk-grid-column-one-third">
                                <p class="govuk-body">property address</p>
                              </div>
                            </div>
                          </main>
                        </div>
                """)

            .label(
                "enforcementNameAndAddressPage-details-confirmation",
                """
                ---
                <h2 class="govuk-heading-m">Is this the correct name and address for the eviction?</h2>
                """)
            .complex(PCSCase::getEnforcementOrder)
            .mandatory(EnforcementOrder::getNameAndAddressForEviction)
            .label("enforcementNameAndAddressPage-details-save-and-return", EnforcementMultiLabel.SAVE_AND_RETURN);

    }

}
