package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.model.Defendant;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@AllArgsConstructor
public class DefendantService {

    private final ModelMapper modelMapper;

    public List<Defendant> buildDefendantsList(PCSCase pcsCase) {
        Objects.requireNonNull(pcsCase.getDefendant1(), "Defendant 1 must be provided");

        List<Defendant> defendants = new ArrayList<>();

        Defendant defendant1 = buildDefendant(pcsCase.getDefendant1());
        defendants.add(defendant1);
        defendant1.setAdditionalDefendantsAdded(pcsCase.getAddAnotherDefendant().toBoolean());

        if (pcsCase.getAddAnotherDefendant() == VerticalYesNo.YES) {
            List<Defendant> additionalDefendants = buildAdditionalDefendants(pcsCase.getAdditionalDefendants());
            defendants.addAll(additionalDefendants);
        }

        return defendants;
    }

    public List<DefendantDetails> mapToDefendantDetails(List<Defendant> defendantList) {
        if (defendantList == null) {
            return Collections.emptyList();
        }

        return defendantList.stream()
            .map(defendant -> modelMapper.map(defendant, DefendantDetails.class))
            .toList();
    }

    private Defendant buildDefendant(DefendantDetails defendantDetails) {
        Defendant defendant = new Defendant();

        defendant.setPartyId(UUID.randomUUID());
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

    /**
     * Builds a display name for a defendant from their details.
     * Handles cases where the name is not known.
     *
     * @param details Defendant details
     * @return Display name for the defendant
     */
    public String buildDefendantDisplayName(DefendantDetails details) {
        if (details == null) {
            return "Unknown";
        }
        if (details.getNameKnown() == VerticalYesNo.NO) {
            return "Name not known";
        }
        String firstName = details.getFirstName() != null ? details.getFirstName() : "";
        String lastName = details.getLastName() != null ? details.getLastName() : "";
        String fullName = (firstName + " " + lastName).trim();
        return fullName.isEmpty() ? "Unknown" : fullName;
    }

    /**
     * Builds a list of DynamicStringListElement from defendant details stored in the database.
     *
     * @param defendants List of defendants from the case data
     * @return List of DynamicStringListElement for the multi-select list
     */
    public List<DynamicStringListElement> buildDefendantListItems(
        List<ListValue<DefendantDetails>> defendants) {

        if (CollectionUtils.isEmpty(defendants)) {
            return new ArrayList<>();
        }

        List<DynamicStringListElement> listItems = new ArrayList<>();
        for (ListValue<DefendantDetails> listValue : defendants) {
            DefendantDetails defendantDetails = listValue.getValue();
            String defendantName = buildDefendantDisplayName(defendantDetails);

            listItems.add(DynamicStringListElement.builder()
                .code(UUID.randomUUID().toString())
                .label(defendantName)
                .build());
        }

        return listItems;
    }

}
