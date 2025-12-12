package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoRentArrearsDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoRentArrearsMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoRentArrearsGroundsOptions;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;

import java.util.List;
import java.util.Set;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;


@Slf4j
@Component
public class NoRentArrearsGroundsForPossessionOptions implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("noRentArrearsGroundsForPossessionOptions", this::midEvent)
            .pageLabel("What are your grounds for possession?")
            .showCondition("claimDueToRentArrears=\"No\" AND typeOfTenancyLicence=\"ASSURED_TENANCY\""
                             + " AND legislativeCountry=\"England\""
            )
            .readonly(PCSCase::getShowRentSectionPage, NEVER_SHOW)
            .complex(PCSCase::getNoRentArrearsGroundsOptions)
            .readonly(NoRentArrearsGroundsOptions::getShowGroundReasonPage, NEVER_SHOW)
            .label(
                "noRentArrearsGroundsForPossessionOptions-information", """
                    ---
                    <p>You may have already given the defendants notice of your intention to begin possession
                    proceedings. If you have, you should have written the grounds you’re making your claim under.
                    You should select these grounds here and any extra grounds you’d like to add to your claim,
                    if you need to.</p>
                    <p class="govuk-body">
                      <a href="https://england.shelter.org.uk/professional_resources/legal/possession_and_eviction/grounds_for_possession" class="govuk-link" rel="noreferrer noopener" target="_blank">More information about possession grounds (opens in new tab)</a>.
                    </p>"""
            )
            .optional(NoRentArrearsGroundsOptions::getMandatoryGrounds)
            .optional(NoRentArrearsGroundsOptions::getDiscretionaryGrounds)
            .done()
            .label("noRentArrearsGroundsForPossessionOptions-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();
        Set<NoRentArrearsMandatoryGrounds> mandatoryGrounds =
            caseData.getNoRentArrearsGroundsOptions().getMandatoryGrounds();
        Set<NoRentArrearsDiscretionaryGrounds> discretionaryGrounds =
            caseData.getNoRentArrearsGroundsOptions().getDiscretionaryGrounds();

        if (mandatoryGrounds.isEmpty() && discretionaryGrounds.isEmpty()) {
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .errors(List.of("Please select at least one ground"))
                .build();
        }

        boolean hasOtherMandatoryGrounds = mandatoryGrounds.stream()
            .anyMatch(ground -> ground
                != NoRentArrearsMandatoryGrounds.SERIOUS_RENT_ARREARS);

        boolean hasOtherDiscretionaryGrounds =  discretionaryGrounds.stream()
            .anyMatch(ground -> ground != NoRentArrearsDiscretionaryGrounds.RENT_ARREARS
                && ground != NoRentArrearsDiscretionaryGrounds.RENT_PAYMENT_DELAY);

        boolean shouldShowReasonsPage = hasOtherDiscretionaryGrounds || hasOtherMandatoryGrounds;
        caseData.getNoRentArrearsGroundsOptions()
            .setShowGroundReasonPage(YesOrNo.from(shouldShowReasonsPage));

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }
}
