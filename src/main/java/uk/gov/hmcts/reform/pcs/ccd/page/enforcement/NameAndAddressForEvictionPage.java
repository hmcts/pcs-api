package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementMultiLabel;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementOrder;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

public class NameAndAddressForEvictionPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("enforcementEvictionNameAndAddress", this::midEvent)
            .pageLabel("The name and address for the eviction")
            .readonly(PCSCase::getDefendant1, NEVER_SHOW)
            .label(
                "enforcementNameAndAddressPage-details-defendants-check",
                """
                    <hr />
                    <h2 class="govuk-heading-m">Check the name and address for the eviction</h2>
                    <div class="govuk-width-container">
                          <main class="govuk-main-wrapper">
                            <div class="govuk-grid-row">
                              <div class="govuk-grid-column-one-third">
                                <h3 class="govuk-heading-s">Defendants</h3>
                              </div>
                              <div class="govuk-grid-column-one-third">
                                <p class="govuk-body">${defendant1.firstName}</p>
                              </div>
                            </div>
                          </main>
                        </div>
                """)
            .readonly(PCSCase::getFormattedClaimantContactAddress, NEVER_SHOW)
            .label(
                "enforcementNameAndAddressPage-details-address-check",
                """
                    <hr />
                    <div class="govuk-width-container">
                          <main class="govuk-main-wrapper">
                            <div class="govuk-grid-row">
                              <div class="govuk-grid-column-one-third">
                                <h3 class="govuk-heading-s">Address</h3>
                              </div>
                              <div class="govuk-grid-column-one-third">
                                <p class="govuk-body">${formattedClaimantContactAddress}</p>
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

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(details.getData())
            .build();
    }

}
