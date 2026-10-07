package uk.gov.hmcts.reform.pcs.ccd.service.form;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;

import static uk.gov.hmcts.reform.pcs.ccd.service.form.FormFieldFormatter.isPopulated;

/**
 * Resolves the postal address and display name a party should be written to, shared by the
 * access-code letter and the bulk-print coversheet so the envelope always matches the enclosed form.
 */
@Service
public class RecipientAddressResolver {

    private static final String PERSONS_UNKNOWN = "Persons unknown";

    public AddressEntity resolvePostalAddress(PartyEntity party, PartyRole role, AddressEntity propertyAddress) {
        if (role == PartyRole.CLAIMANT) {
            return party.getAddress();
        }
        return confirmedOwnAddress(party) ? party.getAddress() : propertyAddress;
    }

    public String resolveDisplayName(PartyEntity party) {
        if (isPopulated(party.getOrgName())) {
            return party.getOrgName();
        }
        if (party.getNameKnown() == VerticalYesNo.NO) {
            return PERSONS_UNKNOWN;
        }
        String name = PartyDisplayMapper.joinName(party.getFirstName(), party.getLastName());
        return name.isBlank() ? PERSONS_UNKNOWN : name;
    }

    private boolean confirmedOwnAddress(PartyEntity party) {
        return party.getAddressKnown() == VerticalYesNo.YES
            && party.getAddressSameAsProperty() != VerticalYesNo.YES
            && party.getAddress() != null;
    }
}
