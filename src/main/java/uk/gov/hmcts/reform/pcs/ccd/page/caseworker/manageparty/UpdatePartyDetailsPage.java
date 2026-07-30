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
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.ManagePartyOptions;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.PartyType;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.UpdatePartyDetails;
import uk.gov.hmcts.reform.pcs.ccd.service.AddressValidator;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.List;

@AllArgsConstructor
@Component
public class UpdatePartyDetailsPage implements CcdPageConfiguration {

    private final AddressValidator addressValidator;
    private final TextAreaValidationService textAreaValidationService;

    private static final String DEFENDANT_TYPE =
        ShowConditions.fieldEquals("updateParty_UpdatePartyType", PartyType.DEFENDANT);

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("updatePartyDetails", this::midEvent)
            .showCondition(ShowConditions.fieldEquals("addParty_ManagePartyOptions", ManagePartyOptions.UPDATE))
            .pageLabel("Update party's details")
            .label("updatePartyDetails-separator", "---")
            .complex(PCSCase::getUpdatePartyDetails)
                .optional(UpdatePartyDetails::getDateOfBirth, DEFENDANT_TYPE)
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

        List<String> validationErrors = addressValidator.validateAddressFields(updatePartyDetails.getAddress());

        return textAreaValidationService.createValidationResponse(caseData, validationErrors);
    }
}