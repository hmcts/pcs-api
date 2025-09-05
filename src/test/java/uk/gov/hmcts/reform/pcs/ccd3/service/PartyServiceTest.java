package uk.gov.hmcts.reform.pcs.ccd3.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd3.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd3.entity.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd3.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd3.repository.PartyRepository;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartyServiceTest {

    @Mock
    private PartyRepository partyRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private PartyService partyService;

    @Test
    void shouldCreateAndLinkParty() {
        PcsCaseEntity caseEntity = new PcsCaseEntity();
        UUID userId = UUID.randomUUID();
        String forename = "Alice";
        String surname = "Smith";
        AddressUK contactAddress = mock(AddressUK.class);
        String contactEmail = "Alice.Smith@test.com";
        String contactPhone = "07478963256";
        Boolean active = true;

        AddressEntity addressEntity = mock(AddressEntity.class);
        when(modelMapper.map(contactAddress, AddressEntity.class)).thenReturn(addressEntity);

        PartyEntity party = partyService.createAndLinkParty(caseEntity, userId,
                                                            forename, surname,
                                                            contactEmail,
                                                            contactAddress,contactPhone,
                                                            active);

        assertThat(party.getIdamId()).isEqualTo(userId);
        assertThat(party.getForename()).isEqualTo(forename);
        assertThat(party.getSurname()).isEqualTo(surname);
        assertThat(party.getContactEmail()).isEqualTo(contactEmail);
        assertThat(party.getContactAddress()).isEqualTo(addressEntity);
        assertThat(party.getContactPhoneNumber()).isEqualTo(contactPhone);
        assertThat(party.getActive()).isEqualTo(active);
        assertThat(party.getPcsCase()).isSameAs(caseEntity);
        assertThat(caseEntity.getParties().size()).isEqualTo(1);
        assertThat(caseEntity.getParties().iterator().next()).isSameAs(party);
    }

    @Test
    void shouldSaveParty() {
        PartyEntity party = new PartyEntity();
        when(partyRepository.save(party)).thenReturn(party);

        PartyEntity result = partyService.saveParty(party);

        verify(partyRepository, times(1)).save(party);
        assertThat(result).isSameAs(party);
    }

}
