package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.service.AddressValidator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@AllArgsConstructor
@Component
public class DefendantsDetails implements CcdPageConfiguration {

    private final AddressValidator addressValidator;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("defendantsDetails", this::midEvent)
            .pageLabel("Defendant details")
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
            .done()
            .label("defendantsDetails-additionalDefendants", """
                ---
                <h2>Additional defendants</h2>""")
            .mandatory(PCSCase::getAddAnotherDefendant)
            .mandatory(PCSCase::getAdditionalDefendants, "addAnotherDefendant=\"YES\"");

    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {

        PCSCase caseData = details.getData();

        boolean additionalDefendantsProvided = caseData.getAddAnotherDefendant() == VerticalYesNo.YES;

        DefendantDetails defendantDetails = caseData.getDefendant1();
        List<String> validationErrors
            = new ArrayList<>(validateDefendant1(defendantDetails, additionalDefendantsProvided));

        if (additionalDefendantsProvided) {
            List<String> additionalValidationErrors = validateAdditionalDefendants(caseData.getAdditionalDefendants());
            validationErrors.addAll(additionalValidationErrors);
        }

        if (!validationErrors.isEmpty()) {
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .errors(validationErrors)
                .build();
        }

        caseData.getDefendantCircumstances()
            .setDefendantTermPossessive(additionalDefendantsProvided ? "defendants'" : "defendant's");

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();

    }

    private List<String> validateDefendant1(DefendantDetails defendantDetails, boolean additionalDefendantsProvided) {
        String sectionHint = additionalDefendantsProvided ? "defendant 1" : "";
        return validateDefendant(defendantDetails, sectionHint);
    }

    private List<String> validateAdditionalDefendants(List<ListValue<DefendantDetails>> additionalDefendants) {
        List<String> validationErrors = new ArrayList<>();

        for (int i = 0; i < additionalDefendants.size(); i++) {
            DefendantDetails defendantDetails = additionalDefendants.get(i).getValue();
            String sectionHint = "additional defendant %d".formatted(i + 1);
            List<String> defendantValidationErrors = validateDefendant(defendantDetails, sectionHint);

            validationErrors.addAll(defendantValidationErrors);
        }

        return validationErrors;
    }

    private List<String> validateDefendant(DefendantDetails defendantDetails, String sectionHint) {
        if (defendantDetails.getAddressKnown() == VerticalYesNo.YES
            && defendantDetails.getAddressSameAsPossession() == VerticalYesNo.NO) {

            return addressValidator.validateAddressFields(defendantDetails.getCorrespondenceAddress(), sectionHint);
        } else {
            return Collections.emptyList();
        }
    }

}
