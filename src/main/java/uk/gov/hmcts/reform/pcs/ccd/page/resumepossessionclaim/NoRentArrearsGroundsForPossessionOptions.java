package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import lombok.AllArgsConstructor;
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

import java.util.List;
import java.util.Set;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;


@AllArgsConstructor
@Component
@Slf4j
public class NoRentArrearsGroundsForPossessionOptions implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("noRentArrearsGroundsForPossessionOptions", this::midEvent)
            .pageLabel("What are your grounds for possession?")
            .showCondition("groundsForPossession=\"No\" AND typeOfTenancyLicence=\"ASSURED_TENANCY\"")
            .readonly(PCSCase::getShowNoRentArrearsGroundReasonPage, NEVER_SHOW)
            .readonly(PCSCase::getShowRentDetailsPage, NEVER_SHOW)
            .label(
                "NoRentArrearsGroundsForPossessionOptions-information", """
                    ---
                    <p>You may have already given the defendants notice of your intention to begin possession
                    proceedings. If you have, you should have written the grounds you’re making your claim under.
                    You should select these grounds here and any extra grounds you’d like to add to your claim,
                    if you need to.</p>"""
            )
            .optional(PCSCase::getNoRentArrearsMandatoryGroundsOptions)
            .optional(PCSCase::getNoRentArrearsDiscretionaryGroundsOptions);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();
        Set<NoRentArrearsMandatoryGrounds> mandatoryGrounds = caseData.getNoRentArrearsMandatoryGroundsOptions();
        Set<NoRentArrearsDiscretionaryGrounds> discretionaryGrounds =
            caseData.getNoRentArrearsDiscretionaryGroundsOptions();

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
        caseData.setShowNoRentArrearsGroundReasonPage(YesOrNo.from(shouldShowReasonsPage));

        // Determine if Rent Details page should be shown (HDPI-2123)
        // Show rent details if ground 8, 10, or 11 is selected
        boolean hasRentRelatedGrounds = mandatoryGrounds.contains(NoRentArrearsMandatoryGrounds.SERIOUS_RENT_ARREARS)
            || discretionaryGrounds.contains(NoRentArrearsDiscretionaryGrounds.RENT_ARREARS)
            || discretionaryGrounds.contains(NoRentArrearsDiscretionaryGrounds.RENT_PAYMENT_DELAY);
        caseData.setShowRentDetailsPage(YesOrNo.from(hasRentRelatedGrounds));

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }
}
