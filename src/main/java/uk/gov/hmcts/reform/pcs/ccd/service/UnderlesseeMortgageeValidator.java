package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.UnderlesseeMortgagee;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@AllArgsConstructor
public class UnderlesseeMortgageeValidator {

    private final AddressValidator addressValidator;

    public List<String> validateFirstUnderlesseeOrMortgagee(UnderlesseeMortgagee defendant1Details,
                                                            boolean additionalDefendantsProvided) {
        String sectionHint = additionalDefendantsProvided ? "Underlessee or mortgagee 1" : ""; // might not be needed
        return validateUnderlesseeOrMortgageeAddress(defendant1Details, sectionHint);
    }

    public List<String> validateAdditionalUnderlesseeOrMortgagee(
        List<ListValue<UnderlesseeMortgagee>> additionalDefendants) {

        List<String> validationErrors = new ArrayList<>();

        for (int i = 0; i < additionalDefendants.size(); i++) {
            UnderlesseeMortgagee underlesseeMortgageeDetails = additionalDefendants.get(i).getValue();
            String sectionHint = "additional underlessee or mortgagee %d".formatted(i + 1);
            List<String> defendantValidationErrors = validateUnderlesseeOrMortgageeAddress(underlesseeMortgageeDetails,
                                                                                           sectionHint);

            validationErrors.addAll(defendantValidationErrors);
        }

        return validationErrors;
    }

    private List<String> validateUnderlesseeOrMortgageeAddress(UnderlesseeMortgagee underlesseeMortgagee,
                                                               String sectionHint) {
        if (underlesseeMortgagee.getUnderlesseeOrMortgageeAddressKnown() == VerticalYesNo.YES) {
            return addressValidator.validateAddressFields(underlesseeMortgagee.getUnderlesseeOrMortgageeAddress(),
                                                          sectionHint);
        }
        return Collections.emptyList();
    }

}
