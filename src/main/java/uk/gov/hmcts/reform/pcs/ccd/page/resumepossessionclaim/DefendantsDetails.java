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

import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.common.MultiPageLabel.SAVE_AND_RETURN_HTML;

@AllArgsConstructor
@Component
public class DefendantsDetails implements CcdPageConfiguration {

    private final AddressValidator addressValidator;

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
                .mandatory(DefendantDetails::getEmail)
            .done()
            .label("defendantsDetails-saveAndResume", SAVE_AND_RETURN_HTML);

    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {

        PCSCase caseData = details.getData();
        DefendantDetails defendantDetails = caseData.getDefendant1();

        if (defendantDetails.getAddressSameAsPossession() == VerticalYesNo.NO
            && defendantDetails.getAddressKnown() == VerticalYesNo.YES) {

            AddressUK correspondenceAddress = defendantDetails.getCorrespondenceAddress();
            List<String> validationErrors = addressValidator.validateAddressFields(correspondenceAddress);
            if (!validationErrors.isEmpty()) {
                return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                    .errors(validationErrors)
                    .build();
            }
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();

    }

}
