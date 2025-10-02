package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyRepository;

import java.util.UUID;

@Service
@AllArgsConstructor
public class PartyService {

    private final PartyRepository partyRepository;
    private final ModelMapper modelMapper;

    public PartyEntity createPartyEntity(UUID userId,
                                         String forename,
                                         String surname,
                                         String contactEmail,
                                         AddressUK contactAddress,
                                         String contactPhoneNumber) {

        AddressEntity addressEntity = contactAddress != null
            ? modelMapper.map(contactAddress, AddressEntity.class) : null;

        PartyEntity partyEntity = PartyEntity.builder()
            .idamId(userId)
            .forename(forename)
            .surname(surname)
            .active(true)
            .contactEmail(contactEmail)
            .contactAddress(addressEntity)
            .contactPhoneNumber(contactPhoneNumber)
            .build();

        partyRepository.save(partyEntity);

        return partyEntity;
    }

}
