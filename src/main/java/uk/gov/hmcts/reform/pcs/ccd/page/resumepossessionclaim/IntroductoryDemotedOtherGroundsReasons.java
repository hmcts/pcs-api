package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.ShowCondition;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.IntroductoryDemotedOtherGroundReason;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.IntroductoryDemotedOrOtherGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.IntroductoryDemotedOtherGroundsForPossession;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.ccd.sdk.api.ShowCondition.ref;
import static uk.gov.hmcts.ccd.sdk.api.ShowCondition.when;

@AllArgsConstructor
@Component
public class IntroductoryDemotedOtherGroundsReasons implements CcdPageConfiguration {

    private final TextAreaValidationService textAreaValidationService;
    private static final ShowCondition.FieldRef INTRODUCTORY_OTHER_GROUNDS = ref(
        PCSCase::getIntroductoryDemotedOrOtherGroundsForPossession,
        IntroductoryDemotedOtherGroundsForPossession::getIntroductoryDemotedOrOtherGrounds
    );
    private static final ShowCondition ANTI_SOCIAL_SELECTED =
        INTRODUCTORY_OTHER_GROUNDS.contains(IntroductoryDemotedOrOtherGrounds.ANTI_SOCIAL);
    private static final ShowCondition BREACH_SELECTED =
        INTRODUCTORY_OTHER_GROUNDS.contains(IntroductoryDemotedOrOtherGrounds.BREACH_OF_THE_TENANCY);
    private static final ShowCondition ABSOLUTE_SELECTED =
        INTRODUCTORY_OTHER_GROUNDS.contains(IntroductoryDemotedOrOtherGrounds.ABSOLUTE_GROUNDS);
    private static final ShowCondition OTHER_SELECTED =
        INTRODUCTORY_OTHER_GROUNDS.contains(IntroductoryDemotedOrOtherGrounds.OTHER);
    private static final ShowCondition NO_GROUNDS_SELECTED = when(
        PCSCase::getIntroductoryDemotedOrOtherGroundsForPossession,
        IntroductoryDemotedOtherGroundsForPossession::getHasIntroductoryDemotedOtherGroundsForPossession
    ).is(VerticalYesNo.NO);

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("introductoryDemotedOtherGroundsReasons", this::midEvent)
            .pageLabel("Reasons for possession ")
            .showWhen(when(PCSCase::getShowIntroductoryDemotedOtherGroundReasonPage).is(YesOrNo.YES)
                .and(when(PCSCase::getTenancyLicenceDetails, TenancyLicenceDetails::getTypeOfTenancyLicence)
                    .isNot(TenancyLicenceType.ASSURED_TENANCY))
                .and(when(PCSCase::getTenancyLicenceDetails, TenancyLicenceDetails::getTypeOfTenancyLicence)
                    .isNot(TenancyLicenceType.SECURE_TENANCY))
                .and(when(PCSCase::getTenancyLicenceDetails, TenancyLicenceDetails::getTypeOfTenancyLicence)
                    .isNot(TenancyLicenceType.FLEXIBLE_TENANCY))
                .and(when(PCSCase::getLegislativeCountry).is(LegislativeCountry.ENGLAND)))
            .complex(PCSCase::getIntroductoryDemotedOtherGroundReason)
            .labelWhen("introductoryDemotedOtherGroundsReasons-antiSocial-label","""
                ---
                <h2 class="govuk-heading-l" tabindex="0">Antisocial behaviour</h2>
                <h3 class="govuk-heading-m" tabindex="0" >
                    Why are you making a claim for possession under this ground?
                </h3>
                """, ANTI_SOCIAL_SELECTED)
            .mandatoryWhen(IntroductoryDemotedOtherGroundReason::getAntiSocialBehaviourGround,
                ANTI_SOCIAL_SELECTED)

            .labelWhen("introductoryDemotedOtherGroundsReasons-breachOfTenancy-label","""
                ---
                <h2 class="govuk-heading-l" tabindex="0">Breach of the tenancy</h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, BREACH_SELECTED)
            .mandatoryWhen(IntroductoryDemotedOtherGroundReason::getBreachOfTheTenancyGround,
                BREACH_SELECTED)

            .labelWhen("introductoryDemotedOtherGroundsReasons-absoluteGrounds-label","""
                ---
                <h2 class="govuk-heading-l" tabindex="0">Absolute grounds</h2>
                <h3 class="govuk-heading-m" tabindex="0"> Why are you claiming possession?</h3>
                """, ABSOLUTE_SELECTED)
            .mandatoryWhen(IntroductoryDemotedOtherGroundReason::getAbsoluteGrounds,
                ABSOLUTE_SELECTED)

            .labelWhen("introductoryDemotedOtherGroundsReasons-otherGround-label","""
                ---
                <h2 class="govuk-heading-l" tabindex="0">Other grounds</h2>
                <h3 class="govuk-heading-m" tabindex="0"> Why are you claiming possession?</h3>
                """, OTHER_SELECTED)
            .mandatoryWhen(IntroductoryDemotedOtherGroundReason::getOtherGround,
                OTHER_SELECTED)
            .labelWhen("introductoryDemotedOtherGroundsReasons-noGrounds-label","""
                ---
                <h2 class="govuk-heading-l" tabindex="0">No grounds</h2>
                <h3 class="govuk-heading-m" tabindex="0"> Why are you claiming possession?</h3>
                """, NO_GROUNDS_SELECTED)
            .mandatoryWhen(IntroductoryDemotedOtherGroundReason::getNoGrounds,
                NO_GROUNDS_SELECTED)
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
