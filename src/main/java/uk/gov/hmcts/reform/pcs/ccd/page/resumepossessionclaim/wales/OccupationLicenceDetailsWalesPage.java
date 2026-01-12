package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceDetailsWales;
import uk.gov.hmcts.reform.pcs.ccd.service.TextValidationService;

/**
 * CCD page configuration for the Occupation contract or licence details screen.
 * This page is part of the Welsh journey for recording occupation contract details.
 */
@AllArgsConstructor
@Component
public class OccupationLicenceDetailsWalesPage implements CcdPageConfiguration {

    private static final String SHOW_OTHER_DETAILS_CONDITION = "occupationLicenceTypeWales=\"OTHER\"";
    private static final String OCCUPATION_CONTRACT_DETAILS = "OccupationLicenceDetailsWales";
    private static final String OCCUPATION_CONTRACT_DETAILS_LABEL = "Occupation contract or licence details";
    private static final String DATE_NOT_TODAY_ERROR_MESSAGE =
        "Occupation contract or licence start date cannot be today";
    private static final String DATE_NOT_FUTURE_ERROR_MESSAGE =
        "Occupation contract or licence start date cannot be in the future";

    private final Clock ukClock;
    private final TextValidationService textValidationService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page(OCCUPATION_CONTRACT_DETAILS, this::midEvent)
            .pageLabel(OCCUPATION_CONTRACT_DETAILS_LABEL)
            .showCondition("legislativeCountry=\"Wales\"")
            .complex(PCSCase::getOccupationLicenceDetailsWales)
            .label("OccupationLicenceDetailsWales-info", """
                ---
                <h2 class="govuk-heading-m">Occupation contract or licence type</h2>
                """)
            .mandatory(OccupationLicenceDetailsWales::getOccupationLicenceTypeWales)
            .mandatory(OccupationLicenceDetailsWales::getOtherLicenceTypeDetails, SHOW_OTHER_DETAILS_CONDITION)
            .label("OccupationLicenceDetailsWales-date-section", """
               ---
               <h2 class="govuk-heading-m">Occupation contract or licence start date</h2>
               """)
            .optional(OccupationLicenceDetailsWales::getLicenceStartDate)
            .label(
                "occupationLicenceDetailsWales-upload-section",
                """
                ---
                <h2 class=\"govuk-heading-m\">Upload occupation contract or licence</h2>
                <h3 class=\"govuk-heading-s\">
                Do you want to upload a copy of the occupation contract or licence? (Optional)
                </h3>
                <p class="govuk-hint govuk-!-font-size-16 govuk-!-margin-top-1">
                You can either upload this now or closer to the hearing date.
                Any documents you upload now will be included in the pack of
                documents a judge will receive before the hearing (the bundle)
                </p>
                """
            )
            .optional(OccupationLicenceDetailsWales::getLicenceDocuments)
            .done()
            .label("occupationLicenceDetailsWales-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);

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
        OccupationLicenceDetailsWales occupationLicenceDetailsWales = caseData.getOccupationLicenceDetailsWales();

        List<String> validationErrors = new ArrayList<>();

        if (occupationLicenceDetailsWales != null) {
            // Validate text area field
            validationErrors.addAll(textValidationService.validateSingleTextArea(
                occupationLicenceDetailsWales.getOtherLicenceTypeDetails(),
                "Give details about what type of occupation contract or licence is in place",
                TextValidationService.MEDIUM_TEXT_LIMIT
            ));

            // Validate date fields
            validationErrors.addAll(validateOccupationLicenceDetailsWales(occupationLicenceDetailsWales));
        }

        return textValidationService.createValidationResponse(caseData, validationErrors);
    }

    /**
     * Validates the occupation contract details according to the acceptance criteria.
     */
    private List<String> validateOccupationLicenceDetailsWales(
        OccupationLicenceDetailsWales details) {
        List<String> errors = new ArrayList<>();

        // Date field validation
        LocalDate contractStartDate = details.getLicenceStartDate();
        if (contractStartDate != null) {
            LocalDate currentDate = LocalDate.now(ukClock);

            // Date cannot be current date
            if (contractStartDate.isEqual(currentDate)) {
                errors.add(DATE_NOT_TODAY_ERROR_MESSAGE);
            } else if (contractStartDate.isAfter(currentDate)) {
                // Date cannot be in the future
                errors.add(DATE_NOT_FUTURE_ERROR_MESSAGE);
            }
        }

        return errors;
    }
}
