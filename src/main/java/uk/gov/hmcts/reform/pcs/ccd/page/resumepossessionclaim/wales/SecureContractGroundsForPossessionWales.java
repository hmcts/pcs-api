package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales;

import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Set;

import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.EstateManagementGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractDiscretionaryGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractMandatoryGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.service.routing.wales.WalesRentDetailsRoutingService;

@Component
public class SecureContractGroundsForPossessionWales implements CcdPageConfiguration {

    private final WalesRentDetailsRoutingService walesRentDetailsRoutingService;

    public SecureContractGroundsForPossessionWales(WalesRentDetailsRoutingService walesRentDetailsRoutingService) {
        this.walesRentDetailsRoutingService = walesRentDetailsRoutingService;
    }

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
               proceedings. If you have, you should have written the grounds you're making your claim under.
               You should select these grounds here and any extra ground you'd like to add to your claim,
               if you need to.</p>
               <p class="govuk-body">
                 <a href="https://www.gov.wales/understanding-possession-action-process-guidance-tenants-contract-holders-html" class="govuk-link" rel="noreferrer noopener" target="_blank">More information about possession grounds (opens in new tab)</a>
               </p>
               """)
                .optional(PCSCase::getSecureContractDiscretionaryGroundsWales)
                .optional(PCSCase::getSecureContractEstateManagementGroundsWales,
                        "secureContractDiscretionaryGroundsWalesCONTAINS\"ESTATE_MANAGEMENT_GROUNDS\"")
                .optional(PCSCase::getSecureContractMandatoryGroundsWales)
                .label("secureOrFlexibleGroundsForPossessionWales-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
            CaseDetails<PCSCase, State> detailsBefore) {
                
        PCSCase caseData = details.getData();

        Set<SecureContractDiscretionaryGroundsWales> discretionaryGrounds = 
                                                caseData.getSecureContractDiscretionaryGroundsWales();

        Set<SecureContractMandatoryGroundsWales> mandatoryGrounds = caseData.getSecureContractMandatoryGroundsWales();

        Set<EstateManagementGroundsWales> estateManagement = caseData.getSecureContractEstateManagementGroundsWales();

        if (discretionaryGrounds.contains(SecureContractDiscretionaryGroundsWales.ESTATE_MANAGEMENT_GROUNDS)
                && estateManagement.isEmpty()) {
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                    .errors(List.of("Please select at least one ground in 'Estate management grounds (section 160)'."))
                    .build();
        }

        if (discretionaryGrounds.isEmpty() && mandatoryGrounds.isEmpty()) {
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                    .errors(List.of("Please select at least one ground"))
                    .build();
        }

        // Rent details routing (from HEAD - using routing service)
        caseData.setShowRentDetailsPage(walesRentDetailsRoutingService.shouldShowRentDetails(caseData));

        // ASB/Reasons routing (from master - conditional logic)
        boolean hasDiscretionary = discretionaryGrounds != null && !discretionaryGrounds.isEmpty();
        boolean hasMandatory = mandatoryGrounds != null && !mandatoryGrounds.isEmpty();

        boolean hasRentArrears = hasDiscretionary
                && discretionaryGrounds.contains(SecureContractDiscretionaryGroundsWales.RENT_ARREARS);
        boolean hasASB = discretionaryGrounds != null
                && discretionaryGrounds.contains(SecureContractDiscretionaryGroundsWales.ANTISOCIAL_BEHAVIOUR);
        boolean hasOtherBreach = hasDiscretionary
                && discretionaryGrounds.contains(SecureContractDiscretionaryGroundsWales.OTHER_BREACH_OF_CONTRACT);
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