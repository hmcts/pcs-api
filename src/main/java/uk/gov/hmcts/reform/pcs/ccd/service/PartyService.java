package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyRepository;

import java.util.UUID;

@Service
@AllArgsConstructor
public class PartyService {

    private final PartyRepository partyRepository;
    private final ModelMapper modelMapper;

    public PartyEntity createAndLinkParty(PcsCaseEntity caseEntity,
                                          UUID userId, String forename,
                                          String surname,String contactEmail,
                                          AddressUK contactAddress,
                                          String contactPhoneNumber,
                                          Boolean active) {
        AddressUK applicantAddress = contactAddress;

        AddressEntity addressEntity = contactAddress != null
            ? modelMapper.map(applicantAddress, AddressEntity.class) : null;

        PartyEntity party = PartyEntity.builder()
            .idamId(userId)
            .forename(forename)
            .surname(surname)
            .active(active)
            .contactEmail(contactEmail)
            .contactAddress(addressEntity)
            .contactPhoneNumber(contactPhoneNumber)
            .pcsCase(caseEntity)
            .build();

        caseEntity.getParties().add(party);

        return party;
    }

    public PartyEntity saveParty(PartyEntity party) {
        return partyRepository.save(party);
    }

}
