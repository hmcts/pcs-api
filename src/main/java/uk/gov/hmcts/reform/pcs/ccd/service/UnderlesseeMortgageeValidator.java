package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.UnderlesseeMortgageeDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class UnderlesseeMortgageeValidator {

    static final String EXUI_POFCC81_ERROR = """
        This page did not load correctly. Go back to the previous page and return to
        this page to enter the underlessee or mortgagee's correspondence address. The answers you’ve
        entered so far on this page will be kept
        """;

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
        if (underlesseeOrMortgagee.getAddressKnown() == VerticalYesNo.YES) {
            AddressUK correspondenceAddress = underlesseeOrMortgagee.getAddress();

            if (correspondenceAddress != null) {
                return addressValidator.validateAddressFields(correspondenceAddress, sectionHint);
            } else {
                // This is an ExUI bug and needs user action to reset it. See Jira POFCC-81
                return List.of(EXUI_POFCC81_ERROR);
            }
        }
        return new ArrayList<>();
    }

}
