package uk.gov.hmcts.reform.pcs.ccd.page.caseworker.manageparty;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.AddPartyDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.PartyType;
import uk.gov.hmcts.reform.pcs.ccd.service.AddressValidator;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService.EXTRA_SHORT_TEXT_LIMIT;
import static uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService.FieldValidation;

@AllArgsConstructor
@Component
public class AddPartyDetailsPage implements CcdPageConfiguration {

    private final TextAreaValidationService textAreaValidationService;
    private final AddressValidator addressValidator;

    private static final String ORGANISATION_NAME_LABEL = "Organisation name";
    private static final String EMAIL_ADDRESS_LABEL = "Email address";
    private static final String PHONE_NUMBER_LABEL = "Phone number";

    private static final String CLAIMANT_SELECTED =
        ShowConditions.fieldEquals("addParty_AddPartyType", PartyType.CLAIMANT);
    private static final String DEFENDANT_SELECTED =
        ShowConditions.fieldEquals("addParty_AddPartyType", PartyType.DEFENDANT);
    private static final String LITIGATION_FRIEND_SELECTED =
        ShowConditions.fieldEquals("addParty_AddPartyType", PartyType.LITIGATION_FRIEND);

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("addClaimantOrDefendantDetails", this::midEvent)
            .showCondition(ShowConditions.or(CLAIMANT_SELECTED, DEFENDANT_SELECTED, LITIGATION_FRIEND_SELECTED))
            .pageLabel("Party details")
            .label("addClaimantOrDefendantDetails-separator", "---")
            .complex(PCSCase::getAddPartyDetails)
                // Claimant
                .optional(AddPartyDetails::getClaimantOrganisationName, CLAIMANT_SELECTED)
                .mandatory(AddPartyDetails::getClaimantFirstName, CLAIMANT_SELECTED)
                .mandatory(AddPartyDetails::getClaimantLastName, CLAIMANT_SELECTED)
                .complex(AddPartyDetails::getClaimantAddress, CLAIMANT_SELECTED)
                    .mandatory(AddressUK::getAddressLine1)
                    .optional(AddressUK::getAddressLine2)
                    .optional(AddressUK::getAddressLine3)
                    .mandatory(AddressUK::getPostTown)
                    .optional(AddressUK::getCounty)
                    .optional(AddressUK::getCountry)
                    .mandatoryWithLabel(AddressUK::getPostCode, "Postcode")
                .done()
                .optional(AddPartyDetails::getClaimantEmail, CLAIMANT_SELECTED)
                .optional(AddPartyDetails::getClaimantPhoneNumber, CLAIMANT_SELECTED)

                // Defendant
                .mandatory(AddPartyDetails::getFirstName, DEFENDANT_SELECTED)
                .mandatory(AddPartyDetails::getLastName, DEFENDANT_SELECTED)
                .optional(AddPartyDetails::getDefendantDateOfBirth, DEFENDANT_SELECTED)
                .complex(AddPartyDetails::getDefendantAddress, DEFENDANT_SELECTED)
                    .mandatory(AddressUK::getAddressLine1)
                    .optional(AddressUK::getAddressLine2)
                    .optional(AddressUK::getAddressLine3)
                    .mandatory(AddressUK::getPostTown)
                    .optional(AddressUK::getCounty)
                    .optional(AddressUK::getCountry)
                    .mandatoryWithLabel(AddressUK::getPostCode, "Postcode")
                .done()
                .optional(AddPartyDetails::getDefendantEmail, DEFENDANT_SELECTED)
                .optional(AddPartyDetails::getDefendantPhoneNumber, DEFENDANT_SELECTED)

                // Litigation friend
                .optional(AddPartyDetails::getLitigationFriendOrganisationName, LITIGATION_FRIEND_SELECTED)
                .mandatory(AddPartyDetails::getLitigationFriendFirstName, LITIGATION_FRIEND_SELECTED)
                .mandatory(AddPartyDetails::getLitigationFriendLastName, LITIGATION_FRIEND_SELECTED)
                .complex(AddPartyDetails::getLitigationFriendAddress, LITIGATION_FRIEND_SELECTED)
                    .mandatory(AddressUK::getAddressLine1)
                    .optional(AddressUK::getAddressLine2)
                    .optional(AddressUK::getAddressLine3)
                    .mandatory(AddressUK::getPostTown)
                    .optional(AddressUK::getCounty)
                    .optional(AddressUK::getCountry)
                    .mandatoryWithLabel(AddressUK::getPostCode, "Postcode")
                .done()
                .optional(AddPartyDetails::getLitigationFriendEmail, LITIGATION_FRIEND_SELECTED)
                .optional(AddPartyDetails::getLitigationFriendPhoneNumber, LITIGATION_FRIEND_SELECTED)
            .done();
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(
        CaseDetails<PCSCase, State> details, CaseDetails<PCSCase, State> detailsBefore) {

        PCSCase caseData = details.getData();
        AddPartyDetails partyDetails = caseData.getAddPartyDetails();

        List<String> validationErrors = textAreaValidationService.validateMultipleTextAreas(
            FieldValidation.of(
                partyDetails.getClaimantOrganisationName(), ORGANISATION_NAME_LABEL, EXTRA_SHORT_TEXT_LIMIT),
            FieldValidation.of(partyDetails.getClaimantEmail(), EMAIL_ADDRESS_LABEL, EXTRA_SHORT_TEXT_LIMIT),
            FieldValidation.of(partyDetails.getClaimantPhoneNumber(), PHONE_NUMBER_LABEL, EXTRA_SHORT_TEXT_LIMIT),
            FieldValidation.of(partyDetails.getDefendantEmail(), EMAIL_ADDRESS_LABEL, EXTRA_SHORT_TEXT_LIMIT),
            FieldValidation.of(partyDetails.getDefendantPhoneNumber(), PHONE_NUMBER_LABEL, EXTRA_SHORT_TEXT_LIMIT),
            FieldValidation.of(
                partyDetails.getLitigationFriendOrganisationName(), ORGANISATION_NAME_LABEL, EXTRA_SHORT_TEXT_LIMIT),
            FieldValidation.of(partyDetails.getLitigationFriendEmail(), EMAIL_ADDRESS_LABEL, EXTRA_SHORT_TEXT_LIMIT),
            FieldValidation.of(
                partyDetails.getLitigationFriendPhoneNumber(), PHONE_NUMBER_LABEL, EXTRA_SHORT_TEXT_LIMIT)
        );

        AddressUK address = switch (partyDetails.getAddPartyType()) {
            case CLAIMANT -> partyDetails.getClaimantAddress();
            case DEFENDANT -> partyDetails.getDefendantAddress();
            case LITIGATION_FRIEND -> partyDetails.getLitigationFriendAddress();
        };
        validationErrors.addAll(addressValidator.validateAddressFields(address));

        return textAreaValidationService.createValidationResponse(caseData, validationErrors);
    }
}
