package uk.gov.hmcts.reform.pcs.ccd.service.caseworker.manageparty;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.PartyType;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.UpdatePartyDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;

import java.util.UUID;

@Service
@AllArgsConstructor
public class UpdatePartyService {

    private final PartyService partyService;
    private final PartyRepository partyRepository;
    private final AddressMapper addressMapper;

    @Transactional
    public void updateParty(UpdatePartyDetails updatePartyDetails, long caseReference) {
        UUID partyId = updatePartyDetails.getPartyToUpdate().getValueCode();
        PartyEntity partyEntity = partyService.getPartyEntityById(partyId, caseReference);

        AddressUK address = updatePartyDetails.getAddress();
        partyEntity.setAddressKnown(VerticalYesNo.from(address != null));
        partyEntity.setAddress(address != null ? addressMapper.toAddressEntityAndNormalise(address) : null);

        String phoneNumber = nullIfBlank(updatePartyDetails.getPhoneNumber());
        partyEntity.setPhoneNumberProvided(VerticalYesNo.from(phoneNumber != null));
        partyEntity.setPhoneNumber(phoneNumber);
        partyEntity.setEmailAddress(nullIfBlank(updatePartyDetails.getEmail()));

        if (updatePartyDetails.getPartyType() == PartyType.DEFENDANT) {
            partyEntity.setDateOfBirth(updatePartyDetails.getDateOfBirth());
        }

        partyRepository.save(partyEntity);
    }

    private String nullIfBlank(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
