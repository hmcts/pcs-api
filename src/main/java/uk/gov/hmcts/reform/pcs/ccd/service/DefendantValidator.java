package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@AllArgsConstructor
public class DefendantValidator {

    static final String EXUI_POFCC81_ERROR = """
        This page did not load correctly. Go back to the previous page and return to
        this page to enter the defendant's correspondence address. The answers you’ve
        entered so far on this page will be kept
        """;

    private final AddressValidator addressValidator;

    public List<String> validateDefendant1(DefendantDetails defendant1Details, boolean additionalDefendantsProvided) {
        String sectionHint = additionalDefendantsProvided ? "defendant 1" : "";
        return validateDefendant(defendant1Details, sectionHint);
    }

    public List<String> validateAdditionalDefendants(List<ListValue<DefendantDetails>> additionalDefendants) {
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

            AddressUK correspondenceAddress = defendantDetails.getCorrespondenceAddress();
            if (correspondenceAddress != null) {
                return addressValidator.validateAddressFields(correspondenceAddress, sectionHint);
            } else {
                // This is an ExUI bug and needs user action to reset it. See Jira POFCC-81
                return List.of(EXUI_POFCC81_ERROR);
            }
        } else {
            return Collections.emptyList();
        }
    }


}
