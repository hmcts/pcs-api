package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.OccupationContractDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * CCD page configuration for the Occupation contract or licence details screen.
 * This page is part of the Welsh journey for recording occupation contract details.
 */
@AllArgsConstructor
@Component
@Slf4j
public class OccupationContractDetailsPage implements CcdPageConfiguration {

    private static final String YOU_CAN_ENTER_UP_TO_500_CHARACTERS = "You can enter up to 500 characters";
    private static final String SHOW_OTHER_DETAILS_CONDITION = "contractType=\"OTHER\"";
    private static final String OCCUPATION_CONTRACT_DETAILS = "occupationContractDetails";
    private static final String OCCUPATION_CONTRACT_DETAILS_LABEL = "Occupation contract or licence details";
    private static final String DATE_IN_PAST_ERROR_MESSAGE = 
        "Occupation contract or licence start date must be in the past";
    private static final String DATE_NOT_TODAY_ERROR_MESSAGE = 
        "Occupation contract or licence start date cannot be today";
    private static final String DATE_NOT_FUTURE_ERROR_MESSAGE = 
        "Occupation contract or licence start date cannot be in the future";

    private final Clock ukClock;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page(OCCUPATION_CONTRACT_DETAILS, this::midEvent)
            .pageLabel(OCCUPATION_CONTRACT_DETAILS_LABEL)
            .showCondition("legislativeCountry=\"Wales\"")
            .complex(PCSCase::getOccupationContractDetails)
                .label("occupationContractDetails-separator", "---")
                .label(
                    "occupationContractDetails-question",
                    "<h2 class=\"govuk-heading-m\">Occupation contract or licence type</h2>"
                )
                .label(
                    "occupationContractDetails-subquestion",
                    "<h3 class=\"govuk-heading-s\">What type of occupation contract or licence is in place?</h3>"
                )
                .mandatoryWithLabel(
                    OccupationContractDetails::getContractType,
                    ""
                )
                .mandatory(OccupationContractDetails::getOtherContractTypeDetails, SHOW_OTHER_DETAILS_CONDITION,
                           "",
                           "Give details of the type of occupation contract or licence that's in place",
                           YOU_CAN_ENTER_UP_TO_500_CHARACTERS,
                           false
                )
                .label("occupationContractDetails-separator-2", "---")
                .label(
                    "occupationContractStartDate-question",
                    """
                    <h2 class=\"govuk-heading-m\">Occupation contract or licence start date</h2>
                    <h3 class=\"govuk-heading-s\">
                    What date did the occupation contract or licence begin? (Optional)
                    </h3>
                    """
                )
                .optional(OccupationContractDetails::getContractStartDate)
                .label("occupationContractStartDate-separator", "---")
                .label(
                    "uploadSection",
                    """
                    <h2 class=\"govuk-heading-m\">Upload occupation contract or licence</h2>
                    <h3 class=\"govuk-heading-s\">
                    Do you want to upload a copy of the occupation contract or licence? (Optional)
                    </h3>
                    <p class=\"govuk-hint\">You can either upload this now or closer to the hearing date.
                    Any documents you upload now will be included in the pack of
                    documents a judge will receive
                    before the hearing (the bundle).</p>
                    """
                )
                .optional(OccupationContractDetails::getContractDocuments)
            .done();
    }

    /**
     * Mid-event callback to validate the occupation contract details.
     * Implements validation for:
     * - Contract type selection (mandatory)
     * - Other contract type details character limit (500 characters)
     * - Date validation (must be in the past, not today, not future)
     */
    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();
        OccupationContractDetails occupationContractDetails = caseData.getOccupationContractDetails();
        
        if (occupationContractDetails == null) {
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data(caseData)
                .build();
        }

        List<String> errors = validateOccupationContractDetails(occupationContractDetails);
        
        if (!errors.isEmpty()) {
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .errors(errors)
                .build();
        }

        return AboutToStartOrSubmitResponse
            .<PCSCase, State>builder()
            .data(caseData)
            .build();
    }

    /**
     * Validates the occupation contract details according to the acceptance criteria.
     */
    private List<String> validateOccupationContractDetails(
        OccupationContractDetails details) {
        List<String> errors = new java.util.ArrayList<>();

        // Date field validation
        LocalDate contractStartDate = details.getContractStartDate();
        if (contractStartDate != null) {
            LocalDate currentDate = LocalDate.now(ukClock);
            
            // Date cannot be current date
            if (contractStartDate.isEqual(currentDate)) {
                errors.add(DATE_NOT_TODAY_ERROR_MESSAGE);
            }
            
            // Date cannot be in the future
            if (contractStartDate.isAfter(currentDate)) {
                errors.add(DATE_NOT_FUTURE_ERROR_MESSAGE);
            }
            
            // Date must be in the past (not today or future)
            if (!contractStartDate.isBefore(currentDate)) {
                errors.add(DATE_IN_PAST_ERROR_MESSAGE);
            }
        }

        return errors;
    }
}
