package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.service.AddressValidator;
import uk.gov.hmcts.reform.pcs.ccd.util.PostcodeValidator;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Component
public class DefendantsDetails implements CcdPageConfiguration {

    private final AddressValidator addressValidator;
    private final PostcodeValidator postcodeValidator;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("defendantsDetails", this::midEvent)
            .pageLabel("Defendant 1 details")
            .complex(PCSCase::getDefendant1)
                .readonly(DefendantDetails::getNameSectionLabel)
                .mandatory(DefendantDetails::getNameKnown)
                .mandatory(DefendantDetails::getFirstName)
                .mandatory(DefendantDetails::getLastName)

                .readonly(DefendantDetails::getAddressSectionLabel)
                .mandatory(DefendantDetails::getAddressKnown)
                .mandatory(DefendantDetails::getAddressSameAsPossession)
                .complex(DefendantDetails::getCorrespondenceAddress)
                    .mandatory(AddressUK::getAddressLine1)
                    .optional(AddressUK::getAddressLine2)
                    .optional(AddressUK::getAddressLine3)
                    .mandatory(AddressUK::getPostTown)
                    .optional(AddressUK::getCounty)
                    .optional(AddressUK::getCountry)
                    .mandatoryWithLabel(AddressUK::getPostCode, "Postcode")
                .done()
                .mandatory(DefendantDetails::getCorrespondenceAddress)

                .readonly(DefendantDetails::getEmailSectionLabel)
                .mandatory(DefendantDetails::getEmailKnown)
                .mandatory(DefendantDetails::getEmail);

    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {

        PCSCase caseData = details.getData();
        List<String> errors = new ArrayList<>();
        
        DefendantDetails defendantDetails = caseData.getDefendant1();
        
        if (defendantDetails != null 
            && defendantDetails.getAddressSameAsPossession() == VerticalYesNo.NO
            && defendantDetails.getAddressKnown() == VerticalYesNo.YES) {

            AddressUK correspondenceAddress = defendantDetails.getCorrespondenceAddress();
            
            // Validate address fields
            List<String> addressErrors = addressValidator.validateAddressFields(correspondenceAddress);
            errors.addAll(addressErrors);
            
            // Validate postcode
            List<String> postcodeErrors = postcodeValidator.getValidationErrors(
                correspondenceAddress,
                "defendant1.correspondenceAddress"
            );
            errors.addAll(postcodeErrors);
        }

        if (!errors.isEmpty()) {
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data(caseData)
                .errors(errors)
                .build();
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();

    }

}
