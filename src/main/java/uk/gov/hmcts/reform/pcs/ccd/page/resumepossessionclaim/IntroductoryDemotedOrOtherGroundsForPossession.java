package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.IntroductoryDemotedOrOtherGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.IntroductoryDemotedOtherGroundsForPossession;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

@AllArgsConstructor
@Component
public class IntroductoryDemotedOrOtherGroundsForPossession implements CcdPageConfiguration {

    private final TextAreaValidationService textAreaValidationService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("introductoryDemotedOrOtherGroundsForPossession", this::midEvent)
            .pageLabel("Grounds for possession")
            .showCondition(
                "tenancy_TypeOfTenancyLicence=\"INTRODUCTORY_TENANCY\" "
                  + "OR tenancy_TypeOfTenancyLicence=\"DEMOTED_TENANCY\" "
                  + "OR tenancy_TypeOfTenancyLicence=\"OTHER\""
                  + " AND legislativeCountry=\"England\"")
            .readonly(PCSCase::getShowIntroductoryDemotedOtherGroundReasonPage, NEVER_SHOW)
            .complex(PCSCase::getIntroductoryDemotedOrOtherGroundsForPossession)
                .label(
                    "introductoryDemotedOrOtherGroundsForPossession-info",
                      """
                       ---
                       <p class="govuk-body" tabindex="0">In some cases, a claimant can make a claim for possession of a
                        property without having to rely on a specific ground. If your claim meets these
                       requirements, you can select that you have no grounds for possession.

                       You may have already given the defendants notice of your intention to begin
                        possession proceedings.
                        If you have, you should have written the grounds you’re making your claim under.
                         You should select these grounds here and any extra ground you’d like to add to your claim,
                         if you need to.
                       </p>
                       <p class="govuk-body">
                        <a href="https://england.shelter.org.uk/professional_resources/legal/possession_and_eviction/grounds_for_possession" class="govuk-link" rel="noreferrer noopener" target="_blank">More information about possession grounds (opens in new tab)</a>.
                       </p>
                       """)
                .mandatory(
                    IntroductoryDemotedOtherGroundsForPossession::getHasIntroductoryDemotedOtherGroundsForPossession)
                .mandatory(IntroductoryDemotedOtherGroundsForPossession::getIntroductoryDemotedOrOtherGrounds,
                    "introGrounds_"
                        + "HasIntroductoryDemotedOtherGroundsForPossession=\"YES\"")
                .mandatory(IntroductoryDemotedOtherGroundsForPossession::getOtherGroundDescription,
                            "introGrounds_"
                                + "IntroductoryDemotedOrOtherGroundsCONTAINS\"OTHER\""
                            + "AND introGrounds_"
                                + "HasIntroductoryDemotedOtherGroundsForPossession=\"YES\"")
                .done()
            .label(
                "introductoryDemotedOrOtherGroundsForPossession-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();

        List<String> validationErrors = new ArrayList<>();

        if (caseData.getIntroductoryDemotedOrOtherGroundsForPossession().getOtherGroundDescription() != null) {
            validationErrors.addAll(textAreaValidationService.validateSingleTextArea(
                caseData.getIntroductoryDemotedOrOtherGroundsForPossession().getOtherGroundDescription(),
                PCSCase.OTHER_GROUND_DESCRIPTION_LABEL,
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ));
        }

        boolean hasOtherDiscretionaryGrounds =
            Optional.ofNullable(caseData.getIntroductoryDemotedOrOtherGroundsForPossession()
                                    .getIntroductoryDemotedOrOtherGrounds())
                .stream()
                .flatMap(Collection::stream)
                .anyMatch(grounds -> grounds
                    != IntroductoryDemotedOrOtherGrounds.RENT_ARREARS);

        if (hasOtherDiscretionaryGrounds
            || caseData.getIntroductoryDemotedOrOtherGroundsForPossession()
                .getHasIntroductoryDemotedOtherGroundsForPossession()
            == VerticalYesNo.NO) {
            caseData.setShowIntroductoryDemotedOtherGroundReasonPage(YesOrNo.YES);
        } else {
            caseData.setShowIntroductoryDemotedOtherGroundReasonPage(YesOrNo.NO);
        }

        if (!validationErrors.isEmpty()) {
            return textAreaValidationService.createValidationResponse(caseData, validationErrors);
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }

}
