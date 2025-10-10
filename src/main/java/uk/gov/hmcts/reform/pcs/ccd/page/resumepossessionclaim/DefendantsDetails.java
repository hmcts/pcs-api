package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDOBDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.service.AddressValidator;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
            .done()

            .readonly(PCSCase::getAdditionalDefendantsSectionLabel)
            .mandatory(PCSCase::getAddAdditionalDefendant)
                .mandatory(PCSCase::getDefendants, "addAdditionalDefendant=\"YES\"");
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

        // TODO: Update this once multiple defendant support is implemented.
        //  Set the text dynamically for one/multiple defendants.
        caseData.getDefendantCircumstances().setDefendantTermPossessive("defendants'");


        List<ListValue<DefendantDetails>> defendants = caseData.getDefendants();
        List<ListValue<DefendantDOBDetails>> defendantsDOB = new ArrayList<>();

        int index = 0;
        for (ListValue<DefendantDetails> defendant : defendants) {
            DefendantDOBDetails dobDetails = DefendantDOBDetails.builder()
                .name(defendant.getValue().getFirstName() + " " + defendant.getValue().getLastName())
                .dob(null)
                .build();

            ListValue<DefendantDOBDetails> lv = new ListValue<>(
                String.valueOf(index++),
                dobDetails
            );

            defendantsDOB.add(lv);
        }

        caseData.setDefendantsDOB(defendantsDOB);

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }
}
