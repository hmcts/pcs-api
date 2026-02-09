package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
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

import static uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractDiscretionaryGroundsWales.ANTISOCIAL_BEHAVIOUR_S157;
import static uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractDiscretionaryGroundsWales.ESTATE_MANAGEMENT_GROUNDS_S160;
import static uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractDiscretionaryGroundsWales.OTHER_BREACH_OF_CONTRACT_S157;
import static uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractDiscretionaryGroundsWales.RENT_ARREARS_S157;

@Component
public class SecureContractGroundsForPossessionWalesPage implements CcdPageConfiguration {

    private static final String DISCRETIONARY_GROUNDS = "secureGroundsWales_DiscretionaryGrounds";

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
                .optional(SecureContractGroundsForPossessionWales::getDiscretionaryGrounds)
                .optional(SecureContractGroundsForPossessionWales::getEstateManagementGrounds,
                          ShowConditions.fieldContains(DISCRETIONARY_GROUNDS,
                                               ESTATE_MANAGEMENT_GROUNDS_S160
                          )
                )
                .optional(SecureContractGroundsForPossessionWales::getMandatoryGrounds)
                .done()
                .label("secureOrFlexibleGroundsForPossessionWales-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
            CaseDetails<PCSCase, State> detailsBefore) {

        PCSCase caseData = details.getData();

        Set<SecureContractDiscretionaryGroundsWales> discretionaryGrounds =
            caseData.getSecureContractGroundsForPossessionWales().getDiscretionaryGrounds();

        Set<SecureContractMandatoryGroundsWales> mandatoryGrounds = caseData
            .getSecureContractGroundsForPossessionWales().getMandatoryGrounds();

        Set<EstateManagementGroundsWales> estateManagement = caseData
            .getSecureContractGroundsForPossessionWales().getEstateManagementGrounds();

        if (discretionaryGrounds.contains(ESTATE_MANAGEMENT_GROUNDS_S160)
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

        boolean hasRentArrears = discretionaryGrounds.contains(RENT_ARREARS_S157);
        boolean hasASB = discretionaryGrounds.contains(ANTISOCIAL_BEHAVIOUR_S157);
        boolean hasOtherBreach = discretionaryGrounds.contains(OTHER_BREACH_OF_CONTRACT_S157);
        boolean hasEstateManagement = hasDiscretionary && discretionaryGrounds.contains(ESTATE_MANAGEMENT_GROUNDS_S160);

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
