package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyRepository;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartyServiceTest {

    @Mock
    private PartyRepository partyRepository;
    @Mock
    private ModelMapper modelMapper;

    private PartyService underTest;

    @BeforeEach
    void setUp() {
        underTest = new PartyService(partyRepository, modelMapper);
    }

    @Test
    void shouldCreatePartyEntity() {
        // Given
        UUID userId = UUID.randomUUID();
        String forename = "Alice";
        String surname = "Smith";
        AddressUK contactAddress = mock(AddressUK.class);
        String contactEmail = "Alice.Smith@test.com";
        String contactPhone = "07478963256";

        AddressEntity addressEntity = mock(AddressEntity.class);
        when(modelMapper.map(contactAddress, AddressEntity.class)).thenReturn(addressEntity);

        // When
        PartyEntity createdPartyEntity = underTest.createPartyEntity(userId, forename, surname, contactEmail,
                                                        contactAddress, contactPhone);

        // Then
        assertThat(createdPartyEntity.getIdamId()).isEqualTo(userId);
        assertThat(createdPartyEntity.getForename()).isEqualTo(forename);
        assertThat(createdPartyEntity.getSurname()).isEqualTo(surname);
        assertThat(createdPartyEntity.getContactEmail()).isEqualTo(contactEmail);
        assertThat(createdPartyEntity.getContactAddress()).isEqualTo(addressEntity);
        assertThat(createdPartyEntity.getContactPhoneNumber()).isEqualTo(contactPhone);
        assertThat(createdPartyEntity.getActive()).isTrue();

        verify(partyRepository).save(createdPartyEntity);
    }

}
