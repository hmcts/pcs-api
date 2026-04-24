package uk.gov.hmcts.reform.pcs.ccd.view;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.CasePartiesTab;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.ClaimantTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.DefendantTabDetails;

import java.util.ArrayList;
import java.util.List;

@Component
public class CaseTabView {

    private static final String NAME_UNKNOWN = "Person unknown";

    public void setCaseTabFields(PCSCase pcsCase) {
        setCasePartiesTabDetails(pcsCase);
    }

    private void setCasePartiesTabDetails(PCSCase pcsCase) {
        CasePartiesTab tab = CasePartiesTab.builder().build();

        if (pcsCase.getAllClaimants() != null && !pcsCase.getAllClaimants().isEmpty()) {
            Party claimant = pcsCase.getAllClaimants().getFirst().getValue();
            ClaimantTabDetails claimantTabDetails = createClaimantTabDetails(claimant, pcsCase);
            tab.setClaimantDetails(claimantTabDetails);
        }

        if (pcsCase.getAllDefendants() != null && !pcsCase.getAllDefendants().isEmpty()) {
            List<ListValue<Party>> allDefendants = new ArrayList<>(pcsCase.getAllDefendants());
            Party defendant1 = allDefendants.removeFirst().getValue();
            DefendantTabDetails defendant1TabDetails = createDefendantTabDetails(defendant1, pcsCase);
            tab.setDefendantOneDetails(defendant1TabDetails);

            if (!allDefendants.isEmpty()) {
                List<ListValue<DefendantTabDetails>> additionalDefendants = allDefendants
                    .stream().map(partyListValue -> {
                        Party defendant = partyListValue.getValue();
                        DefendantTabDetails defendantTabDetails = createDefendantTabDetails(defendant, pcsCase);
                        return ListValue.<DefendantTabDetails>builder().value(defendantTabDetails).build();
                    }).toList();

                tab.setDefendantsDetails(additionalDefendants);
            }
        }

        pcsCase.setCasePartiesTab(tab);
    }

    private ClaimantTabDetails createClaimantTabDetails(Party claimant, PCSCase pcsCase) {
        return ClaimantTabDetails.builder()
            .name(claimant.getOrgName())
            .emailAddress(claimant.getEmailAddress())
            .serviceAddress(claimant.getAddress())
            .telephoneNumber(claimant.getPhoneNumber())
            .build();
    }

    private DefendantTabDetails createDefendantTabDetails(Party defendant, PCSCase pcsCase) {
        AddressUK defendantAddress = defendant.getAddress() != null
            ? defendant.getAddress() : pcsCase.getPropertyAddress();
        String defendantFirstName = NAME_UNKNOWN;
        String defendantLastName = NAME_UNKNOWN;

        if (defendant.getNameKnown() == VerticalYesNo.YES) {
            defendantFirstName = defendant.getFirstName();
            defendantLastName = defendant.getLastName();
        }

        return DefendantTabDetails.builder()
            .serviceAddress(defendantAddress)
            .firstName(defendantFirstName)
            .lastName(defendantLastName)
            .build();
    }
}
