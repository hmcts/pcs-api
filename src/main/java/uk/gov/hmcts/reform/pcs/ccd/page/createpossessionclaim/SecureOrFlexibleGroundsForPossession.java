package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.SecureOrFlexibleDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm;
import uk.gov.hmcts.reform.pcs.ccd.domain.SecureOrFlexibleMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.SecureOrFlexibleMandatoryGroundsAlternativeAccomm;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import java.util.List;
import java.util.Set;

import static uk.gov.hmcts.reform.pcs.ccd.domain.SecureOrFlexibleDiscretionaryGrounds.RENT_ARREARS_OR_BREACH_OF_TENANCY;

public class SecureOrFlexibleGroundsForPossession implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("secureOrFlexibleGroundsForPossession", this::midEvent)
            .pageLabel("What are your grounds for possession")
            .showCondition("typeOfTenancyLicence=\"SECURE_TENANCY\" OR typeOfTenancyLicence=\"FLEXIBLE_TENANCY\"")
            .label("secureOrFlexibleGroundsForPossession-info", """
               ---
               <p class="govuk-body"> You may have already given the defendants notice of your intention to begin
                possession proceedings.
                If you have, you should have written the grounds you're making your claim under. You should select these
                grounds here and any extra ground you'd like to add to your claim, if you need to.
               </p>
               """)
            .optional(PCSCase::getSecureOrFlexibleDiscretionaryGrounds)
            .optional(PCSCase::getSecureOrFlexibleMandatoryGrounds)
            .optional(PCSCase::getSecureOrFlexibleMandatoryGroundsAlt)
            .optional(PCSCase::getSecureOrFlexibleDiscretionaryGroundsAlt);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();

        Set<SecureOrFlexibleDiscretionaryGrounds> discretionaryGrounds =
                caseData.getSecureOrFlexibleDiscretionaryGrounds();

        Set<SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm> discretionaryGroundsAlt =
                caseData.getSecureOrFlexibleDiscretionaryGroundsAlt();

        Set<SecureOrFlexibleMandatoryGrounds> mandatoryGrounds = caseData.getSecureOrFlexibleMandatoryGrounds();

        Set<SecureOrFlexibleMandatoryGroundsAlternativeAccomm> mandatoryGroundsAlt =
                caseData.getSecureOrFlexibleMandatoryGroundsAlt();

        boolean hasOtherDiscretionaryGrounds = discretionaryGrounds
                .stream()
                .anyMatch(ground -> ground != RENT_ARREARS_OR_BREACH_OF_TENANCY
                );

        if (!discretionaryGrounds.contains(RENT_ARREARS_OR_BREACH_OF_TENANCY)) {
            caseData.setRentArrearsOrBreachOfTenancy(Set.of());
        }

        if (hasOtherDiscretionaryGrounds
               || !discretionaryGroundsAlt.isEmpty()
               || !mandatoryGrounds.isEmpty()
               || !mandatoryGroundsAlt.isEmpty()
        ) {
            caseData.setShowReasonsForGroundsPage(YesOrNo.YES);
        } else {
            caseData.setShowReasonsForGroundsPage(YesOrNo.NO);
        }

        if (discretionaryGrounds.isEmpty() && discretionaryGroundsAlt.isEmpty() && mandatoryGrounds.isEmpty()
            && mandatoryGroundsAlt.isEmpty()) {
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                    .errors(List.of("Please select at least one ground"))
                    .build();
        }
        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }

}
