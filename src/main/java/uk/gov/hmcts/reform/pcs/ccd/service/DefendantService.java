package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantsDOBStringInterpolation;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantsDOBMultiLabel;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.model.Defendant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
@AllArgsConstructor
public class DefendantService {

    private ModelMapper modelMapper;

    public List<Defendant> buildDefendantsList(PCSCase pcsCase) {
        Objects.requireNonNull(pcsCase.getDefendant1(), "Defendant 1 must be provided");

        List<Defendant> defendants = new ArrayList<>();

        Defendant defendant1 = buildDefendant(pcsCase.getDefendant1());
        defendants.add(defendant1);

        if (pcsCase.getAddAnotherDefendant() == VerticalYesNo.YES) {
            List<Defendant> additionalDefendants = buildAdditionalDefendants(pcsCase.getAdditionalDefendants());
            defendants.addAll(additionalDefendants);
        }

        return defendants;
    }

    public DefendantsDOBMultiLabel mapToDefendantDOBConcept(List<Defendant> defendantList) {
        DefendantsDOBMultiLabel defendantsDOBMultiLabel = new DefendantsDOBMultiLabel();

        if (defendantList == null) {
            return defendantsDOBMultiLabel;
        }

        defendantsDOBMultiLabel.setFirstName1(safeFirstName(0, defendantList));
        defendantsDOBMultiLabel.setFirstName2(safeFirstName(1, defendantList));
        defendantsDOBMultiLabel.setFirstName3(safeFirstName(2, defendantList));
        defendantsDOBMultiLabel.setFirstName4(safeFirstName(3, defendantList));
        defendantsDOBMultiLabel.setFirstName5(safeFirstName(4, defendantList));

        return defendantsDOBMultiLabel;
    }

    private String safeFirstName(int i, List<Defendant> defendantList) {
        if (i > defendantList.size() - 1) {
            return "999";
        } else {
            return Objects.requireNonNullElse(defendantList.get(i).getFirstName(), "999");
        }
    }

    public List<DefendantDetails> mapToDefendantDetails(List<Defendant> defendantList) {
        if (defendantList == null) {
            return Collections.emptyList();
        }

        return defendantList.stream()
            .map(defendant -> modelMapper.map(defendant, DefendantDetails.class))
            .toList();
    }

    public List<DefendantsDOBStringInterpolation> mapToDefendantDOB(List<Defendant> defendantList) {
        if (defendantList == null) {
            return Collections.emptyList();
        }

        return defendantList.stream()
            .map(defendant -> new DefendantsDOBStringInterpolation(
                "Do you know " + defendant.getFirstName() + " "
                    + defendant.getLastName() + "'s date of birth?", null)
            )
            .toList();
    }

    private Defendant buildDefendant(DefendantDetails defendantDetails) {
        Defendant defendant = new Defendant();

        boolean nameKnown = defendantDetails.getNameKnown().toBoolean();
        defendant.setNameKnown(nameKnown);
        if (nameKnown) {
            defendant.setFirstName(defendantDetails.getFirstName());
            defendant.setLastName(defendantDetails.getLastName());
        }

        boolean addressKnown = defendantDetails.getAddressKnown().toBoolean();
        defendant.setAddressKnown(addressKnown);
        if (addressKnown) {
            boolean addressSameAsPossession = defendantDetails.getAddressSameAsPossession().toBoolean();
            defendant.setAddressSameAsPossession(addressSameAsPossession);
            if (!addressSameAsPossession) {
                defendant.setCorrespondenceAddress(defendantDetails.getCorrespondenceAddress());
            }
        }

        return defendant;
    }

    private List<Defendant> buildAdditionalDefendants(List<ListValue<DefendantDetails>> additionalDefendantsDetails) {
        if (additionalDefendantsDetails == null) {
            return Collections.emptyList();
        }

        return additionalDefendantsDetails.stream()
            .map(ListValue::getValue)
            .map(this::buildDefendant)
            .toList();

    }

}
