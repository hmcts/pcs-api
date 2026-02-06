package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.EstateManagementGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractDiscretionaryGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractGroundsForPossessionWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractMandatoryGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;

import java.util.Set;

@Component
public class SecureContractGroundsForPossessionWalesPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("secureOrFlexibleGroundsForPossessionWales", this::midEvent)
                .pageLabel("What are your grounds for possession?")
                .showCondition(
                        "occupationLicenceTypeWales=\"SECURE_CONTRACT\""
                        + " AND legislativeCountry=\"Wales\""
                )
                .label("secureOrFlexibleGroundsForPossessionWales-info", """
               ---
               <p>You may have already given the defendants notice of your intention to begin possession
               proceedings. If you have, you should have written the grounds you’re making your claim under.
               You should select these grounds here and any extra ground you’d like to add to your claim,
               if you need to.</p>
               <p class="govuk-body">
                 <a href="https://www.gov.wales/understanding-possession-action-process-guidance-tenants-contract-holders-html" class="govuk-link" rel="noreferrer noopener" target="_blank">More information about possession grounds (opens in new tab)</a>.
               </p>
               """)
            .complex(PCSCase::getSecureContractGroundsForPossessionWales)
                .optional(SecureContractGroundsForPossessionWales::getDiscretionaryGroundsWales)
                .optional(SecureContractGroundsForPossessionWales::getEstateManagementGroundsWales,
                        "secureContract_DiscretionaryGroundsWalesCONTAINS\"ESTATE_MANAGEMENT_GROUNDS\"")
                .optional(SecureContractGroundsForPossessionWales::getMandatoryGroundsWales)
                .done()
                .label("secureOrFlexibleGroundsForPossessionWales-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
            CaseDetails<PCSCase, State> detailsBefore) {

        PCSCase caseData = details.getData();

        Set<SecureContractDiscretionaryGroundsWales> discretionaryGrounds =
            caseData.getSecureContractGroundsForPossessionWales().getDiscretionaryGroundsWales();

        Set<SecureContractMandatoryGroundsWales> mandatoryGrounds = caseData
            .getSecureContractGroundsForPossessionWales().getMandatoryGroundsWales();

        Set<EstateManagementGroundsWales> estateManagement = caseData
            .getSecureContractGroundsForPossessionWales().getEstateManagementGroundsWales();

        if (discretionaryGrounds.contains(SecureContractDiscretionaryGroundsWales.ESTATE_MANAGEMENT_GROUNDS)
                && estateManagement.isEmpty()) {
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                    .errorMessageOverride(
                        "Please select at least one ground in ‘Estate management grounds (section 160)’.")
                    .build();
        }

        if (discretionaryGrounds.isEmpty() && mandatoryGrounds.isEmpty()) {
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                    .errorMessageOverride("Please select at least one ground")
                    .build();
        }
        // ASB/Reasons routing (from master - conditional logic)
        boolean hasDiscretionary = !discretionaryGrounds.isEmpty();
        boolean hasMandatory = !mandatoryGrounds.isEmpty();

        boolean hasRentArrears = discretionaryGrounds.contains(SecureContractDiscretionaryGroundsWales.RENT_ARREARS);
        boolean hasASB = discretionaryGrounds.contains(SecureContractDiscretionaryGroundsWales.ANTISOCIAL_BEHAVIOUR);
        boolean hasOtherBreach =
            discretionaryGrounds.contains(SecureContractDiscretionaryGroundsWales.OTHER_BREACH_OF_CONTRACT);
        boolean hasEstateManagement = hasDiscretionary
                && discretionaryGrounds.contains(SecureContractDiscretionaryGroundsWales.ESTATE_MANAGEMENT_GROUNDS);

        // Determine if there are "other options" (anything that's not rent arrears or ASB)
        boolean hasOtherOptions = hasOtherBreach || hasEstateManagement || hasMandatory;

        // Routing rules based on options selected
        if (hasRentArrears && !hasASB && !hasOtherOptions) {
            caseData.setShowASBQuestionsPageWales(YesOrNo.NO);
            caseData.setShowReasonsForGroundsPageWales(YesOrNo.NO);
        } else if (hasASB && !hasOtherOptions) {
            caseData.setShowASBQuestionsPageWales(YesOrNo.YES);
            caseData.setShowReasonsForGroundsPageWales(YesOrNo.NO);
        } else if (hasOtherOptions) {
            caseData.setShowASBQuestionsPageWales(YesOrNo.NO);
            caseData.setShowReasonsForGroundsPageWales(YesOrNo.YES);
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data(caseData)
                .build();
    }

}
