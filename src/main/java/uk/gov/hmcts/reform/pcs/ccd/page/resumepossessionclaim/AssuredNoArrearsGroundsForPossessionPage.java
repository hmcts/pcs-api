package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredAdditionalOtherGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredDiscretionaryGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredMandatoryGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredNoArrearsPossessionGrounds;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;


@Slf4j
@AllArgsConstructor
@Component
public class AssuredNoArrearsGroundsForPossessionPage implements CcdPageConfiguration {

    private final TextAreaValidationService textAreaValidationService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("assuredNoArrearsGroundsForPossession", this::midEvent)
            .pageLabel("What are your grounds for possession?")
            .showCondition("claimDueToRentArrears=\"NO\" AND tenancy_TypeOfTenancyLicence=\"ASSURED_TENANCY\""
                             + " AND legislativeCountry=\"England\""
            )
            .readonly(PCSCase::getShowRentSectionPage, NEVER_SHOW)
            .complex(PCSCase::getNoRentArrearsGroundsOptions)
            .readonly(AssuredNoArrearsPossessionGrounds::getShowGroundReasonPage, NEVER_SHOW)
            .label(
                "assuredNoArrearsGroundsForPossession-information", """
                    ---
                    <p>You may have already given the defendants notice of your intention to begin possession
                    proceedings. If you have, you should have written the grounds you’re making your claim under.
                    You should select these grounds here and any extra grounds you’d like to add to your claim,
                    if you need to.</p>
                    <p class="govuk-body">
                      <a href="https://england.shelter.org.uk/professional_resources/legal/possession_and_eviction/grounds_for_possession" class="govuk-link" rel="noreferrer noopener" target="_blank">More information about possession grounds (opens in new tab)</a>.
                    </p>"""
            )
            .optional(AssuredNoArrearsPossessionGrounds::getMandatoryGrounds)
            .optional(AssuredNoArrearsPossessionGrounds::getDiscretionaryGrounds)
            .optional(AssuredNoArrearsPossessionGrounds::getOtherGround)
            .mandatory(AssuredNoArrearsPossessionGrounds::getOtherGroundDescription,
                    "noRentArrears_"
                            + "OtherGroundCONTAINS\"OTHER\"")
            .done()
            .label("assuredNoArrearsGroundsForPossession-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();
        AssuredNoArrearsPossessionGrounds groundsForPossession = caseData.getNoRentArrearsGroundsOptions();

        Set<AssuredMandatoryGround> mandatoryGrounds =
            caseData.getNoRentArrearsGroundsOptions().getMandatoryGrounds();
        Set<AssuredDiscretionaryGround> discretionaryGrounds =
            caseData.getNoRentArrearsGroundsOptions().getDiscretionaryGrounds();
        Set<AssuredAdditionalOtherGround> additionalOtherGrounds
                = groundsForPossession.getOtherGround();

        if (CollectionUtils.isEmpty(mandatoryGrounds) && CollectionUtils.isEmpty(discretionaryGrounds)
                && CollectionUtils.isEmpty(additionalOtherGrounds)) {
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .errorMessageOverride("Please select at least one ground")
                .build();
        }

        boolean hasOtherMandatoryGrounds = !CollectionUtils.isEmpty(mandatoryGrounds)
                && mandatoryGrounds.stream()
            .anyMatch(ground -> ground
                != AssuredMandatoryGround.SERIOUS_RENT_ARREARS_GROUND8);

        boolean hasOtherDiscretionaryGrounds =  !CollectionUtils.isEmpty(discretionaryGrounds)
                && discretionaryGrounds.stream()
            .anyMatch(ground -> ground != AssuredDiscretionaryGround.RENT_ARREARS_GROUND10
                && ground != AssuredDiscretionaryGround.PERSISTENT_DELAY_GROUND11);

        boolean shouldShowReasonsPage = hasOtherDiscretionaryGrounds || hasOtherMandatoryGrounds;
        caseData.getNoRentArrearsGroundsOptions()
            .setShowGroundReasonPage(YesOrNo.from(shouldShowReasonsPage));

        if (!CollectionUtils.isEmpty(additionalOtherGrounds)
                && groundsForPossession.getOtherGroundDescription() != null) {
            List<String> validationErrors = new ArrayList<>();

            validationErrors.addAll(textAreaValidationService.validateSingleTextArea(
                    caseData.getNoRentArrearsGroundsOptions().getOtherGroundDescription(),
                    PCSCase.OTHER_GROUND_DESCRIPTION_LABEL,
                    TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ));
            if (!validationErrors.isEmpty()) {
                return textAreaValidationService.createValidationResponse(caseData, validationErrors);
            }
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }
}
