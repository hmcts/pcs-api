package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@AllArgsConstructor
public class DefendantValidator {

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

            return addressValidator.validateAddressFields(defendantDetails.getCorrespondenceAddress(), sectionHint);
        } else {
            return Collections.emptyList();
        }
    }


}
