package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import java.util.List;
import java.util.Set;

import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.EstateManagementGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.SecureContractDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.SecureContractMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

public class SecureContractGroundsForPossessionWales implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("secureOrFlexibleGroundsForPossessionWales", this::midEvent)
                .pageLabel("What are your grounds for possession? (Wales)")
                // TODO - Once HDPI-2365 is implemented the condition can be added
                // .showCondition("typeOfTenancyLicence=\"SECURE_CONTRACT\" ") 
                .label("secureOrFlexibleGroundsForPossessionWales-info", """
               ---
               <p class="govuk-body" tabindex="0">
                  You may have already given the defendants notice of your intention to begin possession proceedings.
                  If you have, you should have written the grounds you're making your claim under. You should select
                  these grounds here and any extra ground you'd like to add to your claim, if you need to.
               </p>

               <p class="govuk-body" tabindex="0">
                  <a class="govuk-link govuk-!-margin-bottom-3" href="#"  rel="noreferrer noopener" 
                  target="_blank" aria-label="More information about possession grounds (opens in new tab)">
                        More information about possession grounds (opens in new tab).
                  </a>
                </p>

               """)
                .optional(PCSCase::getSecureContractDiscretionaryGrounds)
                .optional(PCSCase::getEstateManagementGrounds,
                        "secureContractDiscretionaryGroundsCONTAINS\"ESTATE_MANAGEMENT_GROUNDS\"")
                .optional(PCSCase::getSecureContractMandatoryGrounds);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
            CaseDetails<PCSCase, State> detailsBefore) {
                
        PCSCase caseData = details.getData();

        Set<SecureContractDiscretionaryGrounds> discretionaryGrounds = caseData.getSecureContractDiscretionaryGrounds();

        Set<SecureContractMandatoryGrounds> mandatoryGrounds = caseData.getSecureContractMandatoryGrounds();

        Set<EstateManagementGrounds> estateManagement = caseData.getEstateManagementGrounds();

        if (discretionaryGrounds.contains(SecureContractDiscretionaryGrounds.ESTATE_MANAGEMENT_GROUNDS)
                && estateManagement.isEmpty()) {
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                    .errors(List.of("Please select at least one Estate management ground"))
                    .build();
        }

        if (discretionaryGrounds.isEmpty() && mandatoryGrounds.isEmpty()) {
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                    .errors(List.of("Please select at least one ground"))
                    .build();
        }
        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data(caseData)
                .build();
    }

}
