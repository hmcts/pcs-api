package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementOrder;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;
import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;

public class NameAndAddressForEvictionPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("enforcementEvictionNameAndAddress")
            .pageLabel("The name and address for the eviction (placeholder)")
            .readonly(PCSCase::getDefendant1, NEVER_SHOW)
            .label(
                "enforcementEvictionNameAndAddress-details-defendants-check",
                """
                    <hr />
                    <h2 class="govuk-heading-m">Check the name and address for the eviction</h2>
                    <div class="govuk-width-container">
                          <main class="govuk-main-wrapper">
                            <div class="govuk-grid-row">
                              <div class="govuk-grid-column-one-third">
                                <h3 class="govuk-body">Defendants</h3>
                              </div>
                              <div class="govuk-grid-column-one-third">
                                <p class="govuk-body">${defendant1.firstName} ${defendant1.lastName}</p>
                              </div>
                            </div>
                          </main>
                        </div>
                """)
            .readonly(PCSCase::getFormattedPropertyAddress, NEVER_SHOW)
            .label(
                "enforcementEvictionNameAndAddress-details-address-check",
                """
                    <hr />
                    <div class="govuk-width-container">
                          <main class="govuk-main-wrapper">
                            <div class="govuk-grid-row">
                              <div class="govuk-grid-column-one-third">
                                <h3 class="govuk-body">Address</h3>
                              </div>
                              <div class="govuk-grid-column-one-third">
                                <p class="govuk-body">${formattedPropertyAddress}</p>
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
            .label("enforcementNameAndAddressPage-details-save-and-return", SAVE_AND_RETURN);

    }

}
