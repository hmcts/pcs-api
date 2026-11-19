package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredNoArrearsPossessionGrounds;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.service.rules.GroundsRules;
import uk.gov.hmcts.reform.pcs.ccd.util.StringUtils;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;
import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.ENGLAND;

@Slf4j
@AllArgsConstructor
@Component
public class AssuredNoArrearsGroundsForPossessionPage implements CcdPageConfiguration {

    private final GroundsRules groundsRules;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("assuredNoArrearsGroundsForPossession", this::midEvent)
            .pageLabel("What are your grounds for possession?")
            .showCondition("claimDueToRentArrears=\"No\" AND tenancy_TypeOfTenancyLicence=\"ASSURED_TENANCY\""
                             + " AND " + ENGLAND
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

        if (!groundsRules.hasAtLeastOneAssuredNoArrearsGround(groundsForPossession)) {
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .errorMessageOverride(GroundsRules.AT_LEAST_ONE_GROUND_ERROR)
                .build();
        }

        groundsForPossession.setShowGroundReasonPage(
            groundsRules.showReasonsForAssuredNoArrearsGrounds(groundsForPossession));

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .errorMessageOverride(StringUtils.joinIfNotEmpty("\n", groundsRules.validateOtherGroundDescription(
                groundsForPossession.getOtherGroundDescription())))
            .build();
    }
}
