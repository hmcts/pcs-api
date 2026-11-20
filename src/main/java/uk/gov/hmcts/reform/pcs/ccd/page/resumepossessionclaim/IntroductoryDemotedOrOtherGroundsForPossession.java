package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.IntroductoryDemotedOtherGroundsForPossession;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;
import uk.gov.hmcts.reform.pcs.ccd.service.rules.GroundsRules;

import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;
import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.ENGLAND;

@AllArgsConstructor
@Component
public class IntroductoryDemotedOrOtherGroundsForPossession implements CcdPageConfiguration {

    private final TextAreaValidationService textAreaValidationService;
    private final GroundsRules groundsRules;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("introductoryDemotedOrOtherGroundsForPossession", this::midEvent)
            .pageLabel("Grounds for possession")
            .showCondition(
                "tenancy_TypeOfTenancyLicence=\"INTRODUCTORY_TENANCY\" "
                  + "OR tenancy_TypeOfTenancyLicence=\"DEMOTED_TENANCY\" "
                  + "OR tenancy_TypeOfTenancyLicence=\"OTHER\""
                  + " AND " + ENGLAND)
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
                         You should select these grounds here and any extra grounds you’d like to add to your claim,
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

        IntroductoryDemotedOtherGroundsForPossession grounds =
            caseData.getIntroductoryDemotedOrOtherGroundsForPossession();

        List<String> validationErrors = groundsRules.validateIntroductoryDemotedOrOtherGrounds(grounds);

        caseData.setShowIntroductoryDemotedOtherGroundReasonPage(
            groundsRules.showReasonsForIntroductoryDemotedOrOtherGrounds(grounds));

        if (!validationErrors.isEmpty()) {
            return textAreaValidationService.createValidationResponse(caseData, validationErrors);
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }

}
