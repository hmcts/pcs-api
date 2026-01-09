package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.UnderlesseeMortgageeDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.model.UnderlesseeMortgagee;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@AllArgsConstructor
public class UnderlesseeMortgageeService {

    public List<UnderlesseeMortgagee> buildUnderlesseeMortgageeList(PCSCase pcsCase) {
        Objects.requireNonNull(pcsCase.getUnderlesseeOrMortgagee1(),
                               "First underlessee or mortgagee must be provided");

        List<UnderlesseeMortgagee> underlesseesOrMortgagees = new ArrayList<>();

        UnderlesseeMortgagee underlesseeOrMortgagee1 = buildUnderlesseeMortgagee(pcsCase.getUnderlesseeOrMortgagee1());
        underlesseesOrMortgagees.add(underlesseeOrMortgagee1);

        if (pcsCase.getAddAdditionalUnderlesseeOrMortgagee() != null) {
            underlesseeOrMortgagee1.setAddAdditionalUnderlesseeOrMortgagee(
                pcsCase.getAddAdditionalUnderlesseeOrMortgagee().toBoolean()
            );
        }

        if (pcsCase.getAddAdditionalUnderlesseeOrMortgagee() == VerticalYesNo.YES) {
            List<UnderlesseeMortgagee> additionalUnderlesseeMortgagee
                = buildAdditionalUnderlesseeMortgagee(pcsCase.getAdditionalUnderlesseeOrMortgagee());
            underlesseesOrMortgagees.addAll(additionalUnderlesseeMortgagee);
        }

        return underlesseesOrMortgagees;
    }

    private UnderlesseeMortgagee buildUnderlesseeMortgagee(UnderlesseeMortgageeDetails underlesseeOrMortgagee1) {
        UnderlesseeMortgagee underlesseeOrMortgagee = new UnderlesseeMortgagee();

        boolean nameKnown = underlesseeOrMortgagee1.getNameKnown() != null
            && underlesseeOrMortgagee1.getNameKnown().toBoolean();

        underlesseeOrMortgagee.setUnderlesseeOrMortgageeNameKnown(nameKnown);
        if (nameKnown) {
            underlesseeOrMortgagee.setUnderlesseeOrMortgageeName(underlesseeOrMortgagee1
                                                                   .getName());
        }

        boolean addressKnown = underlesseeOrMortgagee1.getAddressKnown() != null
            && underlesseeOrMortgagee1.getAddressKnown().toBoolean();

        underlesseeOrMortgagee.setUnderlesseeOrMortgageeAddressKnown(addressKnown);
        if (addressKnown) {
            underlesseeOrMortgagee.setUnderlesseeOrMortgageeAddress(underlesseeOrMortgagee1
                                                                      .getAddress());
        }

        return underlesseeOrMortgagee;
    }

    private List<UnderlesseeMortgagee> buildAdditionalUnderlesseeMortgagee(
        List<ListValue<UnderlesseeMortgageeDetails>> additionalUnderlesseeOrMortgageeDetails) {
        if (additionalUnderlesseeOrMortgageeDetails == null) {
            return new ArrayList<>();
        }

        return additionalUnderlesseeOrMortgageeDetails.stream()
            .map(ListValue::getValue)
            .map(this::buildUnderlesseeMortgagee)
            .toList();

    }
}
