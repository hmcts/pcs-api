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
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Component
public class IntroductoryDemotedOtherGroundsReasons implements CcdPageConfiguration {

    private final TextAreaValidationService textAreaValidationService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("introductoryDemotedOtherGroundsReasons", this::midEvent)
            .pageLabel("Reasons for possession ")
            .showCondition("showIntroductoryDemotedOtherGroundReasonPage=\"Yes\""
                    + " AND (typeOfTenancyLicence=\"INTRODUCTORY_TENANCY\""
                    + " OR typeOfTenancyLicence=\"DEMOTED_TENANCY\""
                    +  " OR typeOfTenancyLicence=\"OTHER\")")
            .complex(PCSCase::getIntroductoryDemotedOtherGroundReason)
            .label("introductoryDemotedOtherGroundsReasons-antiSocial-label","""
                ---
                <h2 class="govuk-heading-l" tabindex="0">Antisocial behaviour</h2>
                <h3 class="govuk-heading-m" tabindex="0" >
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "introductoryDemotedOrOtherGroundsCONTAINS\"ANTI_SOCIAL\"")
            .mandatory(IntroductoryDemotedOtherGroundReason::getAntiSocialBehaviourGround,
                    "introductoryDemotedOrOtherGroundsCONTAINS\"ANTI_SOCIAL\"")

            .label("introductoryDemotedOtherGroundsReasons-breachOfTenancy-label","""
                ---
                <h2 class="govuk-heading-l" tabindex="0">Breach of the tenancy</h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                ""","introductoryDemotedOrOtherGroundsCONTAINS\"BREACH_OF_THE_TENANCY\"")
            .mandatory(IntroductoryDemotedOtherGroundReason::getBreachOfTheTenancyGround,
                    "introductoryDemotedOrOtherGroundsCONTAINS\"BREACH_OF_THE_TENANCY\"")

            .label("introductoryDemotedOtherGroundsReasons-absoluteGrounds-label","""
                ---
                <h2 class="govuk-heading-l" tabindex="0">Absolute grounds</h2>
                <h3 class="govuk-heading-m" tabindex="0"> Why are you claiming possession?</h3>
                ""","introductoryDemotedOrOtherGroundsCONTAINS\"ABSOLUTE_GROUNDS\"")
            .mandatory(IntroductoryDemotedOtherGroundReason::getAbsoluteGrounds,
                    "introductoryDemotedOrOtherGroundsCONTAINS\"ABSOLUTE_GROUNDS\"")

            .label("introductoryDemotedOtherGroundsReasons-otherGround-label","""
                ---
                <h2 class="govuk-heading-l" tabindex="0">Other grounds</h2>
                <h3 class="govuk-heading-m" tabindex="0"> Why are you claiming possession?</h3>
                ""","introductoryDemotedOrOtherGroundsCONTAINS\"OTHER\"")
            .mandatory(IntroductoryDemotedOtherGroundReason::getOtherGround,
                    "introductoryDemotedOrOtherGroundsCONTAINS\"OTHER\"")
            .label("introductoryDemotedOtherGroundsReasons-noGrounds-label","""
                ---
                <h2 class="govuk-heading-l" tabindex="0">No grounds</h2>
                <h3 class="govuk-heading-m" tabindex="0"> Why are you claiming possession?</h3>
                ""","hasIntroductoryDemotedOtherGroundsForPossession=\"NO\"")
            .mandatory(IntroductoryDemotedOtherGroundReason::getNoGrounds,
                       "hasIntroductoryDemotedOtherGroundsForPossession=\"NO\"")
            .done();

    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();
        
        // Validate all text area fields for character limit - ultra simple approach
        List<String> validationErrors = new ArrayList<>();
        
        IntroductoryDemotedOtherGroundReason introductoryDemotedOtherGroundReason = 
            caseData.getIntroductoryDemotedOtherGroundReason();
        if (introductoryDemotedOtherGroundReason != null) {
            validationErrors.addAll(textAreaValidationService.validateMultipleTextAreas(
                TextAreaValidationService.FieldValidation.of(
                    introductoryDemotedOtherGroundReason.getAntiSocialBehaviourGround(),
                    "Antisocial behaviour",
                    TextAreaValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextAreaValidationService.FieldValidation.of(
                    introductoryDemotedOtherGroundReason.getBreachOfTheTenancyGround(),
                    "Breach of the tenancy",
                    TextAreaValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextAreaValidationService.FieldValidation.of(
                    introductoryDemotedOtherGroundReason.getAbsoluteGrounds(),
                    "Absolute grounds",
                    TextAreaValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextAreaValidationService.FieldValidation.of(
                    introductoryDemotedOtherGroundReason.getOtherGround(),
                    "Other grounds",
                    TextAreaValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextAreaValidationService.FieldValidation.of(
                    introductoryDemotedOtherGroundReason.getNoGrounds(),
                    "No grounds",
                    TextAreaValidationService.MEDIUM_TEXT_LIMIT
                )
            ));
        }
        
        return textAreaValidationService.createValidationResponse(caseData, validationErrors);
    }
}
