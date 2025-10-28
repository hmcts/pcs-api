package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.UnderlesseeMortgagee;
import uk.gov.hmcts.reform.pcs.ccd.domain.UnderlesseeMortgageeDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
@AllArgsConstructor
public class UnderlesseeMortgageeService {

    public List<UnderlesseeMortgagee> buildUnderlesseeMortgageeList(PCSCase pcsCase) {
        Objects.requireNonNull(pcsCase.getUnderlesseeMortgageeDetails(),
                               "Underlessee or mortgagee must be provided");

        List<UnderlesseeMortgagee> underlesseesMortgagees = new ArrayList<>();

        UnderlesseeMortgagee underlesseeMortgagee1 = buildUnderlesseeMortgagee(pcsCase.getUnderlesseeMortgageeDetails());
        underlesseesMortgagees.add(underlesseeMortgagee1);

        if (pcsCase.getAddAdditionalUnderlesseeOrMortgagee() == VerticalYesNo.YES) {
            List<UnderlesseeMortgagee> additionalUnderlesseeMortgagee
                = buildAdditionalUnderlesseeMortgagee(pcsCase.getAdditionalUnderlesseeMortgagee());
            underlesseesMortgagees.addAll(additionalUnderlesseeMortgagee);
        }

        return underlesseesMortgagees;
    }

    private UnderlesseeMortgagee buildUnderlesseeMortgagee(UnderlesseeMortgageeDetails underlesseeMortgageeDetails) {
        UnderlesseeMortgagee underlesseeMortgagee = new UnderlesseeMortgagee();

        boolean nameKnown = underlesseeMortgageeDetails.getUnderlesseeOrMortgageeNameKnown().toBoolean();
        underlesseeMortgagee.setUnderlesseeOrMortgageeNameKnown(nameKnown);
        if (nameKnown) {
            underlesseeMortgagee.setUnderlesseeOrMortgageeName(underlesseeMortgageeDetails
                                                                   .getUnderlesseeOrMortgageeName());
        }

        boolean addressKnown = underlesseeMortgageeDetails.getUnderlesseeOrMortgageeAddressKnown().toBoolean();
        underlesseeMortgagee.setUnderlesseeOrMortgageeAddressKnown(addressKnown);
        if (addressKnown) {
            underlesseeMortgagee.setUnderlesseeOrMortgageeAddress(underlesseeMortgageeDetails
                                                                      .getUnderlesseeOrMortgageeAddress());
        }

        return underlesseeMortgagee;
    }

    private List<UnderlesseeMortgagee> buildAdditionalUnderlesseeMortgagee(
        List<ListValue<UnderlesseeMortgageeDetails>> additionalUnderlesseeMortgageeDetails) {
        if (additionalUnderlesseeMortgageeDetails == null) {
            return Collections.emptyList();
        }

        return additionalUnderlesseeMortgageeDetails.stream()
            .map(ListValue::getValue)
            .map(this::buildUnderlesseeMortgagee)
            .toList();

    }
}
