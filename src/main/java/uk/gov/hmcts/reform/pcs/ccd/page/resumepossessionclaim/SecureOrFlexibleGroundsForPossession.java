package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexiblePossessionGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.service.rules.GroundsRules;

import java.util.List;
import java.util.Set;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.ENGLAND;

public class SecureOrFlexibleGroundsForPossession implements CcdPageConfiguration {

    private final GroundsRules groundsRules;

    public SecureOrFlexibleGroundsForPossession() {
        this(new GroundsRules());
    }

    public SecureOrFlexibleGroundsForPossession(GroundsRules groundsRules) {
        this.groundsRules = groundsRules;
    }

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("secureOrFlexibleGroundsForPossession", this::midEvent)
            .pageLabel("What are your grounds for possession?")
            .showCondition("(tenancy_TypeOfTenancyLicence=\"SECURE_TENANCY\""
                        + " OR tenancy_TypeOfTenancyLicence=\"FLEXIBLE_TENANCY\")"
                        + " AND " + ENGLAND)
            .label("secureOrFlexibleGroundsForPossession-info", """
               ---
               <p class="govuk-body" tabindex="0">
                  You may have already given the defendants notice of your intention to begin possession proceedings.
                  If you have, you should have written the grounds you’re making your claim under. You should select
                  these grounds here and any extra grounds you’d like to add to your claim, if you need to.
               </p>
               <p class="govuk-body">
                 <a href="https://england.shelter.org.uk/professional_resources/legal/possession_and_eviction/grounds_for_possession" class="govuk-link" rel="noreferrer noopener" target="_blank">More information about possession grounds (opens in new tab)</a>.
               </p>
               """)
            .complex(PCSCase::getSecureOrFlexiblePossessionGrounds)
            .optional(SecureOrFlexiblePossessionGrounds::getSecureOrFlexibleDiscretionaryGrounds)
            .optional(SecureOrFlexiblePossessionGrounds::getSecureOrFlexibleMandatoryGrounds)
            .mandatory(SecureOrFlexiblePossessionGrounds::getSecureAntisocialAdditionalGrounds,
                    ShowConditions.fieldContains("secureOrFlexibleMandatoryGrounds",
                            SecureOrFlexibleMandatoryGrounds.ANTI_SOCIAL))
            .optional(SecureOrFlexiblePossessionGrounds::getSecureOrFlexibleMandatoryGroundsAlt)
            .optional(SecureOrFlexiblePossessionGrounds::getSecureOrFlexibleDiscretionaryGroundsAlt)
            .done()
            .label("secureOrFlexibleGroundsForPossession-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();
        SecureOrFlexiblePossessionGrounds grounds = caseData.getSecureOrFlexiblePossessionGrounds();

        List<String> errors = groundsRules.validateSecureOrFlexibleGrounds(grounds);
        if (!errors.isEmpty()) {
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .errorMessageOverride(errors.get(0))
                .build();
        }

        if (!groundsRules.isRentArrearsOrBreachOfTenancyGroundSelected(grounds)) {
            // Ground 1 not selected - clear rent arrears data
            caseData.setRentArrearsOrBreachOfTenancy(Set.of());
        }
        caseData.setShowReasonsForGroundsPage(groundsRules.showReasonsForSecureOrFlexibleGrounds(grounds));
        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }
}
