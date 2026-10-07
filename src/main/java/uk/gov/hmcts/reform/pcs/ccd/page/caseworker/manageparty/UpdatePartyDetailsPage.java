package uk.gov.hmcts.reform.pcs.ccd.page.caseworker.manageparty;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.ManagePartyOptions;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.PartyType;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.UpdatePartyDetails;
import uk.gov.hmcts.reform.pcs.ccd.service.AddressValidator;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService.EXTRA_SHORT_TEXT_LIMIT;
import static uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService.FieldValidation;

@Component
public class UpdatePartyDetailsPage implements CcdPageConfiguration {

    private final AddressValidator addressValidator;
    private final TextAreaValidationService textAreaValidationService;
    private final Clock ukClock;

    private static final String EMAIL_ADDRESS_LABEL = "Email address";
    private static final String DATE_OF_BIRTH_PAST_ERROR_MESSAGE = "Date of birth must be in the past";

    private static final String DEFENDANT_TYPE =
        ShowConditions.fieldEquals("updateParty_UpdatePartyType", PartyType.DEFENDANT);

    public UpdatePartyDetailsPage(AddressValidator addressValidator,
                                  TextAreaValidationService textAreaValidationService,
                                  @Qualifier("ukClock") Clock ukClock) {
        this.addressValidator = addressValidator;
        this.textAreaValidationService = textAreaValidationService;
        this.ukClock = ukClock;
    }

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("updatePartyDetails", this::midEvent)
            .showCondition(ShowConditions.fieldEquals("addParty_ManagePartyOptions", ManagePartyOptions.UPDATE))
            .pageLabel("Update party's details")
            .label("updatePartyDetails-separator", "---")
            .complex(PCSCase::getUpdatePartyDetails)
                .mandatory(UpdatePartyDetails::getDateOfBirth, DEFENDANT_TYPE)
                .complex(UpdatePartyDetails::getAddress)
                    .mandatory(AddressUK::getAddressLine1)
                    .optional(AddressUK::getAddressLine2)
                    .optional(AddressUK::getAddressLine3)
                    .mandatory(AddressUK::getPostTown)
                    .optional(AddressUK::getCounty)
                    .optional(AddressUK::getCountry)
                    .mandatoryWithLabel(AddressUK::getPostCode, "Postcode")
                .done()
                .optional(UpdatePartyDetails::getEmail)
                .optional(UpdatePartyDetails::getPhoneNumber)
            .done();
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(
        CaseDetails<PCSCase, State> details, CaseDetails<PCSCase, State> detailsBefore) {

        PCSCase caseData = details.getData();
        UpdatePartyDetails updatePartyDetails = caseData.getUpdatePartyDetails();

        List<String> validationErrors = textAreaValidationService.validateMultipleTextAreas(
            FieldValidation.of(updatePartyDetails.getEmail(), EMAIL_ADDRESS_LABEL, EXTRA_SHORT_TEXT_LIMIT)
        );

        if (updatePartyDetails.getPartyType() == PartyType.DEFENDANT) {
            LocalDate dob =  updatePartyDetails.getDateOfBirth().orElse(null);
            if (dob != null && !dob.isBefore(LocalDate.now(ukClock))) {
                validationErrors.add(DATE_OF_BIRTH_PAST_ERROR_MESSAGE);
            }
        }
        validationErrors.addAll(addressValidator.validateAddressFields(updatePartyDetails.getAddress()));

        return textAreaValidationService.createValidationResponse(caseData, validationErrors);
    }
}
