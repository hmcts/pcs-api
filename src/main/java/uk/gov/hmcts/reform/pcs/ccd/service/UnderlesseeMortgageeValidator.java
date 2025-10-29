package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.UnderlesseeMortgageeDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@AllArgsConstructor
public class UnderlesseeMortgageeValidator {

    private final AddressValidator addressValidator;

    public List<String> validateUnderlesseeOrMortgagee1(UnderlesseeMortgageeDetails underlesseeMortgageeDetails,
                                                        boolean additionalUnderlesseeOrMortgageeProvided) {
        String sectionHint = additionalUnderlesseeOrMortgageeProvided ? "Underlessee or mortgagee 1" : "";
        return validateUnderlesseeOrMortgageeAddress(underlesseeMortgageeDetails, sectionHint);
    }

    public List<String> validateAdditionalUnderlesseeOrMortgagee(
        List<ListValue<UnderlesseeMortgageeDetails>> additionalUnderlesseeOrMortgagee) {

        List<String> validationErrors = new ArrayList<>();

        for (int i = 0; i < additionalUnderlesseeOrMortgagee.size(); i++) {
            UnderlesseeMortgageeDetails underlesseeOrMortgagee = additionalUnderlesseeOrMortgagee.get(i).getValue();
            String sectionHint = "additional underlessee or mortgagee %d".formatted(i + 1);
            List<String> underlesseeMortgageeValidationErrors = validateUnderlesseeOrMortgageeAddress(
                underlesseeOrMortgagee, sectionHint);

            validationErrors.addAll(underlesseeMortgageeValidationErrors);
        }

        return validationErrors;
    }

    private List<String> validateUnderlesseeOrMortgageeAddress(UnderlesseeMortgageeDetails underlesseeOrMortgagee,
                                                               String sectionHint) {
        if (underlesseeOrMortgagee.getUnderlesseeOrMortgageeAddressKnown() == VerticalYesNo.YES) {
            return addressValidator.validateAddressFields(
                underlesseeOrMortgagee.getUnderlesseeOrMortgageeAddress(),
                sectionHint);
        }
        return Collections.emptyList();
    }

}
