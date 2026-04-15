package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;

import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class DefendantService {

    /**
     * Builds a display name for a defendant from their details.
     * Handles cases where the name is not known.
     *
     * @param details Defendant details
     * @return Display name for the defendant
     */
    String buildDefendantDisplayName(Party details) {
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
    public List<DynamicStringListElement> buildDefendantListItems(List<ListValue<Party>> defendants) {

        if (CollectionUtils.isEmpty(defendants)) {
            return new ArrayList<>();
        }

        List<DynamicStringListElement> listItems = new ArrayList<>();
        for (ListValue<Party> listValue : defendants) {
            String partyId = listValue.getId();
            Party defendantDetails = listValue.getValue();
            String defendantName = buildDefendantDisplayName(defendantDetails);

            listItems.add(DynamicStringListElement.builder()
                .code(partyId)
                .label(defendantName)
                .build());
        }

        return listItems;
    }

}
