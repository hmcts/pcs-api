package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.IntroductoryDemotedOtherGroundReason;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.service.TextValidationService;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Component
public class IntroductoryDemotedOtherGroundsReasons implements CcdPageConfiguration {

    private final TextValidationService textValidationService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("introductoryDemotedOtherGroundsReasons", this::midEvent)
            .pageLabel("Reasons for possession ")
            .showCondition("showIntroductoryDemotedOtherGroundReasonPage=\"Yes\""
                    + " AND (tenancy_TypeOfTenancyLicence=\"INTRODUCTORY_TENANCY\""
                    + " OR tenancy_TypeOfTenancyLicence=\"DEMOTED_TENANCY\""
                    +  " OR tenancy_TypeOfTenancyLicence=\"OTHER\")"
                    + " AND legislativeCountry=\"England\"")
            .complex(PCSCase::getIntroductoryDemotedOtherGroundReason)
            .label("introductoryDemotedOtherGroundsReasons-antiSocial-label","""
                ---
                <h2 class="govuk-heading-l" tabindex="0">Antisocial behaviour</h2>
                <h3 class="govuk-heading-m" tabindex="0" >
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "introGrounds_"
                + "IntroductoryDemotedOrOtherGroundsCONTAINS\"ANTI_SOCIAL\"")
            .mandatory(IntroductoryDemotedOtherGroundReason::getAntiSocialBehaviourGround,
                    "introGrounds_"
                        + "IntroductoryDemotedOrOtherGroundsCONTAINS\"ANTI_SOCIAL\"")

            .label("introductoryDemotedOtherGroundsReasons-breachOfTenancy-label","""
                ---
                <h2 class="govuk-heading-l" tabindex="0">Breach of the tenancy</h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                ""","introGrounds_"
                + "IntroductoryDemotedOrOtherGroundsCONTAINS\"BREACH_OF_THE_TENANCY\"")
            .mandatory(IntroductoryDemotedOtherGroundReason::getBreachOfTheTenancyGround,
                    "introGrounds_"
                        + "IntroductoryDemotedOrOtherGroundsCONTAINS\"BREACH_OF_THE_TENANCY\"")

            .label("introductoryDemotedOtherGroundsReasons-absoluteGrounds-label","""
                ---
                <h2 class="govuk-heading-l" tabindex="0">Absolute grounds</h2>
                <h3 class="govuk-heading-m" tabindex="0"> Why are you claiming possession?</h3>
                ""","introGrounds_"
                + "IntroductoryDemotedOrOtherGroundsCONTAINS\"ABSOLUTE_GROUNDS\"")
            .mandatory(IntroductoryDemotedOtherGroundReason::getAbsoluteGrounds,
                    "introGrounds_"
                        + "IntroductoryDemotedOrOtherGroundsCONTAINS\"ABSOLUTE_GROUNDS\"")

            .label("introductoryDemotedOtherGroundsReasons-otherGround-label","""
                ---
                <h2 class="govuk-heading-l" tabindex="0">Other grounds</h2>
                <h3 class="govuk-heading-m" tabindex="0"> Why are you claiming possession?</h3>
                ""","introGrounds_"
                + "IntroductoryDemotedOrOtherGroundsCONTAINS\"OTHER\"")
            .mandatory(IntroductoryDemotedOtherGroundReason::getOtherGround,
                    "introGrounds_"
                        + "IntroductoryDemotedOrOtherGroundsCONTAINS\"OTHER\"")
            .label("introductoryDemotedOtherGroundsReasons-noGrounds-label","""
                ---
                <h2 class="govuk-heading-l" tabindex="0">No grounds</h2>
                <h3 class="govuk-heading-m" tabindex="0"> Why are you claiming possession?</h3>
                ""","introGrounds_"
                + "HasIntroductoryDemotedOtherGroundsForPossession=\"NO\"")
            .mandatory(IntroductoryDemotedOtherGroundReason::getNoGrounds,
                        "introGrounds_"
                            + "HasIntroductoryDemotedOtherGroundsForPossession=\"NO\"")
            .done()
            .label("introductoryDemotedOtherGroundsReasons-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);

    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();

        // Validate all text area fields for character limit - ultra simple approach
        List<String> validationErrors = new ArrayList<>();

        IntroductoryDemotedOtherGroundReason introductoryDemotedOtherGroundReason =
            caseData.getIntroductoryDemotedOtherGroundReason();
        if (introductoryDemotedOtherGroundReason != null) {
            validationErrors.addAll(textValidationService.validateMultipleTextAreas(
                TextValidationService.FieldValidation.of(
                    introductoryDemotedOtherGroundReason.getAntiSocialBehaviourGround(),
                    "Antisocial behaviour",
                    TextValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextValidationService.FieldValidation.of(
                    introductoryDemotedOtherGroundReason.getBreachOfTheTenancyGround(),
                    "Breach of the tenancy",
                    TextValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextValidationService.FieldValidation.of(
                    introductoryDemotedOtherGroundReason.getAbsoluteGrounds(),
                    "Absolute grounds",
                    TextValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextValidationService.FieldValidation.of(
                    introductoryDemotedOtherGroundReason.getOtherGround(),
                    "Other grounds",
                    TextValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextValidationService.FieldValidation.of(
                    introductoryDemotedOtherGroundReason.getNoGrounds(),
                    "No grounds",
                    TextValidationService.MEDIUM_TEXT_LIMIT
                )
            ));
        }

        return textValidationService.createValidationResponse(caseData, validationErrors);
    }
}
