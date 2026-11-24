package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractGroundsForPossessionWales;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.service.rules.GroundsRules;

import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractDiscretionaryGroundsWales.ESTATE_MANAGEMENT_GROUNDS_S160;
import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.WALES;

@Component
public class SecureContractGroundsForPossessionWalesPage implements CcdPageConfiguration {

    private final GroundsRules groundsRules;

    @Autowired
    public SecureContractGroundsForPossessionWalesPage() {
        this(new GroundsRules());
    }

    public SecureContractGroundsForPossessionWalesPage(GroundsRules groundsRules) {
        this.groundsRules = groundsRules;
    }

    private static final String DISCRETIONARY_GROUNDS = "secureGroundsWales_DiscretionaryGrounds";

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("secureOrFlexibleGroundsForPossessionWales", this::midEvent)
                .pageLabel("What are your grounds for possession?")
                .showCondition(
                        "occupationLicenceTypeWales=\"SECURE_CONTRACT\""
                        + " AND " + WALES
                )
                .label("secureOrFlexibleGroundsForPossessionWales-info", """
               ---
               <p>You may have already given the defendants notice of your intention to begin possession
               proceedings. If you have, you should have written the grounds you’re making your claim under.
               You should select these grounds here and any extra grounds you’d like to add to your claim,
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

        SecureContractGroundsForPossessionWales grounds = caseData.getSecureContractGroundsForPossessionWales();

        List<String> errors = groundsRules.validateSecureContractGrounds(grounds);
        if (!errors.isEmpty()) {
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                    .errorMessageOverride(errors.get(0))
                    .build();
        }

        groundsRules.deriveSecureContractGroundsRouting(grounds).ifPresent(routing -> {
            caseData.setShowASBQuestionsPageWales(routing.showAsbQuestionsPage());
            caseData.setShowReasonsForGroundsPageWales(routing.showReasonsPage());
        });

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data(caseData)
                .build();
    }

}
