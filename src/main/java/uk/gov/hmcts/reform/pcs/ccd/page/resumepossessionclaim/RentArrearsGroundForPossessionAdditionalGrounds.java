package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredAdditionalDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredAdditionalMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredAdditionalOtherGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredRentArrearsPossessionGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Page for selecting additional grounds for possession on an assured tenancy.
 */
@Slf4j
@AllArgsConstructor
@Component
public class RentArrearsGroundForPossessionAdditionalGrounds implements CcdPageConfiguration {

    private final TextAreaValidationService textAreaValidationService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("groundForPossessionAdditionalGrounds", this::midEvent)
            .pageLabel("What are your additional grounds for possession?")
            .showCondition("hasOtherAdditionalGrounds=\"YES\""
                           + " AND tenancy_TypeOfTenancyLicence=\"ASSURED_TENANCY\""
                           + " AND claimDueToRentArrears=\"YES\""
                           + " AND legislativeCountry=\"England\"")
            .label("groundForPossessionAdditionalGrounds-info", """
            ---
            <p class="govuk-body">You may have already given the defendants notice of your intention to begin
                possession proceedings. If you have, you should have written the grounds you’re making your
                claim under. You should select these grounds here and any extra grounds you’d like to add to
                your claim, if you need to.</p>
            <p class="govuk-body">
              <a href="https://england.shelter.org.uk/professional_resources/legal/possession_and_eviction/grounds_for_possession" class="govuk-link" rel="noreferrer noopener" target="_blank">More information about possession grounds (opens in new tab)</a>.
            </p>
            """)
            .complex(PCSCase::getAssuredRentArrearsPossessionGrounds)
            .optional(AssuredRentArrearsPossessionGrounds::getAdditionalMandatoryGrounds)
            .optional(AssuredRentArrearsPossessionGrounds::getAdditionalDiscretionaryGrounds)
            .optional(AssuredRentArrearsPossessionGrounds::getAdditionalOtherGround)
            .mandatory(AssuredRentArrearsPossessionGrounds::getAdditionalOtherGroundDescription,
                    "rentArrears_"
                            + "AdditionalOtherGroundCONTAINS\"OTHER\"")
            .done()
            .label("groundForPossessionAdditionalGrounds-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    public AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                 CaseDetails<PCSCase, State> detailsBefore) {

        PCSCase caseData = details.getData();
        AssuredRentArrearsPossessionGrounds groundsForPossession = caseData.getAssuredRentArrearsPossessionGrounds();

        Set<AssuredAdditionalMandatoryGrounds> mandatoryGrounds
            = groundsForPossession.getAdditionalMandatoryGrounds();
        Set<AssuredAdditionalDiscretionaryGrounds> discretionaryGrounds
            = groundsForPossession.getAdditionalDiscretionaryGrounds();
        Set<AssuredAdditionalOtherGround> additionalOtherGrounds
            = groundsForPossession.getAdditionalOtherGround();

        if (CollectionUtils.isEmpty(mandatoryGrounds) && CollectionUtils.isEmpty(discretionaryGrounds)
                && CollectionUtils.isEmpty(additionalOtherGrounds)) {
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .errorMessageOverride("Please select at least one ground")
                .build();
        }

        if (!CollectionUtils.isEmpty(additionalOtherGrounds)
            && groundsForPossession.getAdditionalOtherGroundDescription() != null) {
            List<String> validationErrors = new ArrayList<>();

            validationErrors.addAll(textAreaValidationService.validateSingleTextArea(
                    caseData.getAssuredRentArrearsPossessionGrounds().getAdditionalOtherGroundDescription(),
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
